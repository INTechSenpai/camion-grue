#include "SensorsMgr.h"
#include "LightsMgr.h"
#include "Communication.h"
#include "InternalCom.h"
#include "config.h"
#include "ToF_shortRange.h"
#include "VL6180X.h"
#include <Wire.h>

SensorsMgr sensorsMgr;
LightsMgr lightsMgr;
Communication commMgr;


void setup()
{
    pinMode(PIN_DEL_ON_BOARD, OUTPUT);
    digitalWrite(PIN_DEL_ON_BOARD, HIGH);
    Wire.begin();
    uint32_t beginWait = millis();
    while (!commMgr.available() || millis() - beginWait < 500)
    {
        commMgr.listen();
    }
    lightsMgr.infoSignalOn();
    int ret = sensorsMgr.init();
    lightsMgr.infoSignalOff();
    if (ret != 0)
    {
        lightsMgr.setLightningMode((LightningMode)(ALARM_LIGHT));
        beginWait = millis();
        while (millis() - beginWait < 10000)
        {
            lightsMgr.update();
        }
        lightsMgr.setLightningMode((LightningMode)ALL_OFF);
    }

    digitalWrite(PIN_DEL_ON_BOARD, LOW);
}


void loop()
{
    SensorValue sensorsValues[NB_SENSORS];
    for (size_t i = 0; i < NB_SENSORS; i++)
    {
        sensorsValues[i] = (SensorValue)SENSOR_DEAD;
    }

    while (true)
    {
        commMgr.listen();
        if (commMgr.available())
        {
            lightsMgr.setLightningMode(commMgr.getLightningMode());
        }
        lightsMgr.update();

        sensorsMgr.update();
        sensorsMgr.getValues(sensorsValues);
        commMgr.sendSensorsData(sensorsValues);
    }
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
