#ifndef _COMMUNICATION_h
#define _COMMUNICATION_h

#include "config.h"

#define ID_FRAME_SENSORS_REPORT 0x00
#define ID_FRAME_LED_MODE       0x01


class Communication
{
public:
    Communication()
    {

    }

    void begin()
    {
        SERIAL.begin(BAUDRATE_SERIAL);
    }

    void listen()
    {

    }

    int available()
    {

    }

    void sendSensorsData(uint8_t sensorsValues[NB_SENSORS])
    {

    }

private:

};


#endif
