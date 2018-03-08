/*
 Name:		lowlevel_master.ino
 Created:	04/03/2018 20:10:42
 Author:	Sylvain Gaultier
*/

#include "CommunicationServer/OrderMgr.h"
#include "Locomotion\MotionControlSystem.h"

#define START_ON_SERIAL 1


void setup()
{
#if START_ON_SERIAL
    while (!Serial);
#endif
}

OrderMgr orderManager = OrderMgr();
MotionControlSystem &mcs = MotionControlSystem::Instance();

void loop()
{
    orderManager.execute();
    mcs.control();
    Server.printf("loli%d", 5);
}


/* Ce bout de code permet de compiler avec std::vector */
namespace std {
    void __throw_bad_alloc()
    {
        while (true)
        {
            Serial.println("Unable to allocate memory");
            delay(500);
        }
    }

    void __throw_length_error(char const*e)
    {
        while (true)
        {
            Serial.println(e);
            delay(500);
        }
    }

    void __throw_out_of_range(char const*e)
    {
        while (true)
        {
            Serial.println(e);
            delay(500);
        }
    }

    void __throw_out_of_range_fmt(char const*e, ...)
    {
        while (true)
        {
            Serial.println(e);
            delay(500);
        }
    }
}
