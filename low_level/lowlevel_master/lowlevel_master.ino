/*
 Name:		lowlevel_master.ino
 Created:	04/03/2018 20:10:42
 Author:	Sylvain Gaultier
*/

#include "Config/pin_mapping.h"
#include "CommunicationServer/OrderMgr.h"
#include "Locomotion/MotionControlSystem.h"

#define START_ON_SERIAL 0


void setup()
{
    //pinMode(PIN_DEL_STATUS_1, OUTPUT);
    //digitalWrite(PIN_DEL_STATUS_1, HIGH);
#if START_ON_SERIAL
    while (!Serial);
#endif
    Server.begin();
}


void loop()
{
    OrderMgr orderManager = OrderMgr();
    MotionControlSystem &motionControlSystem = MotionControlSystem::Instance();
    DirectionController &directionController = DirectionController::Instance();

    IntervalTimer motionControlTimer;
    motionControlTimer.priority(253);
    motionControlTimer.begin(motionControlInterrupt, PERIOD_ASSERV);

    while (true)
    {
        orderManager.execute();
        directionController.control();
    }
}


void motionControlInterrupt()
{
    static MotionControlSystem &motionControlSystem = MotionControlSystem::Instance();
    motionControlSystem.control();
}


/* Ce bout de code permet de compiler avec std::vector */
namespace std {
    void __throw_bad_alloc()
    {
        while (true)
        {
            Server.printf_err("Unable to allocate memory\n");
            delay(500);
        }
    }

    void __throw_length_error(char const*e)
    {
        while (true)
        {
            Server.printf_err(e);
            Server.println_err();
            delay(500);
        }
    }

    void __throw_out_of_range(char const*e)
    {
        while (true)
        {
            Server.printf_err(e);
            Server.println_err();
            delay(500);
        }
    }

    void __throw_out_of_range_fmt(char const*e, ...)
    {
        while (true)
        {
            Server.printf_err(e);
            Server.println_err();
            delay(500);
        }
    }
}
