#include "SensorsMgr.h"
#include "LightsMgr.h"
#include "config.h"
#include "ToF_shortRange.h"
#include "VL6180X.h"
#include <Wire.h>

SensorsMgr sensorsMgr;
LightsMgr lightsMgr;


void setup()
{
    pinMode(PIN_DEL_ON_BOARD, OUTPUT);
    digitalWrite(PIN_DEL_ON_BOARD, HIGH);
    Wire.begin();
    delay(500);
    sensorsMgr.init();

    digitalWrite(PIN_DEL_ON_BOARD, LOW);
}


void loop()
{
    while (true)
    {
        sensorsMgr.update();
        lightsMgr.update();

        Serial.println(sensorsMgr);
    }
}
