// MotionControlSystem.h

#ifndef _MOTIONCONTROLSYSTEM_h
#define _MOTIONCONTROLSYSTEM_h

#include "Singleton.h"
#include "Motor.h"
#include "PID.h"
#include "Average.h"
#include "Encoder.h"
#include "Position.h"
#include "TrajectoryPoint.h"
#include "DirectionController.h"
#include "BlockingMgr.h"
#include <math.h>
#include <EEPROM.h>


#define PERIOD_ASSERV			1000		// Durée entre deux asservissement (en µs)
#define FREQ_ASSERV				1000		// Fréquence d'asservissement (en Hz)
#define AVERAGE_SPEED_SIZE		50			// Nombre de valeurs à utiliser dans le calcul de la moyenne glissante permettant de lisser la mesure de vitesse
#define TRAJECTORY_STEP			206			// Distance (en ticks, correspondant à une translation) entre deux points d'une trajectoire
#define TICK_TO_MM				0.09721		// Conversion ticks-mm pour les roues codeuses arrières. Unité : mm/tick
#define TICK_TO_RADIANS			0.001058	// Conversion ticks-radians. Unité : radian/tick
#define FRONT_TICK_TO_TICK		2.4			// Conversion ticks_des_roues_avant --> ticks. Unité : tick/ticks_des_roues_avant
#define CURVATURE_TOLERANCE		0.3			// Ecart maximal entre la consigne en courbure et la courbure réelle admissible au démarrage. Unité : m^-1
#define MOTOR_SLIP_TOLERANCE	200			// Erreur maximale de rotation enregistrable pour les moteurs de propulsion avant de détecter un dérapage. Unité : ticks
#define TIMEOUT_MOVE_INIT		1000		// Durée maximale le la phase "MOVE_INIT" d'une trajectoire. Unité : ms
#define DISTANCE_MAX_TO_TRAJ	40			// Distance (entre notre position et la trajectoire) au delà de laquelle on abandonne la trajectoire. Unité : mm



class MotionControlSystem : public Singleton<MotionControlSystem>
{
private:
	Motor motor;
	Encoder leftMotorEncoder;
	Encoder rightMotorEncoder;

	Encoder leftFreeEncoder;
	Encoder rightFreeEncoder;

	DirectionController & direction;

	// Trajectoire en cours de parcours
	TrajectoryPoint currentTrajectory[UINT8_MAX + 1];
	
	// Point de la trajectoire courante sur lequel le robot se situe actuellement
	volatile uint8_t trajectoryIndex;

	// Prochain point d'arrêt sur la trajectoire courante. Tant qu'aucun point d'arrêt n'a été reçu, nextStopPoint vaut MAX_UINT_16.
	volatile uint16_t nextStopPoint;

	// Position absolue du robot sur la table (en mm et radians)
	volatile Position position;


	/*
	* 		Définition des variables d'état du système (position, vitesse, consigne, ...)
	*
	* 		Les unités sont :
	* 			Pour les distances		: ticks
	* 			Pour les vitesses		: ticks/seconde
	* 			Ces unités seront vraies pour une fréquence d'asservissement égale à FREQ_ASSERV
	*/

	//	Asservissement en vitesse du moteur droit
	PID rightSpeedPID;
	volatile int32_t rightSpeedSetpoint;	// ticks/seconde
	volatile int32_t currentRightSpeed;		// ticks/seconde
	volatile int32_t rightPWM;
	BlockingMgr rightMotorBlockingMgr;
	volatile int32_t rightMotorError;		// ticks

	//	Asservissement en vitesse du moteur gauche
	PID leftSpeedPID;
	volatile int32_t leftSpeedSetpoint;		// ticks/seconde
	volatile int32_t currentLeftSpeed;		// ticks/seconde
	volatile int32_t leftPWM;
	BlockingMgr leftMotorBlockingMgr;
	volatile int32_t leftMotorError;		// ticks

	//	Asservissement en position : translation
	//  (Ici toutes les grandeurs sont positives, le sens de déplacement sera donné par maxMovingSpeed)
	PID forwardTranslationPID;
	PID backwardTranslationPID;
	volatile int32_t translationSetpoint;	// ticks
	volatile int32_t currentTranslation;	// ticks
	float currentTranslation_float;			// utilisé pour les calculs précis (tjrs mis à jour en même tps que currentTranslation)
	volatile int32_t movingSpeedSetpoint;	// ticks/seconde
	
	StoppingMgr endOfMoveMgr;
	volatile int32_t currentMovingSpeed;	// ticks/seconde

	//  Asservissement sur trajectoire
	volatile float curvatureOrder;	// Consigne de courbure, en m^-1
	float curvatureCorrectorK1;		// Coefficient du facteur "erreur de position"
	float curvatureCorrectorK2;		// Coefficient du facteur "erreur d'orientation"

