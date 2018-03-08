#ifndef _TRAJECTORY_FOLLOWER_h
#define _TRAJECTORY_FOLLOWER_h

#include "DirectionController.h"
#include "Odometry.h"
#include "PID.h"
#include "CurvaturePID.h"
#include "BlockingMgr.h"
#include "Motor.h"
#include "Position.h"
#include "MoveState.h"
#include "MotionControlTunings.h"


#define CURVATURE_TOLERANCE		0.3			// Ecart maximal entre la consigne en courbure et la courbure réelle admissible au démarrage. Unité : m^-1
#define DISTANCE_MAX_TO_TRAJ	40			// Distance (entre notre position et la trajectoire) au delà de laquelle on abandonne la trajectoire. Unité : mm
#define TIMEOUT_MOVE_INIT		1000		// Durée maximale le la phase "MOVE_INIT" d'une trajectoire. Unité : ms
#define MAX_PWM					1023		// Valeur de PWM correspondant à 100%
#define INFINITE_DISTANCE		INT32_MAX


class TrajectoryFollower
{
public:
	TrajectoryFollower(float freqAsserv, volatile Position & position, volatile MoveStatus & moveStatus) :
		directionController(DirectionController::Instance()),
		moveStatus(moveStatus),
		position(position),
		odometry(
			freqAsserv,
			position,
			currentLeftSpeed,
			currentRightSpeed,
			currentTranslation,
			currentMovingSpeed
		),
		leftSpeedPID(currentLeftSpeed, leftPWM, leftSpeedSetPoint),
		leftMotorBlockingMgr(leftSpeedSetPoint, currentLeftSpeed),
		rightSpeedPID(currentRightSpeed, rightPWM, rightSpeedSetPoint),
		rightMotorBlockingMgr(rightSpeedSetPoint, currentRightSpeed),
		translationPID(currentTranslation, movingSpeedSetPoint, translationSetPoint),
		endOfMoveMgr(currentMovingSpeed),
		curvaturePID(position, curvatureOrder, trajectoryPoint)
	{
		this->freqAsserv = freqAsserv;
		movePhase = MOVE_ENDED;
	}


	/*
		#################################################
		#  Méthodes à appeller durant une interruption  #
		#################################################
	*/

	void control()
	{
		odometry.compute(isMovingForward());
		manageStop();
		manageBlocking();
		checkPosition();

		if (movePhase == MOVE_INIT)
		{ // todo: add timeout
			if (trajectoryControlled)
			{
				directionController.setAimCurvature(trajectoryPoint.getCurvature());
				if (ABS(directionController.getRealCurvature() - trajectoryPoint.getCurvature()) < CURVATURE_TOLERANCE)
				{
					movePhase = MOVING;
					endOfMoveMgr.moveIsStarting();
				}
			}
			else
			{
				movePhase = MOVING;
				endOfMoveMgr.moveIsStarting();
			}
			leftSpeedSetPoint = 0;
			rightSpeedSetPoint = 0;
		}
		else if (movePhase == MOVING)
		{
			if (trajectoryControlled)
			{
				curvaturePID.compute(isMovingForward());	// MAJ curvatureOrder
			}
			directionController.setAimCurvature(curvatureOrder);
			updateSideDistanceFactors(curvatureOrder);

			if (translationControlled)
			{
				translationPID.compute();	// MAJ movingSpeedSetPoint
			}
			else
			{
				movingSpeedSetPoint = ABS(maxMovingSpeed);
			}

			// Limitation de l'accélération et de la décélération
			if ((movingSpeedSetPoint - previousMovingSpeedSetpoint) * freqAsserv > maxAcceleration)
			{
				movingSpeedSetPoint = previousMovingSpeedSetpoint + maxAcceleration / freqAsserv;
			}
			else if ((previousMovingSpeedSetpoint - movingSpeedSetPoint) * freqAsserv > maxDeceleration)
			{
				movingSpeedSetPoint = previousMovingSpeedSetpoint - maxDeceleration / freqAsserv;
			}

			previousMovingSpeedSetpoint = movingSpeedSetPoint;

			// Limitation de la vitesse
			if (movingSpeedSetPoint > ABS(maxMovingSpeed))
			{
				movingSpeedSetPoint = ABS(maxMovingSpeed);
			}
			else if (movingSpeedSetPoint < -ABS(maxMovingSpeed))
			{
				movingSpeedSetPoint = -ABS(maxMovingSpeed);
			}

			// Calcul des vitesses gauche et droite en fonction de la vitesse globale
			leftSpeedSetPoint = movingSpeedSetPoint * leftSideDistanceFactor;
			rightSpeedSetPoint = movingSpeedSetPoint * rightSideDistanceFactor;

			// Gestion du sens de déplacement
			if (!isMovingForward())
			{
				leftSpeedSetPoint = -leftSpeedSetPoint;
				rightSpeedSetPoint = -rightSpeedSetPoint;
			}
		}
		else
		{
			leftSpeedSetPoint = 0;
			rightSpeedSetPoint = 0;
		}

		if (speedControlled)
		{
			if (leftSpeedSetPoint == 0)
			{
				leftSpeedPID.resetIntegralError();
			}
			if (rightSpeedSetPoint == 0)
			{
				rightSpeedPID.resetIntegralError();
			}

			leftSpeedPID.compute();
			rightSpeedPID.compute();
		}

		if (pwmControlled)
		{
			if (movePhase == BREAKING)
			{
                motor.breakAll();
			}
			else
			{ 
				motor.runFrontLeft(leftPWM);
				motor.runFrontRight(rightPWM);
                // todo: les autres moteurs LOL !
			}
		}
	}

