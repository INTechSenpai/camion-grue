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


#define PERIOD_ASSERV			1000		// Dur�e entre deux asservissement (en �s)
#define FREQ_ASSERV				1000		// Fr�quence d'asservissement (en Hz)
#define AVERAGE_SPEED_SIZE		50			// Nombre de valeurs � utiliser dans le calcul de la moyenne glissante permettant de lisser la mesure de vitesse
#define TRAJECTORY_STEP			206			// Distance (en ticks, correspondant � une translation) entre deux points d'une trajectoire
#define TICK_TO_MM				0.09721		// Conversion ticks-mm pour les roues codeuses arri�res. Unit� : mm/tick
#define TICK_TO_RADIANS			0.001058	// Conversion ticks-radians. Unit� : radian/tick
#define FRONT_TICK_TO_TICK		2.4			// Conversion ticks_des_roues_avant --> ticks. Unit� : tick/ticks_des_roues_avant
#define CURVATURE_TOLERANCE		0.3			// Ecart maximal entre la consigne en courbure et la courbure r�elle admissible au d�marrage. Unit� : m^-1
#define MOTOR_SLIP_TOLERANCE	200			// Erreur maximale de rotation enregistrable pour les moteurs de propulsion avant de d�tecter un d�rapage. Unit� : ticks
#define TIMEOUT_MOVE_INIT		1000		// Dur�e maximale le la phase "MOVE_INIT" d'une trajectoire. Unit� : ms
#define DISTANCE_MAX_TO_TRAJ	40			// Distance (entre notre position et la trajectoire) au del� de laquelle on abandonne la trajectoire. Unit� : mm



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

	// Prochain point d'arr�t sur la trajectoire courante. Tant qu'aucun point d'arr�t n'a �t� re�u, nextStopPoint vaut MAX_UINT_16.
	volatile uint16_t nextStopPoint;

	// Position absolue du robot sur la table (en mm et radians)
	volatile Position position;


	/*
	* 		D�finition des variables d'�tat du syst�me (position, vitesse, consigne, ...)
	*
	* 		Les unit�s sont :
	* 			Pour les distances		: ticks
	* 			Pour les vitesses		: ticks/seconde
	* 			Ces unit�s seront vraies pour une fr�quence d'asservissement �gale � FREQ_ASSERV
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
	//  (Ici toutes les grandeurs sont positives, le sens de d�placement sera donn� par maxMovingSpeed)
	PID forwardTranslationPID;
	PID backwardTranslationPID;
	volatile int32_t translationSetpoint;	// ticks
	volatile int32_t currentTranslation;	// ticks
	float currentTranslation_float;			// utilis� pour les calculs pr�cis (tjrs mis � jour en m�me tps que currentTranslation)
	volatile int32_t movingSpeedSetpoint;	// ticks/seconde
	
	StoppingMgr endOfMoveMgr;
	volatile int32_t currentMovingSpeed;	// ticks/seconde

	//  Asservissement sur trajectoire
	volatile float curvatureOrder;	// Consigne de courbure, en m^-1
	float curvatureCorrectorK1;		// Coefficient du facteur "erreur de position"
	float curvatureCorrectorK2;		// Coefficient du facteur "erreur d'orientation"

	// Facteurs multiplicatifs � appliquer � la distance parcourue par les roues gauche et droite, en fonction de la courbure courante.
	volatile float leftSideDistanceFactor;
	volatile float rightSideDistanceFactor;
	
	//  Vitesse (alg�brique) de translation maximale : une vitesse n�gative correspond � une marche arri�re
	int32_t maxMovingSpeed;	// en ticks/seconde

	//  Pour le calcul de l'acc�l�ration :
	volatile int32_t previousMovingSpeedSetpoint;	// en ticks.s^-2

	//  Acc�l�ration maximale (variation maximale de movingSpeedSetpoint)
	int32_t maxAcceleration;	// ticks*s^-2
	int32_t maxDeceleration;	// ticks*s^-2

	//	Pour faire de jolies courbes de r�ponse du syst�me, la vitesse moyenne c'est mieux !
	Average<int32_t, AVERAGE_SPEED_SIZE> averageLeftSpeed;
	Average<int32_t, AVERAGE_SPEED_SIZE> averageRightSpeed;
	Average<int32_t, AVERAGE_SPEED_SIZE> averageTranslationSpeed;

	//  Pour mesurer le temps pass� dans l'interruption d'asservissement
	uint32_t lastInterruptDuration; // �s
	uint32_t maxInterruptDuration; // �s

	//  Classe permettant une visualisation facile des grandeurs li�es � l'asservissement sur trajectoire
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
	// Type d�crivant l'�tat du mouvement
	enum MovingState
	{
		STOPPED,		// Robot � l'arr�t, � la position voulue.
		HIGHLEVEL_STOP, // Robot arr�t� en cours de trajectoire sur ordre du haut niveau.
		MOVE_INIT,		// L'ordre de mouvement a �t� re�u, mais le robot n'envoie pas encore un PWM aux moteurs de propulsion (il attend d'avoir les roues de direction en position)
		MOVING,			// Robot en mouvent vers la position voulue.
		MANUAL_MOVE,	// Robot en mouvement, sans utiliser les points de trajectoire.
		EXT_BLOCKED,	// Robot bloqu� par un obstacle ext�rieur (les roues patinent).
		INT_BLOCKED,	// Roues du robot bloqu�es.
		EMPTY_TRAJ,		// La trajectoire courante est termin�e, le dernier point n'�tant pas un point d'arr�t.
		FAR_AWAY		// Le robot se trouve trop loin de la position indiqu�e par le point de trajectoire courant.
	};

	// Type d�signant le PID en cours de r�glage
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
	volatile bool trajectoryFullyCompleted; // Indique que tous les points de la derni�re trajectoire ont �t�s rendus obsol�tes
	bool matchTerminated;

	// Variables d'activation des diff�rents PID
	bool positionControlled;	//  Asservissement en position
	bool leftSpeedControlled;	//	Asservissement en vitesse � gauche
	bool rightSpeedControlled;	//	Asservissement en vitesse � droite
	bool pwmControlled;			//	Mise � jour des PWM gr�ce � l'asservissement en vitesse

	PIDtoSet pidToSet; // Indique lequel des PID est en cours de r�glage

public:
	MotionControlSystem();

	/* Asservissement (fonction � appeller dans l'interruption associ�e) */
	void control();