	// Facteurs multiplicatifs à appliquer à la distance parcourue par les roues gauche et droite, en fonction de la courbure courante.
	volatile float leftSideDistanceFactor;
	volatile float rightSideDistanceFactor;
	
	//  Vitesse (algébrique) de translation maximale : une vitesse négative correspond à une marche arrière
	int32_t maxMovingSpeed;	// en ticks/seconde

	//  Pour le calcul de l'accélération :
	volatile int32_t previousMovingSpeedSetpoint;	// en ticks.s^-2

	//  Accélération maximale (variation maximale de movingSpeedSetpoint)
	int32_t maxAcceleration;	// ticks*s^-2
	int32_t maxDeceleration;	// ticks*s^-2

	//	Pour faire de jolies courbes de réponse du système, la vitesse moyenne c'est mieux !
	Average<int32_t, AVERAGE_SPEED_SIZE> averageLeftSpeed;
	Average<int32_t, AVERAGE_SPEED_SIZE> averageRightSpeed;
	Average<int32_t, AVERAGE_SPEED_SIZE> averageTranslationSpeed;

	//  Pour mesurer le temps passé dans l'interruption d'asservissement
	uint32_t lastInterruptDuration; // µs
	uint32_t maxInterruptDuration; // µs

	//  Classe permettant une visualisation facile des grandeurs liées à l'asservissement sur trajectoire
	class WatchTrajErrors : public Printable
	{
	public:
		WatchTrajErrors()
		{
			traj_curv = 0;
			current_curv = 0;
			aim_curv = 0;
			angle_err = 0;
			pos_err = 0;
		}

		size_t printTo(Print& p) const
		{
			return p.printf("%g_%g_%g_%g_%g", traj_curv, current_curv, aim_curv, angle_err, pos_err);
		}

		float traj_curv;
		float current_curv;
		float aim_curv;
		float angle_err;
		float pos_err;
	};

	//  Classe permettant une visualisation du parcours de la trajectoire
	class WatchTrajIndex : public Printable
	{
	public:
		WatchTrajIndex()
		{
			index = 0;
		}

		size_t printTo(Print& p) const
		{
			return p.printf("%u_", index) + p.print(currentPos) + p.print("_") + p.print(aimPosition);
		}

		uint8_t index;
		Position currentPos;
		Position aimPosition;
	};

	WatchTrajErrors watchTrajErrors;
	WatchTrajIndex watchTrajIndex;

public:
	// Type décrivant l'état du mouvement
	enum MovingState
	{
		STOPPED,		// Robot à l'arrêt, à la position voulue.
		HIGHLEVEL_STOP, // Robot arrêté en cours de trajectoire sur ordre du haut niveau.
		MOVE_INIT,		// L'ordre de mouvement a été reçu, mais le robot n'envoie pas encore un PWM aux moteurs de propulsion (il attend d'avoir les roues de direction en position)
		MOVING,			// Robot en mouvent vers la position voulue.
		MANUAL_MOVE,	// Robot en mouvement, sans utiliser les points de trajectoire.
		EXT_BLOCKED,	// Robot bloqué par un obstacle extérieur (les roues patinent).
		INT_BLOCKED,	// Roues du robot bloquées.
		EMPTY_TRAJ,		// La trajectoire courante est terminée, le dernier point n'étant pas un point d'arrêt.
		FAR_AWAY		// Le robot se trouve trop loin de la position indiquée par le point de trajectoire courant.
	};

	// Type désignant le PID en cours de réglage
	enum PIDtoSet
	{
		LEFT_SPEED,
		RIGHT_SPEED,
		SPEED,
		TRANSLATION,
		REVERSE_TRANSLATION
	};
	
private:
	volatile MovingState movingState;
	volatile bool trajectoryFullyCompleted; // Indique que tous les points de la dernière trajectoire ont étés rendus obsolètes
	bool matchTerminated;

	// Variables d'activation des différents PID
	bool positionControlled;	//  Asservissement en position
	bool leftSpeedControlled;	//	Asservissement en vitesse à gauche
	bool rightSpeedControlled;	//	Asservissement en vitesse à droite
	bool pwmControlled;			//	Mise à jour des PWM grâce à l'asservissement en vitesse

	PIDtoSet pidToSet; // Indique lequel des PID est en cours de réglage

public:
	MotionControlSystem();

	/* Asservissement (fonction à appeller dans l'interruption associée) */
	void control();
private:
	/* Mise à jour des variables :
		position
		currentRightSpeed (maj + filtrage)
		currentLeftSpeed (maj + filtrage)
		currentTranslation
		currentMovingSpeed (maj + filtrage) */
	void updateSpeedAndPosition();

	/* Mise à jour des variables :
		trajectoryIndex
		currentTrajectory (rend obsolète les points qui le sont)
		translationSetpoint (si trajectoryIndex a été incrémenté) */
	void updateTrajectoryIndex();
	
