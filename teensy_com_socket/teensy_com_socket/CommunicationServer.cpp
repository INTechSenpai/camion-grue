#include "CommunicationServer.h"

CommunicationServer Server = CommunicationServer();


CommunicationServer::CommunicationServer() :
    ethernetServer(TCP_PORT)
{
    pinMode(WIZ820_PWDN_PIN, OUTPUT);
  digitalWrite(WIZ820_PWDN_PIN, LOW);
    pinMode(WIZ820_RESET_PIN, OUTPUT);
    digitalWrite(WIZ820_RESET_PIN, LOW);    // begin reset the WIZ820io
    pinMode(WIZ820_SS_PIN, OUTPUT);
    digitalWrite(WIZ820_SS_PIN, HIGH);        // de-select WIZ820io
    digitalWrite(WIZ820_RESET_PIN, HIGH);   // end reset pulse
  uint8_t mac[] = { MAC_ADDR };
    IPAddress ip(IP_ADDR);
    IPAddress dns(DNS_IP);
    IPAddress gateway(GATEWAY_IP);
    IPAddress subnet(SUBNET_MASK);

    Ethernet.begin(mac, ip, dns, gateway, subnet);
    ethernetServer.begin();

    cBufferHead = 0;
    cBufferTail = 0;
    outputBuffer[0] = '\0';
    for (uint8_t i = 0; i < MAX_SOCK_NUM + 1; i++)
    {
        subscriptionList[i] = 0;
    }
    bisTraceVectUsed = false;

    if (Ethernet.localIP() != ip)
    {
        subscriptionList[MAX_SOCK_NUM] |= (1 << ERROR);
        printf_err("Network configuration failed\n");
    }
}

void CommunicationServer::communicate()
{
    /* Gestion des connexions de nouveaux clients */
    EthernetClient client = ethernetServer.available();
    if (client)
    {
        uint8_t socketNb = client.getSocketNumber();
        if (!isConnected(socketNb))
        {
            ethernetClients[socketNb] = client;
            subscriptionList[socketNb] = DEFAULT_SUSCRIPTION;
            printf("New client connected on socket %u\n", socketNb);
        }
    }
#if SERIAL_ENABLE
    static bool serialConnected = false;
    if (!serialConnected && Serial)
    {
        serialConnected = true;
        subscriptionList[MAX_SOCK_NUM] = DEFAULT_SUSCRIPTION;
    }
#endif

    /* Réception des messages */
    bool receivedAtLeastOneByte = true;
    uint32_t startReceptionTime = micros();
    while (micros() - startReceptionTime < MAX_RECEPTION_DURATION && receivedAtLeastOneByte)
    {
        receivedAtLeastOneByte = false;
        if (available() < COMMAND_BUFFER_SIZE - MAX_SOCK_NUM)
        {
            for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
            {
                if (isConnected(i) && ethernetClients[i].available() > 0)
                {
                    receivedAtLeastOneByte = true;
                    uint8_t newByte = ethernetClients[i].read();
                    int8_t ret = receptionHandlers[i].addByte(newByte, i);
                    printf("Message de %u: %u\n", i, newByte);
                    if (ret == 1)
                    {
                        printf_err("Drop the byte\n");
                    }
                    else if (ret == -1)
                    {
                        printf_err("Information frame received\n");
                    }
                    if (receptionHandlers[i].available())
                    {
                        processOrAddCommandToBuffer(receptionHandlers[i].getCommand());
                    }
                }
            }
#if SERIAL_ENABLE
            if (Serial && Serial.available() > 0)
            {
                receivedAtLeastOneByte = true;
                int8_t ret = receptionHandlers[MAX_SOCK_NUM].addByte(Serial.read(), MAX_SOCK_NUM);
                if (ret == 1)
                {
                    printf_err("Drop the byte\n");
                }
                else if (ret == -1)
                {
                    printf_err("Information frame received\n");
                }
                if (receptionHandlers[MAX_SOCK_NUM].available())
                {
                    processOrAddCommandToBuffer(receptionHandlers[MAX_SOCK_NUM].getCommand());
                }
            }
#endif
        }
    }

    /* Envoi des messages à envoi différé (issus des interruptions) */
    noInterrupts();
    bisTraceVectUsed = !bisTraceVectUsed;
    interrupts();
    if (bisTraceVectUsed)
    {
        for (size_t i = 0; i < asyncTraceVect.size(); i++)
        {
            trace(asyncTraceVect.at(i).lineNb, ASYNC_TRACE_FILENAME, asyncTraceVect.at(i).timestamp);
        }
        asyncTraceVect.clear();
    }
    else
    {
        for (size_t i = 0; i < asyncTraceVectBis.size(); i++)
        {
            trace(asyncTraceVectBis.at(i).lineNb, ASYNC_TRACE_FILENAME, asyncTraceVectBis.at(i).timestamp);
        }
        asyncTraceVectBis.clear();
    }

    /* Gestion des déconnexions */
    for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
    {
            printf("Client %u status : %u %u\n", i, ethernetClients[i].status(), ethernetClients[i].connected());
/*        if (ethernetClients[i].status()) 
        {
            ethernetClients[i].stop();
            subscriptionList[i] = 0;
            printf("Client %u disconnected\n", i);
        }
*/    }
}

