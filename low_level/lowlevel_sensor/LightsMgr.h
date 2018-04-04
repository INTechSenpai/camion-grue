#ifndef _LIGHTS_MGR_h
#define _LIGHTS_MGR_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include "config.h"

#define DEFAULT_BLINK_DURATION  500 // ms
#define ALARM_BLINK_DURATION    100 // ms
#define DEFAULT_TOGGLE_DELAY    50 // ms


typedef uint8_t LightningMode;
enum LightId
{
    TURN_LEFT           = 1,
    TURN_RIGHT          = 2,
    FLASHING            = 4,
    STOP_LIGHT          = 8,
    REVERSE_LIGHT       = 16,
    NIGHT_LIGHT_LOW     = 32,
    NIGHT_LIGHT_HIGH    = 64,
    ALARM_LIGHT         = 128
};


class Led
{
public:
    Led(uint8_t aPin, float aToggleDelay = DEFAULT_TOGGLE_DELAY)
    {
        pin = aPin;
        aimState = false;
        power = 255;
        currentPWM = 0;
        toggleDelay = aToggleDelay;
        lastUpdateTime = 0;
        blinkOnDuration = 0;
        blinkOffDuration = 0;
        lastBlinkTime = 0;
        pinMode(pin, OUTPUT);
        analogWrite(pin, 0);
    }

    void turnOn(uint8_t aPower = 255)
    {
        aimState = true;
        power = aPower;
        blinkOnDuration = 0;
        blinkOffDuration = 0;
    }

    void turnOff()
    {
        aimState = false;
        blinkOnDuration = 0;
        blinkOffDuration = 0;
    }

    void blink(uint32_t onDuration, uint32_t offDuration)
    {
        if (blinkOnDuration == 0 || blinkOffDuration == 0)
        {
            lastBlinkTime = millis();
        }
        blinkOnDuration = onDuration;
        blinkOffDuration = offDuration;
    }

    void update()
    {
        uint32_t now = millis();

        if (blinkOnDuration > 0 && blinkOffDuration > 0)
        {
            if (aimState && now - lastBlinkTime > blinkOnDuration)
            {
                aimState = false;
                lastBlinkTime += blinkOnDuration;
            }
            else if (!aimState && now - lastBlinkTime > blinkOffDuration)
            {
                aimState = true;
                lastBlinkTime += blinkOffDuration;
            }
        }

        float increment = (float)power * (float)(now - lastUpdateTime) / toggleDelay;
        lastUpdateTime = now;
        if (aimState)
        {
            currentPWM += (int32_t)increment;
        }
        else
        {
            currentPWM -= (int32_t)increment;
        }
        currentPWM = constrain(currentPWM, 0, (int32_t)power);
        analogWrite(pin, (uint8_t)currentPWM);
    }

private:
    uint8_t pin;
    bool aimState;
    uint8_t power;
    int32_t currentPWM;
    float toggleDelay;
    uint32_t lastUpdateTime;
    uint32_t blinkOnDuration;  // ms
    uint32_t blinkOffDuration; // ms
    uint32_t lastBlinkTime;
};


class FlashingLight
{
public:
    FlashingLight(uint8_t pin1, uint8_t pin2, uint8_t pin3, uint8_t pin4):
        led1(pin1, 150),
        led2(pin2, 150),
        led3(pin3, 150),
        led4(pin4, 150),
        delayToPropagate(150),
        blinkOn(150),
        blinkOff(350)
    {
        lastStartTime = 0;
        phase = 0;
    }

    void turnOn()
    {
        phase = 1;
        led1.blink(blinkOn, blinkOff);
        lastStartTime = millis();
    }

    void turnOff()
    {
        phase = 0;
        led1.turnOff();
        led2.turnOff();
        led3.turnOff();
        led4.turnOff();
    }

    void update()
    {
        uint32_t now = millis();
        if (now - lastStartTime > delayToPropagate)
        {
            if (phase == 1)
            {
                led2.blink(blinkOn, blinkOff);
                phase = 2;
            }
            else if (phase == 2)
            {
                led3.blink(blinkOn, blinkOff);
                phase = 3;
            }
            else if (phase == 3)
            {
                led4.blink(blinkOn, blinkOff);
                phase = 4;
            }
            lastStartTime = now;
        }
        led1.update();
        led2.update();
        led3.update();
        led4.update();
    }

private:
    Led led1;
    Led led2;
    Led led3;
    Led led4;
    const uint32_t delayToPropagate;  // ms
    uint32_t lastStartTime;     // ms
    uint8_t phase;
    const uint32_t blinkOn;     // ms
    const uint32_t blinkOff;    // ms
};


class LightsMgr
{
public:
    LightsMgr() :
        ledTurnLeft(PIN_DEL_CLIGNO_G),
        ledTurnRight(PIN_DEL_CLIGNO_D),
        flashingLight(PIN_DEL_GYRO_1, PIN_DEL_GYRO_2, PIN_DEL_GYRO_3, PIN_DEL_GYRO_4),
        ledStop(PIN_DEL_STOP),
        ledReverse(PIN_DEL_RECUL),
        ledNightFront(PIN_DEL_NUIT_AV),
        ledNightBack(PIN_DEL_NUIT_AR)
    {
        analogWriteResolution(8);
    }

    void setLightningMode(LightningMode mode)
    {
        if (mode & ALARM_LIGHT)
        {
            ledTurnLeft.blink(ALARM_BLINK_DURATION, ALARM_BLINK_DURATION);
            ledTurnRight.blink(ALARM_BLINK_DURATION, ALARM_BLINK_DURATION);
        }
        else
        {
            if (mode & TURN_LEFT)
            {
                ledTurnLeft.blink(DEFAULT_BLINK_DURATION, DEFAULT_BLINK_DURATION);
            }
            else
            {
                ledTurnLeft.turnOff();
            }
            if (mode & TURN_RIGHT)
            {
                ledTurnRight.blink(DEFAULT_BLINK_DURATION, DEFAULT_BLINK_DURATION);
            }
            else
            {
                ledTurnRight.turnOff();
            }
        }
        if (mode & REVERSE_LIGHT)
        {
            ledReverse.turnOn();
        }
        else
        {
            ledReverse.turnOff();
        }
        if (mode & NIGHT_LIGHT_HIGH)
        {
            ledNightFront.turnOn();
            ledNightBack.turnOn(64);
        }
        else if (mode & NIGHT_LIGHT_LOW)
        {
            ledNightFront.turnOn(32);
            ledNightBack.turnOn(64);
        }
        else
        {
            ledNightFront.turnOff();
            ledNightBack.turnOff();
        }
        if (mode & STOP_LIGHT)
        {
            ledStop.turnOn();
            ledNightBack.turnOn();
        }
        else
        {
            ledStop.turnOff();
        }
        if (mode & FLASHING)
        {
            flashingLight.turnOn();
        }
        else
        {
            flashingLight.turnOff();
        }
    }

    void update()
    {
        ledTurnLeft.update();
        ledTurnRight.update();
        flashingLight.update();
        ledStop.update();
        ledReverse.update();
        ledNightFront.update();
        ledNightBack.update();
    }

private:
    Led ledTurnLeft;
    Led ledTurnRight;
    FlashingLight flashingLight;
    Led ledStop;
    Led ledReverse;
    Led ledNightFront;
    Led ledNightBack;
};


#endif
