#include "SensorsMgr.h"
#include "pin_mapping.h"
#include "ToF_shortRange.h"
#include "VL6180X.h"
#include <Wire.h>

SensorsMgr sensorsMgr;


void setup()
{
    pinMode(13, OUTPUT);
    digitalWrite(13, HIGH);
    Wire.begin();
    delay(500);
    sensorsMgr.init();

    pinMode(PIN_DEL_GYRO_1, OUTPUT);
    pinMode(PIN_DEL_GYRO_2, OUTPUT);
    pinMode(PIN_DEL_GYRO_3, OUTPUT);
    pinMode(PIN_DEL_GYRO_4, OUTPUT);
    pinMode(PIN_DEL_NUIT_AV, OUTPUT);
    pinMode(PIN_DEL_CLIGNO_G, OUTPUT);
    pinMode(PIN_DEL_CLIGNO_D, OUTPUT);
    pinMode(PIN_DEL_NUIT_AR, OUTPUT);
    pinMode(PIN_DEL_STOP, OUTPUT);
    pinMode(PIN_DEL_RECUL, OUTPUT);
}

void loop()
{
    while (true) {


    //    digitalWrite(PIN_DEL_CLIGNO_G, HIGH);
    //    delay(200);
    //    digitalWrite(PIN_DEL_CLIGNO_G, LOW);
    //    digitalWrite(PIN_DEL_CLIGNO_D, HIGH);
    //    delay(200);
    //    digitalWrite(PIN_DEL_CLIGNO_D, LOW);
    //    digitalWrite(PIN_DEL_NUIT_AR, HIGH);
    //    delay(200);
    //    digitalWrite(PIN_DEL_NUIT_AR, LOW);
    //    digitalWrite(PIN_DEL_NUIT_AV, HIGH);
    //    delay(200);
    //    digitalWrite(PIN_DEL_NUIT_AV, LOW);
    //    digitalWrite(PIN_DEL_STOP, HIGH);
    //    delay(200);
    //    digitalWrite(PIN_DEL_STOP, LOW);
    //    digitalWrite(PIN_DEL_RECUL, HIGH);
    //    delay(200);
    //    digitalWrite(PIN_DEL_RECUL, LOW);

        digitalWrite(PIN_DEL_NUIT_AV, HIGH);
        digitalWrite(PIN_DEL_GYRO_1, HIGH);
        delay(200);
        digitalWrite(PIN_DEL_GYRO_1, LOW);
        while (true) {
            digitalWrite(PIN_DEL_GYRO_2, HIGH);
            delay(200);
            digitalWrite(PIN_DEL_GYRO_2, LOW);
            digitalWrite(PIN_DEL_GYRO_3, HIGH);
            delay(200);
            digitalWrite(PIN_DEL_GYRO_3, LOW);
            digitalWrite(PIN_DEL_GYRO_4, HIGH);
            digitalWrite(PIN_DEL_GYRO_1, HIGH);
            delay(200);
            digitalWrite(PIN_DEL_GYRO_4, LOW);
            digitalWrite(PIN_DEL_GYRO_1, LOW);
        }
    }
}
