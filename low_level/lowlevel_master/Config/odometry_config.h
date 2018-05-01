#ifndef _ODOMETRY_CONFIG_h
#define _ODOMETRY_CONFIG_h

/* TICK_TO_MM
Th�orique : 0.0812
R�el, roues std : 0.0837
R�el, roues avec �lastiques : 0.0829
*/
#define TICK_TO_MM				0.0829      // Conversion ticks-mm pour les roues codeuses arri�res. Unit� : mm/tick

/* TICK_TO_RADIANS
Th�orique : 0.00108
R�el, roues std : 0.001101
R�el, roues avec �lastiques : ?
*/
#define TICK_TO_RADIANS			0.00108    // Conversion ticks-radians. Unit� : radian/tick

#define MOTOR_TICK_TO_TICK		2.4			// Conversion ticks_des_roues_avant --> ticks. Unit� : tick/ticks_des_roues_avant
#define AVERAGE_SPEED_SIZE      50          // Nombre de valeurs � utiliser dans le calcul de la moyenne glissante permettant de lisser la mesure de vitesse

#endif
