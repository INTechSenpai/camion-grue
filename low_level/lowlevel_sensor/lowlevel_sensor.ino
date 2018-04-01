#include "pin_mapping.h"
#include "ToF_shortRange.h"
#include "VL6180X.h"
#include <Wire.h>

ToF_shortRange tofAVG(42, PIN_EN_TOF_AVG);
ToF_shortRange tofAV(43, PIN_EN_TOF_AV);
ToF_shortRange tofAVD(44, PIN_EN_TOF_AVD);

ToF_shortRange tofFlanAVG(45, PIN_EN_TOF_FLAN_AVG);
ToF_shortRange tofFlanAVD(46, PIN_EN_TOF_FLAN_AVD);
ToF_shortRange tofFlanARG(47, PIN_EN_TOF_FLAN_ARG);
ToF_shortRange tofFlanARD(48, PIN_EN_TOF_FLAN_ARD);

ToF_shortRange tofARG(49, PIN_EN_TOF_ARG);
ToF_shortRange tofARD(50, PIN_EN_TOF_ARD);


void setup()
{
    pinMode(13, OUTPUT);
    digitalWrite(13, HIGH);
    Wire.begin();
    delay(500);

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
    tofAVG.powerON("AVG");
    tofAV.powerON("AV");
    tofAVD.powerON("AVD");

    tofFlanAVG.powerON("FlanAVG");
    tofFlanAVD.powerON("FlanAVD");
    tofFlanARG.powerON("FlanARG");
    tofFlanARD.powerON("FlanARD");

    tofARG.powerON("ARG");
    tofARD.powerON("ARD");

    while (true) {
        //uint32_t mesure1 = tofAVD.getMesure();
        //uint32_t mesure2 = tofAV.getMesure();
        //uint32_t mesure3 = tofAVG.getMesure();
        //uint32_t mesure4 = tofFlanAVD.getMesure();
        //Serial.printf("AVG=%u AV=%u AVD=%u FAVD=%u\n", mesure3, mesure2, mesure1, mesure4);
        //delay(20);

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
