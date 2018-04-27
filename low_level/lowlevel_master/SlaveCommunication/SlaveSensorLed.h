#ifndef _SLAVE_SENSOR_LED_h
#define _SLAVE_SENSOR_LED_h

#include "InternalCom.h"
#include "../Config/serial_config.h"
#include "../CommunicationServer/Serializer.h"
#include "../Tools/Singleton.h"
#include <vector>

#define ID_FRAME_SENSORS_REPORT 0x00
#define ID_FRAME_LED_MODE       0x01


class SlaveSensorLed : public Singleton<SlaveSensorLed>
{
public:
    SlaveSensorLed() :
        internalCom(SERIAL_SENSORS, SERIAL_SENSORS_BAUDRATE)
    {
    }

    void listen()
    {
        internalCom.listen();
        if (internalCom.available())
        {
            lastMessage = internalCom.getMessage();
            if (lastMessage.getInstruction() != ID_FRAME_SENSORS_REPORT)
            {
                lastMessage.reset();
            }
        }
    }

    bool available() const
    {
        return lastMessage.isValid();
    }

    void getSensorsValues(std::vector<uint8_t> & output)
    {
        output.clear();
        for (size_t i = 0; i < lastMessage.size(); i++)
        {
            Serializer::writeInt((int32_t)lastMessage.at(i), output);
        }
        lastMessage.reset();
    }

    void setLightningMode(uint8_t mode)
    {
        uint8_t payload[1] = { mode };
        InternalMessage message(ID_FRAME_LED_MODE, 1, payload);
        internalCom.sendMessage(message);
    }

private:
    InternalCom internalCom;
    InternalMessage lastMessage;
};

#endif
