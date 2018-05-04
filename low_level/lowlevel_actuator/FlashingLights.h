#ifndef FLASHING_LIGHTS_h
#define FLASHING_LIGHTS_h

#include "config.h"


class FlashingLights
{
public:
    FlashingLights(uint32_t onDuration, uint32_t offDurationLeft, uint32_t offDurationRight) :
        leftLed(PIN_DEL_GYRO_G, onDuration, offDurationLeft),
        rightLed(PIN_DEL_GYRO_D, onDuration, offDurationRight)
    {
    }

    void update()
    {
        leftLed.update();
        rightLed.update();
    }

private:
    class LedBlinker
    {
    public:
        LedBlinker(uint8_t pin, uint32_t onDuration, uint32_t offDuration):
            pin(pin),
            onDuration(onDuration),
            offDuration(offDuration)
        {
            isOn = false;
            lastToggleTime = 0;
            pinMode(pin, OUTPUT);
        }

        void update()
        {
            uint32_t now = millis();
            uint32_t delta = now - lastToggleTime;
            if (isOn && delta > onDuration)
            {
                digitalWrite(pin, LOW);
                isOn = false;
                lastToggleTime = now;
            }
            else if (!isOn && delta > offDuration)
            {
                digitalWrite(pin, HIGH);
                isOn = true;
                lastToggleTime = now;
            }
        }

    private:
        const uint8_t pin;
        const uint32_t onDuration;
        const uint32_t offDuration;
        bool isOn;
        uint32_t lastToggleTime;
    };

    LedBlinker leftLed;
    LedBlinker rightLed;
};


#endif
