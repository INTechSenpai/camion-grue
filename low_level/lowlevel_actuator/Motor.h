#ifndef _MOTOR_h
#define _MOTOR_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include "config.h"

#define MAX_PWM 1023


class Motor
{
public:
	Motor(uint8_t pin_en, uint8_t pin_a, uint8_t pin_b) :
        pin_en(pin_en), pin_a(pin_a), pin_b(pin_b)
	{
        // Passage des pins en OUTPUT
		pinMode(pin_en, OUTPUT);
		pinMode(pin_a, OUTPUT);
		pinMode(pin_b, OUTPUT);

		// La résolution des PWM est 10bits (0-1023)
		analogWriteResolution(10);

		// Réglage de la fréquence des PWM
		analogWriteFrequency(pin_en, 35156.25);

		// Initialisation : Moteurs arrêtés (en mode roue "libre")
        breakMotor(0);
	}

    void run(int16_t pwm)
    {
        if (pwm >= 0)
        {
            digitalWrite(pin_a, HIGH);
            digitalWrite(pin_b, LOW);
        }
        else
        {
            digitalWrite(pin_a, LOW);
            digitalWrite(pin_b, HIGH);
            pwm = -pwm;
        }
        if (pwm > MAX_PWM)
        {
            pwm = MAX_PWM;
        }
        analogWrite(pin_en, pwm);
    }

    void breakMotor(uint16_t strength = MAX_PWM)
    {
        digitalWrite(pin_a, LOW);
        digitalWrite(pin_b, LOW);
        analogWrite(pin_en, strength);
    }

private:
    const uint8_t pin_en;
    const uint8_t pin_a;
    const uint8_t pin_b;
};

#endif