uint8_t CommunicationServer::available()
{
    if (cBufferHead >= cBufferTail)
    {
        return cBufferHead - cBufferTail;
    }
    else
    {
        return COMMAND_BUFFER_SIZE + cBufferHead - cBufferTail;
    }
}

bool CommunicationServer::isConnected(uint8_t client)
{
    if (client > MAX_SOCK_NUM)
    {
        return false;
    }
    else if (client == MAX_SOCK_NUM)
    {
        return (bool)Serial;
    }
    else
    {
        return /*!ethernetClients[client].status() &&*/ ethernetClients[client].connected();
    }
}

Command CommunicationServer::getLastCommand()
{
    if (cBufferHead == cBufferTail)
    {
        return Command();
    }
    else
    {
        Command lastCommand = commandBuffer[cBufferTail];
        commandBuffer[cBufferTail].makeInvalid();
        cBufferTail++;
        if (cBufferTail == COMMAND_BUFFER_SIZE)
        {
            cBufferTail = 0;
        }
        return lastCommand;
    }
}

void CommunicationServer::sendAnswer(Command answer)
{
    uint8_t dest = answer.getSource();
    size_t n = 0;
    n += sendByte(0xFF, dest);
    n += sendVector(answer.getVector(), dest);
    if (n != (size_t)answer.getLength() + 3)
    {
        printf_err("Answer not entierly sent (%u/%u)\n", n, (size_t)answer.getLength() + 3);
    }
}

void CommunicationServer::sendData(Channel channel, std::vector<uint8_t> const & data)
{
    if (data.size() > COMMAND_MAX_DATA_SIZE)
    {
        printf_err("Data too big (%u bytes) to fit in a standard frame\n", data.size());
        return;
    }
    for (uint8_t i = 0; i < MAX_SOCK_NUM + 1; i++)
    {
        if (subscribed(i, channel))
        {
            sendByte(0xFF, i);
            sendByte(channel, i);
            sendByte(data.size(), i);
            sendVector(data, i);
        }
    }
}

void CommunicationServer::print(Channel channel, const Printable & obj)
{
    for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
    {
        if (subscribed(i, channel))
        {
            printHeader(ethernetClients[i], channel);
            ethernetClients[i].print(obj);
            ethernetClients[i].print('\0');
        }
    }
#if SERIAL_ENABLE
    if (subscribed(MAX_SOCK_NUM, channel))
    {
        printHeader(Serial, channel);
        Serial.print(obj);
        Serial.print('\0');
    }
#endif
}

