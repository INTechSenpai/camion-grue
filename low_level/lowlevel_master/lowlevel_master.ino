/*
 Name:		lowlevel_master.ino
 Created:	04/03/2018 20:10:42
 Author:	Sylvain Gaultier
*/

#include "Config/pin_mapping.h"
#include "CommunicationServer/OrderMgr.h"
#include "Locomotion/MotionControlSystem.h"

#define LOG_PERIOD  20  // ms


void setup()
{
    pinMode(PIN_DEL_STATUS_1, OUTPUT);
    pinMode(PIN_DEL_STATUS_2, OUTPUT);
    digitalWrite(PIN_DEL_STATUS_1, HIGH);

    //pinMode(PIN_WIZ820_RESET, OUTPUT);
    //pinMode(PIN_WIZ820_SS, OUTPUT);
    //digitalWrite(PIN_WIZ820_RESET, LOW);    // begin reset the WIZ820io
    //digitalWrite(PIN_WIZ820_SS, HIGH);      // de-select WIZ820io
    //delay(1000);

    if (Server.begin() != 0)
    {
        digitalWrite(PIN_DEL_STATUS_2, HIGH);
        delay(500);
    }
}


void loop()
{
    OrderMgr orderManager = OrderMgr();
    MotionControlSystem &motionControlSystem = MotionControlSystem::Instance();
    DirectionController &directionController = DirectionController::Instance();

    IntervalTimer motionControlTimer;
    motionControlTimer.priority(253);
    motionControlTimer.begin(motionControlInterrupt, PERIOD_ASSERV);

    uint32_t logTimer = 0;
    uint32_t delTimer = 0;
    bool delState = true;

    while (true)
    {
        orderManager.execute();
        directionController.control();

        if (millis() - logTimer > LOG_PERIOD)
        {
            motionControlSystem.sendLogs();
            logTimer = millis();
        }

        if (millis() - delTimer > 500)
        {
            delState = !delState;
            digitalWrite(PIN_DEL_STATUS_1, delState);
            delTimer = millis();
        }
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
