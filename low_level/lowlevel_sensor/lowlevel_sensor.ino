#include "SensorsMgr.h"
#include "LightsMgr.h"
#include "pin_mapping.h"
#include "ToF_shortRange.h"
#include "VL6180X.h"
#include <Wire.h>

SensorsMgr sensorsMgr;
LightsMgr lightsMgr;


void setup()
{
    pinMode(13, OUTPUT);
    digitalWrite(13, HIGH);
    Wire.begin();
    delay(500);
    sensorsMgr.init();
}

void loop()
{
    while (true) {
        lightsMgr.update();
    }
}
