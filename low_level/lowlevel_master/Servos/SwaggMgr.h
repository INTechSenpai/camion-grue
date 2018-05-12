#ifndef SWAGG_MGR_h
#define SWAGG_MGR_h

#include "XL320/XL320.h"
#include "../Config/serial_config.h"
#include "../Tools/Singleton.h"

#define ID_XL_AVG   1
#define ID_XL_AVD   2
#define ID_XL_ARG   3
#define ID_XL_ARD   4


class SwaggMgr : public Singleton<SwaggMgr>
{
public:
    SwaggMgr() :
        servos(SERIAL_XL320)
    {}

    void init()
    {
        servos.begin(SERIAL_XL320, SERIAL_XL320_BAUDRATE);
        servos.setSpeed(ID_XL_AVG, 1023);
        // ...
    }

    void setId(uint8_t oldId, uint8_t newId)
    {
        servos.write(oldId, XL_ID, newId, 1);
    }

    void test(uint8_t id)
    {
        while (true)
        {
            servos.setLED(id, 'y');
            delay(200);
            servos.setLED(id, 'c');
            delay(200);
        }
    }

    void deploy()
    {
        //servos.setPosition(ID_XL_AVG, )
    }

    void deploy(uint8_t xl_id)
    {

    }

    void retract()
    {

    }

    void retract(uint8_t xl_id)
    {

    }

private:
    XL320 servos;
};


#endif
