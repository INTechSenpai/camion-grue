#ifndef _ODOMETRY_h
#define _ODOMETRY_h

/*
	Classe permettant de calculer la vitesse et la position d'un ensemble d'encodeurs.
*/


#include <Encoder.h>
#include "Position.h"
#include "../Tools/Average.h"
#include "../Config/pin_mapping.h"


#define TICK_TO_MM				0.09721		// Conversion ticks-mm pour les roues codeuses arrières. Unité : mm/tick
#define TICK_TO_RADIANS			0.001058	// Conversion ticks-radians. Unité : radian/tick
#define FRONT_TICK_TO_TICK		2.4			// Conversion ticks_des_roues_avant --> ticks. Unité : tick/ticks_des_roues_avant
#define AVERAGE_SPEED_SIZE		50			// Nombre de valeurs à utiliser dans le calcul de la moyenne glissante permettant de lisser la mesure de vitesse


class Odometry
{
public:
	Odometry(
		float freqAsserv,
		volatile Position & p, 
		volatile float & leftMotorSpeed, 
		volatile float & rightMotorSpeed, 
		volatile float & currentTranslation, 
		volatile float & translationSpeed
	) :
		leftMotorEncoder(-1, -1),
		rightMotorEncoder(-1, -1),
		leftOdometryEncoder(-1, -1),
		rightOdometryEncoder(-1, -1),
		position(p),
		leftMotorSpeed(leftMotorSpeed),
		rightMotorSpeed(rightMotorSpeed),
		currentTranslation(currentTranslation),
		translationSpeed(translationSpeed)
	{
		this->freqAsserv = freqAsserv;
		leftMotorTicks = 0;
		rightMotorTicks = 0;
		leftOdometryTicks = 0;
		rightOdometryTicks = 0;
		previousLeftMotorTicks = 0;
		previousRightMotorTicks = 0;
		previousLeftOdometryTicks = 0;
		previousRightOdometryTicks = 0;
		deltaLeftMotorTicks = 0;
		deltaRightMotorTicks = 0;
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
		// Récupération des données des encodeurs
		leftMotorTicks = leftMotorEncoder.read();
		rightMotorTicks = rightMotorEncoder.read();
		leftOdometryTicks = leftOdometryEncoder.read();
		rightOdometryTicks = rightOdometryEncoder.read();

		// Calcul du mouvement de chaque roue depuis le dernier asservissement
		deltaLeftMotorTicks = leftMotorTicks - previousLeftMotorTicks;
		deltaRightMotorTicks = rightMotorTicks - previousRightMotorTicks;
		deltaLeftOdometryTicks = leftOdometryTicks - previousLeftOdometryTicks;
		deltaRightOdometryTicks = rightOdometryTicks - previousRightOdometryTicks;

		previousLeftMotorTicks = leftMotorTicks;
		previousRightMotorTicks = rightMotorTicks;
		previousLeftOdometryTicks = leftOdometryTicks;
		previousRightOdometryTicks = rightOdometryTicks;

		// Mise à jour de la vitesse des moteurs
		leftMotorSpeed = (float)deltaLeftMotorTicks * freqAsserv * TICK_TO_MM * FRONT_TICK_TO_TICK;
		rightMotorSpeed = (float)deltaRightMotorTicks * freqAsserv * TICK_TO_MM * FRONT_TICK_TO_TICK;
		averageLeftSpeed.add(leftMotorSpeed);
		averageRightSpeed.add(rightMotorSpeed);
		leftMotorSpeed = averageLeftSpeed.value();
		rightMotorSpeed = averageRightSpeed.value();

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
	float freqAsserv;	// Fréquence d'appel de la méthode 'compute'. Utilisée pour le calcul des vitesses.

	Encoder leftMotorEncoder;
	Encoder rightMotorEncoder;
	Encoder leftOdometryEncoder;
	Encoder rightOdometryEncoder;

	volatile Position & position;			// Position courante dans le référentiel de la table. Unités mm;mm;radians
	volatile float & leftMotorSpeed;		// Vitesse instantanée moyennée du moteur gauche. Unité : mm/s
	volatile float & rightMotorSpeed;		// Vitesse instantanée moyennée du moteur droit. Unité : mm/s
	volatile float & currentTranslation;	// Distance parcourue par le robot en translation (avant-arrière). Unité : mm
	volatile float & translationSpeed;		// Vitesse moyennée du robot selon son axe avant-arrière. Unité : mm/s

	int32_t
		leftMotorTicks,
		rightMotorTicks,
		leftOdometryTicks,
		rightOdometryTicks;
	int32_t
		previousLeftMotorTicks,
		previousRightMotorTicks,
		previousLeftOdometryTicks,
		previousRightOdometryTicks;
	int32_t
		deltaLeftMotorTicks,
		deltaRightMotorTicks,
		deltaLeftOdometryTicks,
		deltaRightOdometryTicks;
	float
		half_deltaRotation_rad,
		currentAngle,
		corrector,
		deltaTranslation;

	Average<float, AVERAGE_SPEED_SIZE> averageLeftSpeed;
	Average<float, AVERAGE_SPEED_SIZE> averageRightSpeed;
	Average<float, AVERAGE_SPEED_SIZE> averageTranslationSpeed;
};


#endif