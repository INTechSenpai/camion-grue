#ifndef _INTERNAL_COM_h
#define _INTERNAL_COM_h

#include <vector>

#define INTERNAL_COM_MAX_LISTEN_DURATION    2   // ms


class InternalMessage
{
public:
    InternalMessage()
    {
        reset();
    }

    InternalMessage(uint8_t instruction, const std::vector<uint8_t> & payload)
    {
        this->instruction = instruction;
        this->length = (uint8_t)payload.size();
        this->payload = payload;
        this->valid = (size_t)this->length == payload.size();
    }

    InternalMessage(uint8_t instruction, uint8_t length, const uint8_t payload[])
    {
        this->instruction = instruction;
        this->length = length;
        this->payload.clear();
        for (size_t i = 0; i < length; i++)
        {
            this->payload.push_back(payload[i]);
        }
        this->valid = true;
    }

    void reset()
    {
        valid = false;
        instruction = 0;
        length = 0;
        payload.clear();
    }

    bool isValid() const
    {
        return valid;
    }

    void setInstruction(uint8_t instruction)
    {
        this->instruction = instruction;
    }

    uint8_t getInstruction() const
    {
        return instruction;
    }

    void setLength(uint8_t length)
    {
        this->length = length;
    }

    void appendToPayload(uint8_t byte)
    {
        payload.push_back(byte);
    }

    size_t size() const
    {
        return payload.size();
    }

    uint8_t at(size_t i) const
    {
        return payload.at(i);
    }
	
    const std::vector<uint8_t> & getPayload() const
    {
        return payload;
    }

    bool payloadIsFull() const
    {
        return payload.size() == length;
    }

    void setChecksum(uint8_t checksum)
    {
        uint8_t computedChecksum = getChecksum();
        valid = checksum == computedChecksum && length == payload.size();
    }

    uint8_t getChecksum() const
    {
        uint8_t computedChecksum = instruction + length;
        for (size_t i = 0; i < payload.size(); i++)
        {
            computedChecksum += payload.at(i);
        }
        computedChecksum = ~computedChecksum;
        return computedChecksum;
    }

private:
    bool valid;
    uint8_t instruction;
    uint8_t length;
    std::vector<uint8_t> payload;
};


class InternalCom
{
public:
    InternalCom(HardwareSerial & serial, uint32_t baudrate) :
        serial(serial)
    {
        serial.begin(baudrate);
        readPhase = HEADER1;
    }

    void listen()
    {
        if (message.isValid())
        {
            return;
        }
        uint32_t beginTime = millis();
        while (serial.available() && millis() - beginTime < INTERNAL_COM_MAX_LISTEN_DURATION)
        {
            uint8_t recByte = serial.read();
            switch (readPhase)
            {
                case HEADER1:
                    if (recByte == 0xFF) {
                        readPhase = HEADER2;
                    } else {
                        // err
                    }
                    break;
                case HEADER2:
                    if (recByte == 0xFF) {
                        readPhase = INSTRUCTION;
                    } else {
                        readPhase = HEADER1;
                        // err
                    }
                    break;
                case INSTRUCTION:
                    message.setInstruction(recByte);
                    readPhase = LENGTH;
                    break;
                case LENGTH:
                    message.setLength(recByte);
                    readPhase = PAYLOAD;
                    break;
                case PAYLOAD:
                    if (message.payloadIsFull()) {
                        message.setChecksum(recByte);
                        readPhase = HEADER1;
                        if (message.isValid()) {
                            return;
                        } else {
                            message.reset();
                            // err
                        }
                    } else {
                        message.appendToPayload(recByte);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    bool available() const
    {
        return message.isValid();
    }

    InternalMessage getMessage()
    {
        InternalMessage ret = message;
        message.reset();
        return ret;
    }

    void sendMessage(const InternalMessage &m)
    {
        if (m.isValid())
        {
            serial.write(0xFF);
            serial.write(0xFF);
            serial.write(m.getInstruction());
            serial.write(m.size());
            for (size_t i = 0; i < m.size(); i++)
            {
                serial.write(m.at(i));
            }
            serial.write(m.getChecksum());
        }
        else
        {
            // err
        }
    }

private:
    enum ReadPhase
    {
        HEADER1, HEADER2, INSTRUCTION, LENGTH, PAYLOAD
    };

    HardwareSerial & serial;
    InternalMessage message;
    ReadPhase readPhase;
};


#endif
