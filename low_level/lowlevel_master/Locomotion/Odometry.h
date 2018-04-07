#ifndef _ODOMETRY_h
#define _ODOMETRY_h

/*
	Classe permettant de calculer la vitesse et la position d'un ensemble d'encodeurs.
*/


#include <Encoder.h>
#include "Position.h"
#include "MotorSpeedSensor.h"
#include "../Tools/Average.h"
#include "../Config/pin_mapping.h"
#include "../Config/odometry_config.h"


class Odometry
{
public:
	Odometry(
		const float freqAsserv,
		volatile Position & p, 
		volatile float & frontLeftMotorSpeed,
        volatile float & frontRightMotorSpeed,
        volatile float & backLeftMotorSpeed,
		volatile float & backRightMotorSpeed,
		volatile float & currentTranslation,
		volatile float & translationSpeed
	) :
        freqAsserv(freqAsserv),
        frontLeftMotor(freqAsserv, PIN_B_FRONT_LEFT_MOTOR_ENCODER, PIN_A_FRONT_LEFT_MOTOR_ENCODER, frontLeftMotorSpeed),
        frontRightMotor(freqAsserv, PIN_A_FRONT_RIGHT_MOTOR_ENCODER, PIN_B_FRONT_RIGHT_MOTOR_ENCODER, frontRightMotorSpeed),
        backLeftMotor(freqAsserv, PIN_B_BACK_LEFT_MOTOR_ENCODER, PIN_A_BACK_LEFT_MOTOR_ENCODER, backLeftMotorSpeed),
        backRightMotor(freqAsserv, PIN_A_BACK_RIGHT_MOTOR_ENCODER, PIN_B_BACK_RIGHT_MOTOR_ENCODER, backRightMotorSpeed),
		leftOdometryEncoder(PIN_A_LEFT_ENCODER, PIN_B_LEFT_ENCODER),
		rightOdometryEncoder(PIN_B_RIGHT_ENCODER, PIN_A_RIGHT_ENCODER),
		position(p),
		currentTranslation(currentTranslation),
		translationSpeed(translationSpeed)
	{
		leftOdometryTicks = 0;
		rightOdometryTicks = 0;
		previousLeftOdometryTicks = 0;
		previousRightOdometryTicks = 0;
		deltaLeftOdometryTicks = 0;
		deltaRightOdometryTicks = 0;
		half_deltaRotation_rad = 0;
		currentAngle = 0;
		corrector = 0;
		deltaTranslation = 0;
	}


	/*
		Lit les valeurs des encodeurs, et met à jour la position et les vitesses en conséquences.
	*/
	inline void compute(bool movingForward)
	{
        // Mise à jour de la vitesse des moteurs de propulsion
        frontLeftMotor.compute();
        frontRightMotor.compute();
        backLeftMotor.compute();
        backRightMotor.compute();

		// Récupération des données des encodeurs
		leftOdometryTicks = leftOdometryEncoder.read();
		rightOdometryTicks = rightOdometryEncoder.read();

		// Calcul du mouvement de chaque roue depuis le dernier asservissement
		deltaLeftOdometryTicks = leftOdometryTicks - previousLeftOdometryTicks;
		deltaRightOdometryTicks = rightOdometryTicks - previousRightOdometryTicks;

		previousLeftOdometryTicks = leftOdometryTicks;
		previousRightOdometryTicks = rightOdometryTicks;

		// Mise à jour de la position et de l'orientattion
		deltaTranslation = (((float)deltaLeftOdometryTicks + (float)deltaRightOdometryTicks) / 2) * TICK_TO_MM;
		half_deltaRotation_rad = (((float)deltaRightOdometryTicks - (float)deltaLeftOdometryTicks) / 4) * TICK_TO_RADIANS;
		currentAngle = position.orientation + half_deltaRotation_rad;
		position.setOrientation(position.orientation + half_deltaRotation_rad * 2);
		corrector = 1 - square(half_deltaRotation_rad) / 6;
		position.x += corrector * deltaTranslation * cosf(currentAngle);
		position.y += corrector * deltaTranslation * sinf(currentAngle);

		// Mise à jour de currentTranslation
		if (movingForward)
		{
			currentTranslation += deltaTranslation;
		}
		else
		{
			currentTranslation -= deltaTranslation;
		}

		// Mise à jour de la vitesse de translation
		translationSpeed = deltaTranslation * freqAsserv;
		averageTranslationSpeed.add(translationSpeed);
		translationSpeed = averageTranslationSpeed.value();
	}

private:
	const float freqAsserv;	// Fréquence d'appel de la méthode 'compute'. Utilisée pour le calcul des vitesses.

    MotorSpeedSensor frontLeftMotor;
    MotorSpeedSensor frontRightMotor;
    MotorSpeedSensor backLeftMotor;
    MotorSpeedSensor backRightMotor;

	Encoder leftOdometryEncoder;
	Encoder rightOdometryEncoder;

	volatile Position & position;			// Position courante dans le référentiel de la table. Unités mm;mm;radians
	volatile float & currentTranslation;	// Distance parcourue par le robot en translation (avant-arrière). Unité : mm
	volatile float & translationSpeed;		// Vitesse moyennée du robot selon son axe avant-arrière. Unité : mm/s

	int32_t
		leftOdometryTicks,
		rightOdometryTicks;
	int32_t
		previousLeftOdometryTicks,
		previousRightOdometryTicks;
	int32_t
		deltaLeftOdometryTicks,
		deltaRightOdometryTicks;
	float
		half_deltaRotation_rad,
		currentAngle,
		corrector,
		deltaTranslation;

	Average<float, AVERAGE_SPEED_SIZE> averageTranslationSpeed;
};


#endif