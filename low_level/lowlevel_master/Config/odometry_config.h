#ifndef _ODOMETRY_CONFIG_h
#define _ODOMETRY_CONFIG_h

#define TICK_TO_MM				0.0837 //0.0812		// Conversion ticks-mm pour les roues codeuses arrières. Unité : mm/tick
#define TICK_TO_RADIANS			0.001101 //0.00108 	// Conversion ticks-radians. Unité : radian/tick
#define MOTOR_TICK_TO_TICK		2.4			// Conversion ticks_des_roues_avant --> ticks. Unité : tick/ticks_des_roues_avant
#define AVERAGE_SPEED_SIZE      50          // Nombre de valeurs à utiliser dans le calcul de la moyenne glissante permettant de lisser la mesure de vitesse

#endif
