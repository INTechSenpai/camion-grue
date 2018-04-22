#include "config.h"
#include "InternalCom.h"
#include "MotorSensor.h"
#include "ArmController.h"
#include "ArmPosition.h"
#include "Communication.h"
#include <Wire.h>


void setup()
{
}

void errorLoop()
{
    pinMode(PIN_DEL_GYRO_G, OUTPUT);
    pinMode(PIN_DEL_GYRO_D, OUTPUT);
    while (true)
    {
        for (int i = 0; i < 4; i++)
        {
            digitalWrite(PIN_DEL_GYRO_G, HIGH);
            digitalWrite(PIN_DEL_GYRO_D, HIGH);
            delay(100);
            digitalWrite(PIN_DEL_GYRO_G, LOW);
            digitalWrite(PIN_DEL_GYRO_D, LOW);
            delay(100);
        }
        delay(400);
    }
}

void loop()
{
    Serial.println("Coucou");
    pinMode(PIN_DEL_GYRO_G, OUTPUT);
    pinMode(PIN_DEL_GYRO_D, OUTPUT);
    digitalWrite(PIN_DEL_GYRO_G, HIGH);
    digitalWrite(PIN_DEL_GYRO_D, LOW);
    Wire.begin();
    delay(200);

    Communication communication;

    if (communication.init() != 0)
    {
        errorLoop();
    }

    IntervalTimer timer;
    timer.priority(253);
    timer.begin(armControllerInterrupt, PERIOD_ASSERV);

    uint32_t reportTimer = 0;
    while (true)
    {
        communication.listenAndExecute();

        if (communication.newScoreAvailable())
        {
            // todo: update score
        }

        if (communication.newSensorsAngle())
        {
            // todo: update sensors aim angle
        }

        if (millis() - reportTimer > REPORT_PERIOD)
        {
            reportTimer = millis();
            communication.sendReport(0, 0, 0, 0); // todo: send real sensors data
        }

        // todo: SensorsMgr.updateServos();

        // todo: UserInputControler.control();
    }
}

void armControllerInterrupt()
{
    static ArmController & armController = ArmController::Instance();
    armController.controlCCMotors();
}


/* Ce bout de code permet de compiler avec std::vector */
namespace std {
    void __throw_bad_alloc()
    {
        while (true)
        {
            Serial.printf("Unable to allocate memory\n");
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