private:
	/* Mise � jour des variables :
		position
		currentRightSpeed (maj + filtrage)
		currentLeftSpeed (maj + filtrage)
		currentTranslation
		currentMovingSpeed (maj + filtrage) */
	void updateSpeedAndPosition();

	/* Mise � jour des variables :
		trajectoryIndex
		currentTrajectory (rend obsol�te les points qui le sont)
		translationSetpoint (si trajectoryIndex a �t� incr�ment�) */
	void updateTrajectoryIndex();
	
	/* Mise � jour de : 
		nextStopPoint */
	void updateNextStopPoint();

	/* V�rifie la validit� des prochains points de la trajectoire 
		MOVING : le point courant et le prochain point doivent �tre valides (sauf si on se trouve sur un point d'arr�t)
		MOVE_INIT : le point courant doit �tre valide
		Autre : aucune v�rification
	*/
	void checkTrajectory();

	/* V�rifie que l'on ne se trouve pas trop loin du point courant de la trajectoire, lors d'un mouvement (movingState == MOVING)
		Interromp la trajectoire courante si besoin.
	*/
	void checkPosition();

	/* Mise � jour de translationSetpoint
		il est suppos� que le robot se situe entre deux points de trajectoire 
		(si ce n'est pas le cas, ET que l'on se trouve dans une phase de d�c�l�ration, il y aura une discontinuti� de la consigne)
	*/
	void updateTranslationSetpoint();

	/* Donne le facteur de multiplication de distance parcourue par les roues gauche et droite 
		(en fonction de la courbure de la trajectoire courante) */
	void updateSideDistanceFactors();

	void manageStop();
	void manageBlocking();

	/* Appel�e en cas d'erreur durant le mouvement, r�initialise la trajectoire courante */
	void clearCurrentTrajectory();
public:

	/* Activation et d�sactivation de l'asserv */
	void enablePositionControl(bool);
	void enableLeftSpeedControl(bool);
	void enableRightSpeedControl(bool);
	void enablePwmControl(bool);
	void getEnableStates(bool &, bool &, bool &, bool&);

	/* Gestion des d�placements */
	void addTrajectoryPoint(const TrajectoryPoint &, uint8_t);
	MovingState getMovingState() const;
	void gotoNextStopPoint();
	void moveUranus(bool);
	void stop(); // Met toutes les consignes de l'asservissement � une valeur permettant l'arr�t du robot
	void highLevelStop(); // Termine la trajectoire courante et stoppe le robot
	void endMatchStop(); // Termine la trajectoire courante et emp�che d'en effectuer une nouvelle
	bool isStopped() const; // Indique si le robot est physiquement � l'arr�t
	bool isBreaking() const; // Indique si le robot est en train de ralentir
	void setMaxMovingSpeed(int32_t); // R�gle la vitess maximale de translation (argument pass� en mm/s)
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

	/* Setters et getters des gestionaires de blocage et d'arr�t */
	void setLeftMotorBmgrTunings(float, uint32_t);
	void setRightMotorBmgrTunings(float, uint32_t);
	void setEndOfMoveMgrTunings(uint32_t, uint32_t);
	void getLeftMotorBmgrTunings(float &, uint32_t &) const;
	void getRightMotorBmgrTunings(float &, uint32_t &) const;
	void getEndOfMoveMgrTunings(uint32_t &, uint32_t &) const;

	/* Getters de d�bug */
	void getTicks(int32_t &, int32_t &, int32_t &, int32_t &);

	/* Sauvegarde et restauration des param�tres de l'asservissement */
	void saveParameters();
	void loadParameters();
	void loadDefaultParameters();

	/* Log les donn�es de l'asservissement 
	(� une fr�quence proche de celle de l'interruption d'asservissement) */
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

