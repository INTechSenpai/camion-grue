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

    std::vector<uint8_t> getPayload() const
    {
        return payload;
    }

    bool payloadIsFull() const
    {
        return payload.size() == length;
    }

    void setChecksum(uint8_t checksum)
    {
        uint8_t computedChecksum = computeChecksum();
        valid = checksum == computedChecksum && length == payload.size();
    }

    void getArray(std::vector<uint8_t> & output)
    {
        output.clear();
        if (valid)
        {
            output.push_back(0xFF);
            output.push_back(0xFF);
            output.push_back(instruction);
            output.push_back(length);
            for (size_t i = 0; i < payload.size(); i++)
            {
                output.push_back(payload.at(i));
            }
            output.push_back(computeChecksum());
        }
    }

private:
    uint8_t computeChecksum()
    {
        uint8_t computedChecksum = instruction + length;
        for (size_t i = 0; i < payload.size(); i++)
        {
            computedChecksum += payload.at(i);
        }
        computedChecksum = ~computedChecksum;
        return computedChecksum;
    }

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
        uint32_t beginTime = millis();
        while (serial.available() && millis() - beginTime < INTERNAL_COM_MAX_LISTEN_DURATION)
        {
            uint8_t recByte = serial.read();
        }
    }

    bool available()
    {

    }

    InternalMessage getMessage()
    {

    }

    void sendMessage(InternalMessage m)
    {

    }

private:
    enum ReadPhase
    {
        HEADER1, HEADER2, INSTRUCTION, LENGTH, PAYLOAD, CHECKSUM
    };

    HardwareSerial & serial;
    InternalMessage message;
    ReadPhase readPhase;
};


#endif