	/* Mise à jour de : 
		nextStopPoint */
	void updateNextStopPoint();

	/* Vérifie la validité des prochains points de la trajectoire 
		MOVING : le point courant et le prochain point doivent être valides (sauf si on se trouve sur un point d'arrêt)
		MOVE_INIT : le point courant doit être valide
		Autre : aucune vérification
	*/
	void checkTrajectory();

	/* Vérifie que l'on ne se trouve pas trop loin du point courant de la trajectoire, lors d'un mouvement (movingState == MOVING)
		Interromp la trajectoire courante si besoin.
	*/
	void checkPosition();

	/* Mise à jour de translationSetpoint
		il est supposé que le robot se situe entre deux points de trajectoire 
		(si ce n'est pas le cas, ET que l'on se trouve dans une phase de décélération, il y aura une discontinutié de la consigne)
	*/
	void updateTranslationSetpoint();

	/* Donne le facteur de multiplication de distance parcourue par les roues gauche et droite 
		(en fonction de la courbure de la trajectoire courante) */
	void updateSideDistanceFactors();

	void manageStop();
	void manageBlocking();

	/* Appelée en cas d'erreur durant le mouvement, réinitialise la trajectoire courante */
	void clearCurrentTrajectory();
public:

	/* Activation et désactivation de l'asserv */
	void enablePositionControl(bool);
	void enableLeftSpeedControl(bool);
	void enableRightSpeedControl(bool);
	void enablePwmControl(bool);
	void getEnableStates(bool &, bool &, bool &, bool&);

	/* Gestion des déplacements */
	void addTrajectoryPoint(const TrajectoryPoint &, uint8_t);
	MovingState getMovingState() const;
	void gotoNextStopPoint();
	void moveUranus(bool);
	void stop(); // Met toutes les consignes de l'asservissement à une valeur permettant l'arrêt du robot
	void highLevelStop(); // Termine la trajectoire courante et stoppe le robot
	void endMatchStop(); // Termine la trajectoire courante et empêche d'en effectuer une nouvelle
	bool isStopped() const; // Indique si le robot est physiquement à l'arrêt
	bool isBreaking() const; // Indique si le robot est en train de ralentir
	void setMaxMovingSpeed(int32_t); // Règle la vitess maximale de translation (argument passé en mm/s)
	int32_t getMaxMovingSpeed() const;
	void setMaxAcceleration(int32_t);
	int32_t getMaxAcceleration() const;
	void setMaxDeceleration(int32_t);
	int32_t getMaxDeceleration() const;

	/* Setters et getters des constantes d'asservissement */
	void setCurrentPIDTunings(float, float, float);
	void getCurrentPIDTunings(float &, float &, float &) const;
	void setForwardTranslationTunings(float, float, float);
	void setBackwardTranslationTunings(float, float, float);
	void setLeftSpeedTunings(float, float, float);
	void setRightSpeedTunings(float, float, float);
	void setTrajectoryTunings(float, float);
	void getForwardTranslationTunings(float &, float &, float &) const;
	void getBackwardTranslationTunings(float &, float &, float &) const;
	void getLeftSpeedTunings(float &, float &, float &) const;
	void getRightSpeedTunings(float &, float &, float &) const;
	void getTrajectoryTunings(float &, float &) const;

	void setPIDtoSet(PIDtoSet newPIDtoSet);
	PIDtoSet getPIDtoSet() const;
	void getPIDtoSet_str(char*, size_t) const;

	/* Setter et getter de la position */
	void setPosition(const Position &);
	void getPosition(Position &) const;
	uint8_t getTrajectoryIndex() const;
	void resetPosition(void);

	/* Setters et getters des gestionaires de blocage et d'arrêt */
	void setLeftMotorBmgrTunings(float, uint32_t);
	void setRightMotorBmgrTunings(float, uint32_t);
	void setEndOfMoveMgrTunings(uint32_t, uint32_t);
	void getLeftMotorBmgrTunings(float &, uint32_t &) const;
	void getRightMotorBmgrTunings(float &, uint32_t &) const;
	void getEndOfMoveMgrTunings(uint32_t &, uint32_t &) const;

	/* Getters de débug */
	void getTicks(int32_t &, int32_t &, int32_t &, int32_t &);

	/* Sauvegarde et restauration des paramètres de l'asservissement */
	void saveParameters();
	void loadParameters();
	void loadDefaultParameters();

	/* Log les données de l'asservissement 
	(à une fréquence proche de celle de l'interruption d'asservissement) */
	void logAllData();

	void printCurrentTrajectory();

	uint32_t getLastInterruptDuration();
	uint32_t getMaxInterruptDuration();

	/* Tests */
	void setPWM(int32_t);
	void setSpeed(int32_t);
	void setTranslation(int32_t);

	void moveIsStarting();
};


#endif