	void setTrajectoryPoint(TrajectoryPoint const & trajPoint)
	{
		if (trajectoryControlled)
		{
			trajectoryPoint = trajPoint;
			maxMovingSpeed = trajectoryPoint.getAlgebricMaxSpeed();
		}
		else
		{
			// todo: throw warning
		}
	}

	void setMaxSpeed(float maxSpeed)
	{
		if (!trajectoryControlled)
		{
			maxMovingSpeed = maxSpeed;
		}
		else
		{
			// todo: throw warning
		}
	}

	void setCurvature(float curvature)
	{
		if (!trajectoryControlled)
		{
			curvatureOrder = curvature;
		}
		else
		{
			// todo: throw warning
		}
	}

	void setDistanceToDrive(float distance)
	{
		if (translationControlled)
		{
			bool resetNeeded = translationSetPoint >= INFINITE_DISTANCE;
			translationSetPoint = currentTranslation + distance;
			if (resetNeeded)
			{
				translationPID.resetIntegralError();
				translationPID.resetDerivativeError();
			}
		}
		else
		{
			// todo: throw warning
		}
	}

	void setInfiniteDistanceToDrive()
	{
		if (translationControlled)
		{
			bool resetNeeded = translationSetPoint < INFINITE_DISTANCE;
			translationSetPoint = INFINITE_DISTANCE;
			if (resetNeeded)
			{
				translationPID.resetIntegralError();
				translationPID.resetDerivativeError();
			}
		}
		else
		{
			// todo: throw warning
		}
	}

	void startMove()
	{
		if (movePhase == MOVE_ENDED)
		{
			movePhase = MOVE_INIT;
		}
		else
		{
			// todo: throw error
		}
	}

	MovePhase getMovePhase()
	{
		return movePhase;
	}

    bool isMovingForward()
    {
        return maxMovingSpeed >= 0;
    }


	/*
		###################################################
		#  Méthodes à appeller dans la boucle principale  #
		###################################################
	*/

	void emergency_stop()
	{
		noInterrupts();
		if (movePhase != MOVE_ENDED)
		{
			movePhase = BREAKING;
			moveStatus |= EMERGENCY_BREAK;
		}
		else
		{
			// todo: throw error
		}
		interrupts();
	}

	void setMotionControlLevel(uint8_t level)
	{
		if (level > 4)
		{
			// todo: throw warning
			return;
		}
		noInterrupts();
		pwmControlled = level > 0;
		speedControlled = level > 1;
		translationControlled = level > 2;
		trajectoryControlled = level > 3;
		interrupts();
	}

	uint8_t getMotionControlLevel() const
	{
		uint8_t level = 0;
		noInterrupts();
		level += (uint8_t)pwmControlled;
		level += (uint8_t)speedControlled;
		level += (uint8_t)translationControlled;
		level += (uint8_t)trajectoryControlled;
		interrupts();
		return level;
	}

	void setTunings(MotionControlTunings const & tunings)
	{
		noInterrupts();
		// todo
		interrupts();
	}

	MotionControlTunings getTunings() const
	{
		static MotionControlTunings tunings;
		noInterrupts();
		// todo
		interrupts();
		return tunings;
	}


private:
	void finalise_stop()
	{
		currentTranslation = 0;
		translationSetPoint = 0;
		leftSpeedSetPoint = 0;
		rightSpeedSetPoint = 0;
		leftPWM = 0;
		rightPWM = 0;
		movingSpeedSetPoint = 0;
		previousMovingSpeedSetpoint = 0;
        motor.breakAll();
		translationPID.resetIntegralError();
		translationPID.resetDerivativeError();
		leftSpeedPID.resetIntegralError();
		leftSpeedPID.resetDerivativeError();
		rightSpeedPID.resetIntegralError();
		rightSpeedPID.resetDerivativeError();
	}

