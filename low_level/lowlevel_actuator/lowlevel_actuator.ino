#include "config.h"
#include "InternalCom.h"
#include "MotorSensor.h"
#include "ArmController.h"
#include "ArmPosition.h"
#include <Wire.h>


void setup()
{
    pinMode(PIN_DEL_GYRO_G, OUTPUT);
    pinMode(PIN_DEL_GYRO_D, OUTPUT);
}

void loop()
{
    Serial.println("Coucou");
    digitalWrite(PIN_DEL_GYRO_G, HIGH);
    digitalWrite(PIN_DEL_GYRO_D, LOW);
    Wire.begin();
    delay(200);

    digitalWrite(PIN_DEL_GYRO_G, LOW);
    digitalWrite(PIN_DEL_GYRO_D, HIGH);
    delay(200);

    ArmController & armController = ArmController::Instance();
    armController.init();
    IntervalTimer timer;
    timer.priority(253);
    timer.begin(armControllerInterrupt, PERIOD_ASSERV);

    delay(1000);
    Serial.println("Abwabwa");
    ArmPosition p;
    Serial.println(p);
    armController.getCurrentPosition(p);
    Serial.println(p);
    p.setHAngle(0);
    p.setVAngle(0);
    p.setHeadGlobalAngle(0);
    p.setPlierPos(25);
    armController.setAimPosition(p);
    Serial.println(p);
    Serial.println();
    while (armController.isMoving())
    {
        armController.controlServos();

        static uint32_t lastTime = 0;
        static bool state = false;
        if (millis() - lastTime > 500)
        {
            lastTime = millis();
            state = !state;
            digitalWrite(PIN_DEL_GYRO_D, state);
        }
    }

    //p.setHAngle(0);
    //p.setVAngle(0);
    //armController.setAimPosition(p);
    //Serial.println(p);
    //Serial.println();
    //while (armController.isMoving())
    //{
    //    armController.controlServos();

    //    static uint32_t lastTime = 0;
    //    static bool state = false;
    //    if (millis() - lastTime > 500)
    //    {
    //        lastTime = millis();
    //        state = !state;
    //        digitalWrite(PIN_DEL_GYRO_D, state);
    //    }
    //}
    Serial.println("The end");

    while (true)
    {
        digitalWrite(PIN_DEL_GYRO_G, HIGH);
        digitalWrite(PIN_DEL_GYRO_D, LOW);
        delay(200);

        digitalWrite(PIN_DEL_GYRO_G, LOW);
        digitalWrite(PIN_DEL_GYRO_D, HIGH);
        delay(200);
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
