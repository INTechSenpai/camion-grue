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
        lightningMode = (uint8_t)ALL_OFF;
    }

    enum LightId
    {
        ALL_OFF = 0,
        TURN_LEFT = 1,
        TURN_RIGHT = 2,
        FLASHING = 4,
        STOP_LIGHT = 8,
        REVERSE_LIGHT = 16,
        NIGHT_LIGHT_LOW = 32,
        NIGHT_LIGHT_HIGH = 64,
        ALARM_LIGHT = 128
    };

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
        lightningMode = mode;
        sendLightningMode();
    }

    void setLightOn(uint8_t mode)
    {
        lightningMode |= mode;
        sendLightningMode();
    }

    void setLightOff(uint8_t mode)
    {
        lightningMode &= ~mode;
        sendLightningMode();
    }

private:
    void sendLightningMode()
    {
        uint8_t payload[1] = { lightningMode };
        InternalMessage message(ID_FRAME_LED_MODE, 1, payload);
        internalCom.sendMessage(message);
    }

    InternalCom internalCom;
    InternalMessage lastMessage;
    uint8_t lightningMode;
};

#endif