void CommunicationServer::printf(const char * format, ...)
{
    if (isThereListener(INFO))
    {
        va_list args;
        va_start(args, format);
        vsnprintf(outputBuffer, OUTPUT_BUFFER_SIZE, format, args);
        va_end(args);
        sendOutputBuffer(INFO);
    }
}

void CommunicationServer::printf_err(const char * format, ...)
{
    if (isThereListener(ERROR))
    {
        va_list args;
        va_start(args, format);
        vsnprintf(outputBuffer, OUTPUT_BUFFER_SIZE, format, args);
        va_end(args);
        sendOutputBuffer(ERROR);
    }
}


void CommunicationServer::printf(Channel channel, const char * format, ...)
{
    if (isThereListener(channel))
    {
        va_list args;
        va_start(args, format);
        vsnprintf(outputBuffer, OUTPUT_BUFFER_SIZE, format, args);
        va_end(args);
        sendOutputBuffer(channel);
    }
}

void CommunicationServer::print(Channel channel, uint32_t u, bool newLine)
{
    for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
    {
        if (subscribed(i, channel))
        {
            printHeader(ethernetClients[i], channel);
            if (newLine) { ethernetClients[i].print(u); }
            else { ethernetClients[i].println(u); }
            ethernetClients[i].print('\0');
        }
    }
#if SERIAL_ENABLE
    if (subscribed(MAX_SOCK_NUM, channel))
    {
        printHeader(Serial, channel);
        if (newLine) { Serial.print(u); }
        else { Serial.println(u); }
        Serial.print('\0');
    }
#endif
}

void CommunicationServer::print(Channel channel, int32_t n, bool newLine)
{
    for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
    {
        if (subscribed(i, channel))
        {
            printHeader(ethernetClients[i], channel);
            if (newLine) { ethernetClients[i].print(n); }
            else { ethernetClients[i].println(n); }
            ethernetClients[i].print('\0');
        }
    }
#if SERIAL_ENABLE
    if (subscribed(MAX_SOCK_NUM, channel))
    {
        printHeader(Serial, channel);
        if (newLine) { Serial.print(n); }
        else { Serial.println(n); }
        Serial.print('\0');
    }
#endif
}

void CommunicationServer::print(Channel channel, double d, bool newLine)
{
    for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
    {
        if (subscribed(i, channel))
        {
            printHeader(ethernetClients[i], channel);
            if (newLine) { ethernetClients[i].print(d); }
            else { ethernetClients[i].println(d); }
            ethernetClients[i].print('\0');
        }
    }
#if SERIAL_ENABLE
    if (subscribed(MAX_SOCK_NUM, channel))
    {
        printHeader(Serial, channel);
        if (newLine) { Serial.print(d); }
        else { Serial.println(d); }
        Serial.print('\0');
    }
#endif
}

void CommunicationServer::print(Channel channel, const char * str, bool newLine)
{
    for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
    {
        if (subscribed(i, channel))
        {
            printHeader(ethernetClients[i], channel);
            if (newLine) { ethernetClients[i].print(str); }
            else { ethernetClients[i].println(str); }
            ethernetClients[i].print('\0');
        }
    }
#if SERIAL_ENABLE
    if (subscribed(MAX_SOCK_NUM, channel))
    {
        printHeader(Serial, channel);
        if (newLine) { Serial.print(str); }
        else { Serial.println(str); }
        Serial.print('\0');
    }
#endif
}

void CommunicationServer::println(Channel channel)
{
    for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
    {
        if (subscribed(i, channel))
        {
            printHeader(ethernetClients[i], channel);
            ethernetClients[i].println();
            ethernetClients[i].print('\0');
        }
    }
#if SERIAL_ENABLE
    if (subscribed(MAX_SOCK_NUM, channel))
    {
        printHeader(Serial, channel);
        Serial.println();
        Serial.print('\0');
    }
#endif
}

