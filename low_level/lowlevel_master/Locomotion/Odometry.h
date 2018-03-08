#ifndef _ODOMETRY_h
#define _ODOMETRY_h

/*
	Classe permettant de calculer la vitesse et la position d'un ensemble d'encodeurs.
*/


#include <Encoder.h>
#include "Position.h"
#include "../Tools/Average.h"
#include "../Config/pin_mapping.h"


#define TICK_TO_MM				0.09721		// Conversion ticks-mm pour les roues codeuses arri�res. Unit� : mm/tick
#define TICK_TO_RADIANS			0.001058	// Conversion ticks-radians. Unit� : radian/tick
#define FRONT_TICK_TO_TICK		2.4			// Conversion ticks_des_roues_avant --> ticks. Unit� : tick/ticks_des_roues_avant
#define AVERAGE_SPEED_SIZE		50			// Nombre de valeurs � utiliser dans le calcul de la moyenne glissante permettant de lisser la mesure de vitesse


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
		Lit les valeurs des encodeurs, et met � jour la position et les vitesses en cons�quences.
	*/
	inline void compute(bool movingForward)
	{
		// R�cup�ration des donn�es des encodeurs
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

		// Mise � jour de la vitesse des moteurs
		leftMotorSpeed = (float)deltaLeftMotorTicks * freqAsserv * TICK_TO_MM * FRONT_TICK_TO_TICK;
		rightMotorSpeed = (float)deltaRightMotorTicks * freqAsserv * TICK_TO_MM * FRONT_TICK_TO_TICK;
		averageLeftSpeed.add(leftMotorSpeed);
		averageRightSpeed.add(rightMotorSpeed);
		leftMotorSpeed = averageLeftSpeed.value();
		rightMotorSpeed = averageRightSpeed.value();

		// Mise � jour de la position et de l'orientattion
		deltaTranslation = (((float)deltaLeftOdometryTicks + (float)deltaRightOdometryTicks) / 2) * TICK_TO_MM;
		half_deltaRotation_rad = (((float)deltaRightOdometryTicks - (float)deltaLeftOdometryTicks) / 4) * TICK_TO_RADIANS;
		currentAngle = position.orientation + half_deltaRotation_rad;
		position.setOrientation(position.orientation + half_deltaRotation_rad * 2);
		corrector = 1 - square(half_deltaRotation_rad) / 6;
		position.x += corrector * deltaTranslation * cosf(currentAngle);
		position.y += corrector * deltaTranslation * sinf(currentAngle);

		// Mise � jour de currentTranslation
		if (movingForward)
		{
			currentTranslation += deltaTranslation;
		}
		else
		{
			currentTranslation -= deltaTranslation;
		}

		// Mise � jour de la vitesse de translation
		translationSpeed = deltaTranslation * freqAsserv;
		averageTranslationSpeed.add(translationSpeed);
		translationSpeed = averageTranslationSpeed.value();
	}

private:
	float freqAsserv;	// Fr�quence d'appel de la m�thode 'compute'. Utilis�e pour le calcul des vitesses.

	Encoder leftMotorEncoder;
	Encoder rightMotorEncoder;
	Encoder leftOdometryEncoder;
	Encoder rightOdometryEncoder;

	volatile Position & position;			// Position courante dans le r�f�rentiel de la table. Unit�s mm;mm;radians
	volatile float & leftMotorSpeed;		// Vitesse instantan�e moyenn�e du moteur gauche. Unit� : mm/s
	volatile float & rightMotorSpeed;		// Vitesse instantan�e moyenn�e du moteur droit. Unit� : mm/s
	volatile float & currentTranslation;	// Distance parcourue par le robot en translation (avant-arri�re). Unit� : mm
	volatile float & translationSpeed;		// Vitesse moyenn�e du robot selon son axe avant-arri�re. Unit� : mm/s

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