	void manageStop()
	{
		endOfMoveMgr.compute();
		if (endOfMoveMgr.isStopped())
		{
			if (movePhase == MOVING)
			{
				movePhase = MOVE_ENDED;
				if (!trajectoryPoint.isStopPoint())
				{
					moveStatus |= EXT_BLOCKED;
				}
				finalise_stop();
			}
			else if (movePhase == BREAKING)
			{
				movePhase = MOVE_ENDED;
				finalise_stop();
			}
		}
	}

	void manageBlocking()
	{
		leftMotorBlockingMgr.compute();
		rightMotorBlockingMgr.compute();
		if (leftMotorBlockingMgr.isBlocked() || rightMotorBlockingMgr.isBlocked())
		{
			movePhase = MOVE_ENDED;
			moveStatus |= INT_BLOCKED;
			finalise_stop();
		}
	}

	void checkPosition()
	{
		if (movePhase == MOVING)
		{
			if (position.distanceTo(trajectoryPoint.getPosition()) > DISTANCE_MAX_TO_TRAJ)
			{
				movePhase = BREAKING;
				moveStatus |= FAR_AWAY;
			}
		}
	}

	void updateSideDistanceFactors(float curvature)
	{
		static float squared_length = square(FRONT_BACK_WHEELS_DISTANCE);
		if (curvature == 0)
		{
			leftSideDistanceFactor = 1;
			rightSideDistanceFactor = 1;
		}
		else
		{
			float r = 1000 / curvature;
			if (r > 0)
			{
				leftSideDistanceFactor = (sqrtf(square(r - DIRECTION_ROTATION_POINT_Y) + squared_length) - DIRECTION_WHEEL_DIST_FROM_ROT_PT) / r;
				rightSideDistanceFactor = (sqrtf(square(r + DIRECTION_ROTATION_POINT_Y) + squared_length) + DIRECTION_WHEEL_DIST_FROM_ROT_PT) / r;
			}
			else
			{
				leftSideDistanceFactor = -(sqrtf(square(r - DIRECTION_ROTATION_POINT_Y) + squared_length) + DIRECTION_WHEEL_DIST_FROM_ROT_PT) / r;
				rightSideDistanceFactor = -(sqrtf(square(r + DIRECTION_ROTATION_POINT_Y) + squared_length) - DIRECTION_WHEEL_DIST_FROM_ROT_PT) / r;
			}
		}
	}


	float freqAsserv;	// Fréquence d'asservissement (Hz)

	DirectionController & directionController;
	Motor motor;
	volatile MoveStatus & moveStatus;
	volatile MovePhase movePhase;

	volatile Position & position;		// Position courante
	TrajectoryPoint trajectoryPoint;	// Point de trajectoire courant pour asservissement

	/* Calcul de la position et des vitesses */
	Odometry odometry;

	/* Asservissement en vitesse du moteur gauche */
	PID leftSpeedPID;
	BlockingMgr leftMotorBlockingMgr;
	volatile float leftSpeedSetPoint;	// consigne (mm/s)
	volatile float currentLeftSpeed;	// vitesse réelle (mm/s)
	volatile float leftPWM;				// sortie (pwm)

	/* Asservissement en vitesse du moteur droit */
	PID rightSpeedPID;
	BlockingMgr rightMotorBlockingMgr;
	volatile float rightSpeedSetPoint;	// consigne (mm/s)
	volatile float currentRightSpeed;	// vitesse réelle (mm/s)
	volatile float rightPWM;			// sortie (pwm)

	/* Asservissement en translation */
	PID translationPID;
	volatile float translationSetPoint;	// consigne (mm)
	volatile float currentTranslation;	// position réelle (mm)
	volatile float movingSpeedSetPoint;	// sortie (mm/s)
	StoppingMgr endOfMoveMgr;
	volatile float currentMovingSpeed;	// vitesse de translation réelle (mm/s)

	/* Asservissement sur trajectoire */
	CurvaturePID curvaturePID;
	volatile float curvatureOrder;		// sortie (m^-1)

	/* Variables d'activation des différents PID */
	bool trajectoryControlled;		// Asservissement sur trajectoire
	bool translationControlled;		// Asservissement en translation
	bool speedControlled;			// Asservissement en vitesse
	bool pwmControlled;				// Mise à jour des PWM grâce à l'asservissement en vitesse

	/* Facteurs multiplicatifs à appliquer à la distance parcourue par les roues gauche et droite, en fonction de la courbure courante. */
	float leftSideDistanceFactor;
	float rightSideDistanceFactor;

	/* Vitesse (algébrique) de translation maximale : une vitesse négative correspond à une marche arrière */
	float maxMovingSpeed;				// (mm/s)

	/* Pour le calcul de l'accélération */
	float previousMovingSpeedSetpoint;	// (mm/s)

	/* Accélérations maximale (variation maximale de movingSpeedSetpoint) */
	float maxAcceleration;	// (mm*s^-2)
	float maxDeceleration;	// (mm*s^-2)

};


#endif