void CommunicationServer::trace(uint32_t line, const char * filename, uint32_t timestamp)
{
    for (uint8_t i = 0; i < MAX_SOCK_NUM; i++)
    {
        if (subscribed(i, TRACE))
        {
            printHeader(ethernetClients[i], TRACE);
            if (timestamp == 0) { ethernetClients[i].print(micros()); }
            else { ethernetClients[i].print(timestamp); }
            ethernetClients[i].print('_');
            ethernetClients[i].print(line);
            ethernetClients[i].print('_');
            ethernetClients[i].print(filename);
            ethernetClients[i].print('\0');
        }
    }
#if SERIAL_ENABLE
    if (subscribed(MAX_SOCK_NUM, TRACE))
    {
        printHeader(Serial, TRACE);
        if (timestamp == 0) { Serial.print(micros()); }
        else { Serial.print(timestamp); }
        Serial.print('_');
        Serial.print(line);
        Serial.print('_');
        Serial.print(filename);
        Serial.print('\0');
    }
#endif
}

void CommunicationServer::asynchronous_trace(uint32_t line)
{
    ExecTrace trace;
    trace.lineNb = line;
    trace.timestamp = micros();
    if (bisTraceVectUsed)
    {
        asyncTraceVectBis.push_back(trace);
    }
    else
    {
        asyncTraceVect.push_back(trace);
    }
}

void CommunicationServer::sendOutputBuffer(Channel channel)
{
    for (uint8_t i = 0; i < MAX_SOCK_NUM + 1; i++)
    {
        if (subscribed(i, channel))
        {
            sendByte(0xFF, i);
            sendByte(channel, i);
            sendByte(0xFF, i);
            sendCString(outputBuffer, i);
        }
    }
    outputBuffer[0] = '\0';
}

size_t CommunicationServer::sendByte(uint8_t byte, uint8_t dest)
{
    if (dest < MAX_SOCK_NUM)
    {
        return ethernetClients[dest].write(byte);
    }
#if SERIAL_ENABLE
    else if (dest == MAX_SOCK_NUM)
    {
        return Serial.write(byte);
    }
#endif
    else
    {
        return 0;
    }
}

size_t CommunicationServer::sendVector(std::vector<uint8_t> const & vect, uint8_t dest)
{
    size_t n = 0;
    if (dest < MAX_SOCK_NUM)
    {
        for (size_t i = 0; i < vect.size(); i++)
        {
            n += ethernetClients[dest].write(vect.at(i));
        }
    }
#if SERIAL_ENABLE
    else if (dest == MAX_SOCK_NUM)
    {
        for (size_t i = 0; i < vect.size(); i++)
        {
            n += Serial.write(vect.at(i));
        }
    }
#endif
    else
    {
        return 0;
    }
    return n;
}

size_t CommunicationServer::sendCString(const char * str, uint8_t dest)
{
    if (dest < MAX_SOCK_NUM)
    {
        return ethernetClients[dest].write(str) + ethernetClients[dest].print('\0');
    }
#if SERIAL_ENABLE
    else if (dest == MAX_SOCK_NUM)
    {
        return Serial.write(str) + Serial.print('\0');
    }
#endif
    else
    {
        return 0;
    }
}

void CommunicationServer::processOrAddCommandToBuffer(Command command)
{
    if (command.getId() < CHANNEL_MAX_NB) // Ordre d'inscription/désinscription à traiter
    {
        if (command.getLength() != 1 || command.getSource() > MAX_SOCK_NUM)
        {
            printf_err("Wrong subscription command\n");
        }
        else
        {
            if (command.getData().at(0)) // inscription
            {
                subscriptionList[command.getSource()] |= (1 << command.getId());
            }
            else // désinscription
            {
                subscriptionList[command.getSource()] &= ~(1 << command.getId());
            }
        }
    }
    else // Autre ordre, à stocker pour le transmettre
    {
        if (available() < COMMAND_BUFFER_SIZE - 1)
        {
            commandBuffer[cBufferHead] = command;
            cBufferHead++;
            if (cBufferHead == COMMAND_BUFFER_SIZE)
            {
                cBufferHead = 0;
            }
        }
        else
        {
            printf_err("Command buffer is full\n");
        }
    }
}
