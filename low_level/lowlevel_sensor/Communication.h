#ifndef _COMMUNICATION_h
#define _COMMUNICATION_h

#include "config.h"
#include "InternalCom.h"
#include "LightsMgr.h"

#define ID_FRAME_SENSORS_REPORT 0x00
#define ID_FRAME_LED_MODE       0x01


class Communication
{
public:
    Communication() :
        internalCom(MASTER_SERIAL, BAUDRATE_SERIAL)
    {
        lightningMode = (LightningMode)ALL_OFF;
        updateAvailable = false;
    }

    void listen()
    {
        internalCom.listen();
        if (internalCom.available())
        {
            InternalMessage message = internalCom.getMessage();
            if (message.getInstruction() == ID_FRAME_LED_MODE && message.size() == 1)
            {
                lightningMode = (LightningMode)message.at(0);
                updateAvailable = true;
            }
            else
            {
                Serial.println("Wrong command received");
            }
        }
    }

    bool available() const
    {
        return updateAvailable;
    }

    LightningMode getLightningMode()
    {
        updateAvailable = false;
        return lightningMode;
    }

    void sendSensorsData(const uint8_t sensorsValues[NB_SENSORS])
    {
        InternalMessage message(ID_FRAME_SENSORS_REPORT, NB_SENSORS, sensorsValues);
        internalCom.sendMessage(message);
    }

private:
    InternalCom internalCom;
    LightningMode lightningMode;
    bool updateAvailable;
};


#endif
