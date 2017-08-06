#ifndef _MOTOR_h
#define _MOTOR_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include "pin_mapping.h"

class Motor
{
public:
	Motor()
	{
		pinMode(PIN_LEFT_MOTOR_A, OUTPUT);
		pinMode(PIN_RIGHT_MOTOR_A, OUTPUT);
		pinMode(PIN_LEFT_MOTOR_B, OUTPUT);
		pinMode(PIN_RIGHT_MOTOR_B, OUTPUT);

		// La résolution des PWM est 10bits (0-1023)
		analogWriteResolution(10);

		// Réglage de la fréquence des PWM
		analogWriteFrequency(PIN_LEFT_MOTOR_A, 35156.25);
		analogWriteFrequency(PIN_RIGHT_MOTOR_A, 35156.25);

		// Initialisation : Moteurs arrêtés
		analogWrite(PIN_LEFT_MOTOR_A, 0);
		analogWrite(PIN_RIGHT_MOTOR_A, 0);
		digitalWrite(PIN_LEFT_MOTOR_B, LOW);
		digitalWrite(PIN_RIGHT_MOTOR_B, LOW);
	}

	void runLeft(int16_t pwm)
	{
		if (pwm >= 0)
		{
			digitalWrite(PIN_LEFT_MOTOR_B, LOW);
			if (pwm > 1023)
				pwm = 1023;
		}
		else
		{
			digitalWrite(PIN_LEFT_MOTOR_B, HIGH);
			pwm = -pwm;
			if (pwm > 1023)
				pwm = 1023;
			pwm = 1023 - pwm;
		}
		analogWrite(PIN_LEFT_MOTOR_A, pwm);
	}

	void runRight(int16_t pwm)
	{
		if (pwm >= 0)
		{
			digitalWrite(PIN_RIGHT_MOTOR_B, HIGH);
			if (pwm > 1023)
				pwm = 1023;
			pwm = 1023 - pwm;
		}
		else
		{
			digitalWrite(PIN_RIGHT_MOTOR_B, LOW);
			pwm = -pwm;
			if (pwm > 1023)
				pwm = 1023;
		}
		analogWrite(PIN_RIGHT_MOTOR_A, pwm);
	}
};

#endif