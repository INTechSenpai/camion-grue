#ifndef _MOTOR_h
#define _MOTOR_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include "../Config/pin_mapping.h"

#define MAX_PWM 1023


class Motor
{
public:
	Motor()
	{
        // Passage des pins en OUTPUT
		pinMode(PIN_EN_FRONT_LEFT_MOTOR, OUTPUT);
		pinMode(PIN_A_FRONT_LEFT_MOTOR, OUTPUT);
		pinMode(PIN_B_FRONT_LEFT_MOTOR, OUTPUT);
		pinMode(PIN_EN_FRONT_RIGHT_MOTOR, OUTPUT);
		pinMode(PIN_A_FRONT_RIGHT_MOTOR, OUTPUT);
		pinMode(PIN_B_FRONT_RIGHT_MOTOR, OUTPUT);
		pinMode(PIN_EN_BACK_LEFT_MOTOR, OUTPUT);
		pinMode(PIN_A_BACK_LEFT_MOTOR, OUTPUT);
		pinMode(PIN_B_BACK_LEFT_MOTOR, OUTPUT);
		pinMode(PIN_EN_BACK_RIGHT_MOTOR, OUTPUT);
		pinMode(PIN_A_BACK_RIGHT_MOTOR, OUTPUT);
		pinMode(PIN_B_BACK_RIGHT_MOTOR, OUTPUT);

		// La résolution des PWM est 10bits (0-1023)
		analogWriteResolution(10);

		// Réglage de la fréquence des PWM
		analogWriteFrequency(PIN_EN_FRONT_LEFT_MOTOR, 35156.25);
		analogWriteFrequency(PIN_EN_FRONT_RIGHT_MOTOR, 35156.25);
		analogWriteFrequency(PIN_EN_BACK_LEFT_MOTOR, 35156.25);
		analogWriteFrequency(PIN_EN_BACK_RIGHT_MOTOR, 35156.25);

		// Initialisation : Moteurs arrêtés (en mode roue "libre")
        breakAll(0);
	}

    void runFrontLeft(int16_t pwm)
    {
        runMotor(pwm, PIN_EN_FRONT_LEFT_MOTOR, PIN_A_FRONT_LEFT_MOTOR, PIN_B_FRONT_LEFT_MOTOR);
    }

    void runFrontRight(int16_t pwm)
    {
        runMotor(-pwm, PIN_EN_FRONT_RIGHT_MOTOR, PIN_A_FRONT_RIGHT_MOTOR, PIN_B_FRONT_RIGHT_MOTOR);
    }

    void runBackLeft(int16_t pwm)
    {
        runMotor(pwm, PIN_EN_BACK_LEFT_MOTOR, PIN_A_BACK_LEFT_MOTOR, PIN_B_BACK_LEFT_MOTOR);
    }

    void runBackRight(int16_t pwm)
    {
        runMotor(-pwm, PIN_EN_BACK_RIGHT_MOTOR, PIN_A_BACK_RIGHT_MOTOR, PIN_B_BACK_RIGHT_MOTOR);
    }

    void breakAll(uint16_t strength = MAX_PWM)
    {
        digitalWrite(PIN_A_FRONT_LEFT_MOTOR, LOW);
        digitalWrite(PIN_B_FRONT_LEFT_MOTOR, LOW);
        digitalWrite(PIN_A_FRONT_RIGHT_MOTOR, LOW);
        digitalWrite(PIN_B_FRONT_RIGHT_MOTOR, LOW);
        digitalWrite(PIN_A_BACK_LEFT_MOTOR, LOW);
        digitalWrite(PIN_B_BACK_LEFT_MOTOR, LOW);
        digitalWrite(PIN_A_BACK_RIGHT_MOTOR, LOW);
        digitalWrite(PIN_B_BACK_RIGHT_MOTOR, LOW);
        analogWrite(PIN_EN_FRONT_LEFT_MOTOR, strength);
        analogWrite(PIN_EN_FRONT_RIGHT_MOTOR, strength);
        analogWrite(PIN_EN_BACK_LEFT_MOTOR, strength);
        analogWrite(PIN_EN_BACK_RIGHT_MOTOR, strength);
    }

private:
    void runMotor(int16_t pwm, uint8_t pin_en, uint8_t pin_a, uint8_t pin_b)
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
};

#endif