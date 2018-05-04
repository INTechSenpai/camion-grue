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
#include "../CommunicationServer/CommunicationServer.h"


#define CURVATURE_TOLERANCE		0.3			// Ecart maximal entre la consigne en courbure et la courbure réelle admissible au démarrage. Unité : m^-1
#define DISTANCE_MAX_TO_TRAJ	40			// Distance (entre notre position et la trajectoire) au delà de laquelle on abandonne la trajectoire. Unité : mm
#define TIMEOUT_MOVE_INIT		1000		// Durée maximale le la phase "MOVE_INIT" d'une trajectoire. Unité : ms
#define MAX_PWM					1023		// Valeur de PWM correspondant à 100%
#define INFINITE_DISTANCE		INT32_MAX


enum MonitoredMotor
{
    NONE = 0,
    FRONT_LEFT = 1,
    FRONT_RIGHT = 2,
    BACK_LEFT = 3,
    BACK_RIGHT = 4
};


class TrajectoryFollower
{
public:
	TrajectoryFollower(const float freqAsserv, volatile Position & position, volatile MoveStatus & moveStatus) :
        freqAsserv(freqAsserv),
		directionController(DirectionController::Instance()),
		moveStatus(moveStatus),
		position(position),
		odometry(
			freqAsserv,
			position,
			currentFrontLeftSpeed,
			currentFrontRightSpeed,
            currentBackLeftSpeed,
            currentBackRightSpeed,
			currentTranslation,
			currentMovingSpeed
		),
		frontLeftSpeedPID(currentFrontLeftSpeed, frontLeftPWM, leftSpeedSetPoint, freqAsserv),
		backLeftSpeedPID(currentBackLeftSpeed, backLeftPWM, leftSpeedSetPoint, freqAsserv),
        frontLeftMotorBlockingMgr(leftSpeedSetPoint, currentFrontLeftSpeed),
        backLeftMotorBlockingMgr(leftSpeedSetPoint, currentBackLeftSpeed),
		frontRightSpeedPID(currentFrontRightSpeed, frontRightPWM, rightSpeedSetPoint, freqAsserv),
		backRightSpeedPID(currentBackRightSpeed, backRightPWM, rightSpeedSetPoint, freqAsserv),
		frontRightMotorBlockingMgr(rightSpeedSetPoint, currentFrontRightSpeed),
		backRightMotorBlockingMgr(rightSpeedSetPoint, currentBackRightSpeed),
		translationPID(currentTranslation, movingSpeedSetPoint, translationSetPoint, freqAsserv),
		endOfMoveMgr(currentMovingSpeed),
		curvaturePID(position, curvatureOrder, trajectoryPoint)
	{
		movePhase = MOVE_ENDED;
        finalise_stop();
        moveInitTimer = 0;
        currentFrontLeftSpeed = 0;
        currentFrontRightSpeed = 0;
        currentBackLeftSpeed = 0;
        currentBackRightSpeed = 0;
        leftSideDistanceFactor = 1;
        rightSideDistanceFactor = 1;
        setMotionControlLevel(4);
        curvatureOrder = 0;
        currentMovingSpeed = 0;
        updateTunings();
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
		{
            if (millis() - moveInitTimer > TIMEOUT_MOVE_INIT)
            {
                movePhase = MOVE_ENDED;
                moveStatus |= EXT_BLOCKED;
                Server.asynchronous_trace(__LINE__);
                finalise_stop();
            }
			else if (trajectoryControlled)
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

            if (ABS(movingSpeedSetPoint) < stoppedSpeed)
            {
                movingSpeedSetPoint = 0;
            }
            else if (ABS(movingSpeedSetPoint) < minAimSpeed)
            {
                if (movingSpeedSetPoint > 0) {
                    movingSpeedSetPoint = minAimSpeed;
                }
                else {
                    movingSpeedSetPoint = -minAimSpeed;
                }
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
			if (leftSpeedSetPoint == 0 && movePhase != BREAKING)
			{
				frontLeftSpeedPID.resetIntegralError();
				backLeftSpeedPID.resetIntegralError();
			}
			if (rightSpeedSetPoint == 0 && movePhase != BREAKING)
			{
				frontRightSpeedPID.resetIntegralError();
				backRightSpeedPID.resetIntegralError();
			}

			frontLeftSpeedPID.compute();
			frontRightSpeedPID.compute();
            backLeftSpeedPID.compute();
            backRightSpeedPID.compute();
		}

		if (pwmControlled)
		{
            motor.runFrontLeft((int16_t)frontLeftPWM);
            motor.runFrontRight((int16_t)frontRightPWM);
            motor.runBackLeft((int16_t)backLeftPWM);
            motor.runBackRight((int16_t)backRightPWM);
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
            Server.printf_err("TrajectoryFollower::setTrajectoryPoint : trajectory not controlled\n");
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
            Server.printf_err("TrajectoryFollower::setMaxSpeed : trajectory is controlled\n");
		}
	}

	void setCurvature(float curvature)
	{
		if (!trajectoryControlled || movePhase == MOVE_ENDED)
		{
			curvatureOrder = curvature;
		}
		else
		{
            Server.printf_err("TrajectoryFollower::setCurvature : trajectory is controlled and move is started\n");
		}
	}

    float getCurvature() const
    {
        return curvatureOrder;
    }

    void setPWM(int16_t pwm)
    {
        if (!speedControlled)
        {
            switch (monitoredMotor)
            {
            case NONE:
                break;
            case FRONT_LEFT:
                frontLeftPWM = (float)pwm;
                break;
            case FRONT_RIGHT:
                frontRightPWM = (float)pwm;
                break;
            case BACK_LEFT:
                backLeftPWM = (float)pwm;
                break;
            case BACK_RIGHT:
                backRightPWM = (float)pwm;
                break;
            default:
                break;
            }
        }
        else
        {
            Server.printf_err("TrajectoryFollower::setPWM : speed is controlled\n");
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
            Server.printf_err("TrajectoryFollower::setDistanceToDrive : translation not controlled\n");
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
            Server.printf_err("TrajectoryFollower::setInfiniteDistanceToDrive : translation not controlled\n");
		}
	}

	void startMove()
	{
		if (movePhase == MOVE_ENDED)
		{
			movePhase = MOVE_INIT;
            moveInitTimer = millis();
		}
		else
		{
            Server.printf_err("TrajectoryFollower::startMove : move already started\n");
		}
	}

	MovePhase getMovePhase() const
	{
		return movePhase;
	}

    bool isMovingForward() const
    {
        return maxMovingSpeed >= 0;
    }

    bool isBreaking() const
    {
        return movePhase == BREAKING || endOfMoveMgr.isBreaking();
    }

    void emergency_stop_from_interrupt()
    {
        if (movePhase != MOVE_ENDED)
        {
            if (translationControlled)
            {
                movePhase = BREAKING;
                moveStatus |= EMERGENCY_BREAK;
                Server.asynchronous_trace(__LINE__);
            }
            else
            {
                movePhase = MOVE_ENDED;
                finalise_stop();
            }
        }
        else
        {
            finalise_stop();
        }
    }


	/*
		###################################################
		#  Méthodes à appeller dans la boucle principale  #
		###################################################
	*/

	void emergency_stop()
	{
		noInterrupts();
        emergency_stop_from_interrupt();
		interrupts();
	}

	void setMotionControlLevel(uint8_t level)
	{
		if (level > 4)
		{
            Server.printf_err("TrajectoryFollower::setMotionControlLevel : level > 4\n");
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

    bool isTrajectoryControlled() const
    {
        bool ret;
        noInterrupts();
        ret = trajectoryControlled;
        interrupts();
        return ret;
    }

	void setTunings(MotionControlTunings const & tunings)
	{
        motionControlTunings = tunings;
        updateTunings();
	}

	MotionControlTunings getTunings() const
	{
		return motionControlTunings;
	}

    void setMonitoredMotor(MonitoredMotor m)
    {
        monitoredMotor = m;
    }

    void sendLogs()
    {
        static MoveStatus lastMoveStatus = MOVE_OK;
        noInterrupts();
        switch (monitoredMotor)
        {
        case NONE:
            break;
        case FRONT_LEFT:
            Server.print(PID_SPEED, frontLeftSpeedPID);
            Server.print(BLOCKING_MGR, frontLeftMotorBlockingMgr);
            break;
        case FRONT_RIGHT:
            Server.print(PID_SPEED, frontRightSpeedPID);
            Server.print(BLOCKING_MGR, frontRightMotorBlockingMgr);
            break;
        case BACK_LEFT:
            Server.print(PID_SPEED, backLeftSpeedPID);
            Server.print(BLOCKING_MGR, backLeftMotorBlockingMgr);
            break;
        case BACK_RIGHT:
            Server.print(PID_SPEED, backRightSpeedPID);
            Server.print(BLOCKING_MGR, backRightMotorBlockingMgr);
            break;
        default:
            break;
        }
        Server.print(PID_TRANS, translationPID);
        Server.print(PID_TRAJECTORY, curvaturePID);
        Server.print(STOPPING_MGR, endOfMoveMgr);
        if (moveStatus != lastMoveStatus)
        {
            if (moveStatus != MOVE_OK)
            {
                Server.printf_err("Move error: %u\n", (uint8_t)moveStatus);
            }
            else
            {
                Server.printf("Move status is OK\n");
            }
            lastMoveStatus = moveStatus;
        }
        interrupts();
    }

private:
	void finalise_stop()
	{
		currentTranslation = 0;
		translationSetPoint = 0;
		leftSpeedSetPoint = 0;
		rightSpeedSetPoint = 0;
		frontLeftPWM = 0;
		frontRightPWM = 0;
        backLeftPWM = 0;
        backRightPWM = 0;
		movingSpeedSetPoint = 0;
		previousMovingSpeedSetpoint = 0;
        maxMovingSpeed = 0;
        motor.breakAll();
		translationPID.resetIntegralError();
		translationPID.resetDerivativeError();
		frontLeftSpeedPID.resetIntegralError();
		frontLeftSpeedPID.resetDerivativeError();
		frontRightSpeedPID.resetIntegralError();
		frontRightSpeedPID.resetDerivativeError();
        backLeftSpeedPID.resetIntegralError();
        backLeftSpeedPID.resetDerivativeError();
        backRightSpeedPID.resetIntegralError();
        backRightSpeedPID.resetDerivativeError();
	}

	void manageStop()
	{
		endOfMoveMgr.compute();
        if (translationControlled)
        {
            if (endOfMoveMgr.isStopped())
            {
                if (movePhase == MOVING)
                {
                    movePhase = MOVE_ENDED;
                    if (trajectoryControlled && !trajectoryPoint.isStopPoint())
                    {
                        moveStatus |= EXT_BLOCKED;
                        Server.asynchronous_trace(__LINE__);
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
	}

	void manageBlocking()
	{
		frontLeftMotorBlockingMgr.compute();
		frontRightMotorBlockingMgr.compute();
        backLeftMotorBlockingMgr.compute();
        backRightMotorBlockingMgr.compute();
        if (movePhase == MOVING)
        {
            if ((frontLeftMotorBlockingMgr.isBlocked() ||
                frontRightMotorBlockingMgr.isBlocked()) ||
                (backLeftMotorBlockingMgr.isBlocked() ||
                    backRightMotorBlockingMgr.isBlocked()))
            {
                movePhase = MOVE_ENDED;
                moveStatus |= INT_BLOCKED;
                finalise_stop();
                Server.asynchronous_trace(__LINE__);
            }
        }
	}

	void checkPosition()
	{
		if (movePhase == MOVING && trajectoryControlled)
		{
			if (position.distanceTo(trajectoryPoint.getPosition()) > DISTANCE_MAX_TO_TRAJ)
			{
				movePhase = BREAKING;
				moveStatus |= FAR_AWAY;
                Server.asynchronous_trace(__LINE__);
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

    void updateTunings()
    {
        noInterrupts();
        maxAcceleration = motionControlTunings.maxAcceleration;
        maxDeceleration = motionControlTunings.maxDeceleration;
        minAimSpeed = motionControlTunings.minAimSpeed;
        stoppedSpeed = motionControlTunings.stoppedSpeed;

        frontLeftSpeedPID.setOutputLimits(-MAX_PWM, MAX_PWM);
        frontRightSpeedPID.setOutputLimits(-MAX_PWM, MAX_PWM);
        backLeftSpeedPID.setOutputLimits(-MAX_PWM, MAX_PWM);
        backRightSpeedPID.setOutputLimits(-MAX_PWM, MAX_PWM);

        frontLeftSpeedPID.setTunings(motionControlTunings.speedKp, motionControlTunings.speedKi, motionControlTunings.speedKd);
        frontRightSpeedPID.setTunings(motionControlTunings.speedKp, motionControlTunings.speedKi, motionControlTunings.speedKd);
        backLeftSpeedPID.setTunings(motionControlTunings.speedKp, motionControlTunings.speedKi, motionControlTunings.speedKd);
        backRightSpeedPID.setTunings(motionControlTunings.speedKp, motionControlTunings.speedKi, motionControlTunings.speedKd);

        frontLeftMotorBlockingMgr.setTunings(motionControlTunings.blockingSensibility, motionControlTunings.blockingResponseTime);
        frontRightMotorBlockingMgr.setTunings(motionControlTunings.blockingSensibility, motionControlTunings.blockingResponseTime);
        backLeftMotorBlockingMgr.setTunings(motionControlTunings.blockingSensibility, motionControlTunings.blockingResponseTime);
        backRightMotorBlockingMgr.setTunings(motionControlTunings.blockingSensibility, motionControlTunings.blockingResponseTime);

        translationPID.setTunings(motionControlTunings.translationKp, 0, motionControlTunings.translationKd);

        endOfMoveMgr.setTunings(motionControlTunings.stoppedSpeed, motionControlTunings.stoppingResponseTime);

        curvaturePID.setCurvatureLimits(-motionControlTunings.maxCurvature, motionControlTunings.maxCurvature);
        curvaturePID.setTunings(motionControlTunings.curvatureK1, motionControlTunings.curvatureK2);
        interrupts();
    }


	const float freqAsserv;     // Fréquence d'asservissement (Hz)

	DirectionController & directionController;
	Motor motor;
	volatile MoveStatus & moveStatus;
	volatile MovePhase movePhase;

	volatile Position & position;		// Position courante
	TrajectoryPoint trajectoryPoint;	// Point de trajectoire courant pour asservissement

	/* Calcul de la position et des vitesses */
	Odometry odometry;

	/* Asservissement en vitesse des moteurs gauche */
	PID frontLeftSpeedPID;
	PID backLeftSpeedPID;
	BlockingMgr frontLeftMotorBlockingMgr;
	BlockingMgr backLeftMotorBlockingMgr;
	volatile float leftSpeedSetPoint;	    // consigne (mm/s)
	volatile float currentFrontLeftSpeed;	// vitesse réelle (mm/s)
	volatile float currentBackLeftSpeed;	// vitesse réelle (mm/s)
	volatile float frontLeftPWM;            // sortie (pwm)
    volatile float backLeftPWM;             // sortie (pwm)

	/* Asservissement en vitesse des moteurs droit */
	PID frontRightSpeedPID;
	PID backRightSpeedPID;
	BlockingMgr frontRightMotorBlockingMgr;
	BlockingMgr backRightMotorBlockingMgr;
	volatile float rightSpeedSetPoint;      // consigne (mm/s)
	volatile float currentFrontRightSpeed;	// vitesse réelle (mm/s)
	volatile float currentBackRightSpeed;	// vitesse réelle (mm/s)
	volatile float frontRightPWM;			// sortie (pwm)
	volatile float backRightPWM;			// sortie (pwm)

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

    /* Accélérations maximale (variation maximale de movingSpeedSetpoint) */
    float maxAcceleration;              // (mm*s^-2)
    float maxDeceleration;              // (mm*s^-2)

    /* Vitesse non nulle minimale pouvant être donnée en tant que consigne */
    float minAimSpeed;                  // (mm*s^-1)

    /* En dessous de cette vitesse, on considère être à l'arrêt */
    float stoppedSpeed;                 // (mm*s^-1)

	/* Pour le calcul de l'accélération */
	float previousMovingSpeedSetpoint;	// (mm/s)

    /* Pour le timeout de la phase MOVE_INIT */
    uint32_t moveInitTimer;

    /* Pour le réglage des paramètres des PID, BlockingMgr et StoppingMgr */
    MotionControlTunings motionControlTunings;

    /* Pour sélectionner le moteur courant à régler et surveiller */
    MonitoredMotor monitoredMotor;

};


#endif