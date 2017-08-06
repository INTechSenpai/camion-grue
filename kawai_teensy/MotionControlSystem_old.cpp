#include "MotionControlSystem.h"


MotionControlSystem::MotionControlSystem() :
leftMotorEncoder(PIN_A_LEFT_MOTOR_ENCODER, PIN_B_LEFT_MOTOR_ENCODER),
rightMotorEncoder(PIN_A_RIGHT_MOTOR_ENCODER, PIN_B_RIGHT_MOTOR_ENCODER),
leftFreeEncoder(PIN_A_LEFT_BACK_ENCODER, PIN_B_LEFT_BACK_ENCODER),
rightFreeEncoder(PIN_A_RIGHT_BACK_ENCODER, PIN_B_RIGHT_BACK_ENCODER),
direction(DirectionController::Instance()),
rightSpeedPID(&currentRightSpeed, &rightPWM, &rightSpeedSetpoint),
rightMotorBlockingMgr(rightSpeedSetpoint, currentRightSpeed),
leftSpeedPID(&currentLeftSpeed, &leftPWM, &leftSpeedSetpoint),
leftMotorBlockingMgr(leftSpeedSetpoint, currentLeftSpeed),
forwardTranslationPID(&currentTranslation, &movingSpeedSetpoint, &translationSetpoint),
backwardTranslationPID(&currentTranslation, &movingSpeedSetpoint, &translationSetpoint),
endOfMoveMgr(currentMovingSpeed)
{
	currentTranslation = 0;
	currentLeftSpeed = 0;
	currentRightSpeed = 0;

	maxMovingSpeed = 0;

	movingState = STOPPED;
	trajectoryFullyCompleted = true;
	matchTerminated = false;
	trajectoryIndex = 0;
	updateNextStopPoint();
	updateSideDistanceFactors();

	leftSpeedPID.setOutputLimits(-1023, 1023);
	rightSpeedPID.setOutputLimits(-1023, 1023);

	loadParameters();

	resetPosition();
	stop();

	lastInterruptDuration = 0;
	maxInterruptDuration = 0;
}

void MotionControlSystem::enablePositionControl(bool enabled)
{
	positionControlled = enabled;
}

void MotionControlSystem::enableLeftSpeedControl(bool enable)
{
	leftSpeedControlled = enable;
}

void MotionControlSystem::enableRightSpeedControl(bool enable)
{
	rightSpeedControlled = enable;
}

void MotionControlSystem::enablePwmControl(bool enable)
{
	pwmControlled = enable;
}

void MotionControlSystem::getEnableStates(bool &cp, bool &cvg, bool &cvd, bool &cpwm)
{
	cp = positionControlled;
	cvg = leftSpeedControlled;
	cvd = rightSpeedControlled;
	cpwm = pwmControlled;
}



/*
	Boucle d'asservissement
*/

void MotionControlSystem::control()
{
	static uint32_t beginTimestamp;
	beginTimestamp = micros();

	updateSpeedAndPosition();
	manageStop();
	manageBlocking();
	checkTrajectory();
	updateTrajectoryIndex();
	checkPosition();

	if (positionControlled)
	{
		/* Gestion du timeout de MOVE_INIT */
		static uint32_t moveInit_startTime = 0;
		static bool moveInit_started = false;
		if (movingState == MOVE_INIT)
		{
			if (!moveInit_started)
			{
				moveInit_started = true;
				moveInit_startTime = millis();
			}
			if (millis() - moveInit_startTime > TIMEOUT_MOVE_INIT)
			{
				movingState = EXT_BLOCKED;
				clearCurrentTrajectory();
				stop();
				Log::critical(34, "MOVE_INIT TIMEOUT");
			}
		}
		else
		{
			moveInit_started = false;
		}

		/* Asservissement */
		if (movingState == MOVE_INIT)
		{
			TrajectoryPoint currentTrajPoint = currentTrajectory[trajectoryIndex];
			direction.setAimCurvature(currentTrajPoint.getCurvature());

			if (ABS(direction.getRealCurvature() - currentTrajPoint.getCurvature()) < CURVATURE_TOLERANCE)
			{
				movingState = MOVING;
				endOfMoveMgr.moveIsStarting();
			}
			leftSpeedSetpoint = 0;
			rightSpeedSetpoint = 0;
		}
		else if (movingState == MOVING)
		{
			/* Asservissement sur trajectoire */

			Position posConsigne = currentTrajectory[trajectoryIndex].getPosition();
			float trajectoryCurvature = currentTrajectory[trajectoryIndex].getCurvature();
			float posError = -(position.x - posConsigne.x) * sinf(posConsigne.orientation) + (position.y - posConsigne.y) * cosf(posConsigne.orientation);
			float orientationError = fmodulo(position.orientation - posConsigne.orientation, TWO_PI);

			if (orientationError > PI)
			{
				orientationError -= TWO_PI;
			}

			if (maxMovingSpeed > 0)
			{
				curvatureOrder = trajectoryCurvature - curvatureCorrectorK1 * posError - curvatureCorrectorK2 * orientationError;
			}
			else
			{
				curvatureOrder = trajectoryCurvature - curvatureCorrectorK1 * posError + curvatureCorrectorK2 * orientationError;
			}

			direction.setAimCurvature(curvatureOrder);
			
			watchTrajErrors.traj_curv = trajectoryCurvature;
			watchTrajErrors.current_curv = direction.getRealCurvature();
			watchTrajErrors.aim_curv = curvatureOrder;
			watchTrajErrors.angle_err = 100*orientationError;
			watchTrajErrors.pos_err = posError;

			watchTrajIndex.index = trajectoryIndex;
			watchTrajIndex.currentPos = position;
			watchTrajIndex.aimPosition = posConsigne;

			if (maxMovingSpeed > 0)
			{
				forwardTranslationPID.compute(); // MAJ movingSpeedSetpoint
			}
			else
			{
				backwardTranslationPID.compute(); // MAJ movingSpeedSetpoint
			}

			// Limitation de l'accélération et de la décélération
			if (movingSpeedSetpoint - previousMovingSpeedSetpoint > maxAcceleration)
			{
				movingSpeedSetpoint = previousMovingSpeedSetpoint + maxAcceleration;
			}
			else if (previousMovingSpeedSetpoint - movingSpeedSetpoint > maxDeceleration)
			{
				movingSpeedSetpoint = previousMovingSpeedSetpoint - maxDeceleration;
			}

			previousMovingSpeedSetpoint = movingSpeedSetpoint;

			// Limitation de la vitesse
			if (movingSpeedSetpoint > ABS(maxMovingSpeed))
			{
				movingSpeedSetpoint = ABS(maxMovingSpeed);
			}

			// Calcul des vitesses gauche et droite en fonction de la vitesse globale
			leftSpeedSetpoint = movingSpeedSetpoint * leftSideDistanceFactor;
			rightSpeedSetpoint = movingSpeedSetpoint * rightSideDistanceFactor;

			// Gestion du sens de déplacement
			if (maxMovingSpeed < 0)
			{
				leftSpeedSetpoint = -leftSpeedSetpoint;
				rightSpeedSetpoint = -rightSpeedSetpoint;
			}
		}
		else
		{
			leftSpeedSetpoint = 0;
			rightSpeedSetpoint = 0;
		}
	}

	if (leftSpeedControlled)
	{
		if (leftSpeedSetpoint == 0)
		{
			leftSpeedPID.resetIntegralError();
		}
		leftSpeedPID.compute();		// Actualise la valeur de 'leftPWM'
	}
	if (rightSpeedControlled)
	{
		if (rightSpeedSetpoint == 0)
		{
			rightSpeedPID.resetIntegralError();
		}
		rightSpeedPID.compute();	// Actualise la valeur de 'rightPWM'
	}

	if (pwmControlled)
	{
		motor.runLeft(leftPWM);
		motor.runRight(rightPWM);
	}

	//  Mesure du temps passé dans l'interruption
	lastInterruptDuration = micros() - beginTimestamp;
	if (lastInterruptDuration > maxInterruptDuration)
	{
		maxInterruptDuration = lastInterruptDuration;
	}
}

void MotionControlSystem::updateSpeedAndPosition() 
{
	static int32_t
		leftMotorTicks = 0,
		rightMotorTicks = 0,
		leftTicks = 0,
		rightTicks = 0;
	static int32_t
		previousLeftMotorTicks = 0,
		previousRightMotorTicks = 0,
		previousLeftTicks = 0,
		previousRightTicks = 0;
	static int32_t
		deltaLeftMotorTicks = 0,
		deltaRightMotorTicks = 0,
		deltaLeftTicks = 0,
		deltaRightTicks = 0;
	static float
		deltaTranslation_mm = 0,
		half_deltaRotation_rad = 0,
		currentAngle = 0,
		corrector = 1,
		deltaTranslation = 0;

	// Récupération des données des encodeurs
	leftMotorTicks = leftMotorEncoder.read();
	rightMotorTicks = rightMotorEncoder.read();
	leftTicks = leftFreeEncoder.read();
	rightTicks = rightFreeEncoder.read();

	// Calcul du mouvement de chaque roue depuis le dernier asservissement
	deltaLeftMotorTicks = leftMotorTicks - previousLeftMotorTicks;
	deltaRightMotorTicks = rightMotorTicks - previousRightMotorTicks;
	deltaLeftTicks = leftTicks - previousLeftTicks;
	deltaRightTicks = rightTicks - previousRightTicks;

	previousLeftMotorTicks = leftMotorTicks;
	previousRightMotorTicks = rightMotorTicks;
	previousLeftTicks = leftTicks;
	previousRightTicks = rightTicks;

	// Mise à jour de la vitesse des moteurs
	currentLeftSpeed = deltaLeftMotorTicks * FREQ_ASSERV;
	currentRightSpeed = deltaRightMotorTicks * FREQ_ASSERV;
	averageLeftSpeed.add(currentLeftSpeed);
	averageRightSpeed.add(currentRightSpeed);
	currentLeftSpeed = averageLeftSpeed.value();
	currentRightSpeed = averageRightSpeed.value();

	// Mise à jour de la position et de l'orientattion
	deltaTranslation = ((float)deltaLeftTicks + (float)deltaRightTicks) / 2;
	deltaTranslation_mm = deltaTranslation * TICK_TO_MM;
	half_deltaRotation_rad = (((float)deltaRightTicks - (float)deltaLeftTicks) / 4) * TICK_TO_RADIANS;
	currentAngle = position.orientation + half_deltaRotation_rad;
	position.setOrientation(position.orientation + half_deltaRotation_rad * 2);
	corrector = 1 - square(half_deltaRotation_rad) / 6;
	position.x += corrector * deltaTranslation_mm * cosf(currentAngle);
	position.y += corrector * deltaTranslation_mm * sinf(currentAngle);

	// Mise à jour de currentTranslation
	if (maxMovingSpeed >= 0)
	{
		currentTranslation_float += deltaTranslation;
	}
	else
	{
		currentTranslation_float -= deltaTranslation;
	}
	currentTranslation = (int32_t)(currentTranslation_float + 0.5);


	// Mise à jour de la vitesse de translation
	currentMovingSpeed = deltaTranslation * FREQ_ASSERV;
	averageTranslationSpeed.add(currentMovingSpeed);
	currentMovingSpeed = averageTranslationSpeed.value();

	// Mise à jour des erreurs cumulatives des encodeurs des moteurs
	leftMotorError += deltaLeftMotorTicks * FRONT_TICK_TO_TICK - (int32_t)(deltaTranslation * leftSideDistanceFactor);
	rightMotorError += deltaRightMotorTicks * FRONT_TICK_TO_TICK - (int32_t)(deltaTranslation * rightSideDistanceFactor);

	// En cas d'erreur excessive au niveau des moteurs de propulsion, le robot est considéré bloqué.
	if ((ABS(leftMotorError) > MOTOR_SLIP_TOLERANCE || ABS(rightMotorError) > MOTOR_SLIP_TOLERANCE) && false)
	{
		movingState = EXT_BLOCKED;
		clearCurrentTrajectory();
		stop();
		Log::critical(34, "Derapage d'un moteur de propulsion");
	}
}

void MotionControlSystem::updateTrajectoryIndex()
{
	if (movingState == MOVING && !currentTrajectory[trajectoryIndex].isStopPoint())
	{
		uint8_t nextPoint = trajectoryIndex + 1;
		
		Position trajPoint = currentTrajectory[trajectoryIndex].getPosition();
		if 
			(
			currentTrajectory[nextPoint].isUpToDate() &&
			((position.x - trajPoint.x) * cosf(trajPoint.orientation) + (position.y - trajPoint.y) * sinf(trajPoint.orientation)) * (float)maxMovingSpeed > 0
			)
		{
			currentTrajectory[trajectoryIndex].makeObsolete();
			trajectoryIndex = nextPoint;
			updateTranslationSetpoint();
			updateSideDistanceFactors();
		}
	}
	else if (movingState == STOPPED && currentTrajectory[trajectoryIndex].isStopPoint())
	{
		if (!trajectoryFullyCompleted)
		{
			currentTrajectory[trajectoryIndex].makeObsolete();
			trajectoryIndex++;
			updateNextStopPoint();
			updateSideDistanceFactors();
			trajectoryFullyCompleted = true;
		}
	}
}

void MotionControlSystem::updateNextStopPoint()
{
	if (currentTrajectory[trajectoryIndex].isUpToDate() && currentTrajectory[trajectoryIndex].isStopPoint())
	{
		nextStopPoint = trajectoryIndex;
	}
	else
	{
		uint16_t infiniteLoopCheck = 0;
		bool found = false;
		nextStopPoint = trajectoryIndex + 1;
		while (currentTrajectory[nextStopPoint].isUpToDate() && infiniteLoopCheck < UINT8_MAX + 1)
		{
			if (currentTrajectory[nextStopPoint].isStopPoint())
			{
				found = true;
				break;
			}
			nextStopPoint++;
			infiniteLoopCheck++;
		}
		if (!found)
		{
			nextStopPoint = UINT16_MAX;
		}
	}
	updateTranslationSetpoint();
}

void MotionControlSystem::checkTrajectory()
{
	if (movingState == MOVE_INIT || movingState == MOVING)
	{
		bool valid = true;
		if (!currentTrajectory[trajectoryIndex].isUpToDate())
		{
			valid = false;
		}
		else if (movingState == MOVING && !currentTrajectory[trajectoryIndex].isStopPoint())
		{
			if (!currentTrajectory[(uint8_t)(trajectoryIndex + 1)].isUpToDate())
			{
				valid = false;
			}
		}
		if (!valid)
		{
			movingState = EMPTY_TRAJ;
			clearCurrentTrajectory();
			stop();
			Log::critical(32, "Empty trajectory");
		}
	}
}

void MotionControlSystem::checkPosition()
{
	if (movingState == MOVING)
	{
		Position trajPosition = currentTrajectory[trajectoryIndex].getPosition();
		if (position.distanceTo(trajPosition) > DISTANCE_MAX_TO_TRAJ)
		{
			movingState = FAR_AWAY;
			clearCurrentTrajectory();
			stop();
			Log::critical(35, "Far away from trajectory");
		}
	}
}

void MotionControlSystem::updateTranslationSetpoint()
{
	static bool undefinedStopPoint = true;
	if (nextStopPoint == UINT16_MAX)
	{
		translationSetpoint = currentTranslation + UINT8_MAX * TRAJECTORY_STEP;
		if (!undefinedStopPoint)
		{
			forwardTranslationPID.resetDerivativeError();
			forwardTranslationPID.resetIntegralError();
			backwardTranslationPID.resetDerivativeError();
			backwardTranslationPID.resetIntegralError();
			undefinedStopPoint = true;
		}
	}
	else
	{
		uint8_t nbPointsToTravel = nextStopPoint - trajectoryIndex;
		translationSetpoint = currentTranslation + nbPointsToTravel * TRAJECTORY_STEP;
		if (nbPointsToTravel > 10)
		{
			translationSetpoint += TRAJECTORY_STEP / 2;
		}
		else
		{
			Position posConsigne = currentTrajectory[trajectoryIndex].getPosition();
			float offset = 
				ABS(
					(
						(position.x - posConsigne.x) * cosf(posConsigne.orientation) + 
						(position.y - posConsigne.y) * sinf(posConsigne.orientation)
					) / TICK_TO_MM
				);
			if (offset < TRAJECTORY_STEP * 2)
			{
				translationSetpoint += (int32_t)offset;
			}
			else
			{
				translationSetpoint += TRAJECTORY_STEP / 2;
				Serial.print("Warning pos: ");
				Position p;
				getPosition(p);
				Serial.print(p);
				Serial.print("  index: ");
				Serial.println(getTrajectoryIndex());
				Log::warning("Position courante tres loin du point de trajectoire courant");
			}
		}

		if (undefinedStopPoint)
		{
			forwardTranslationPID.resetDerivativeError();
			forwardTranslationPID.resetIntegralError();
			backwardTranslationPID.resetDerivativeError();
			backwardTranslationPID.resetIntegralError();
			undefinedStopPoint = false;
		}
	}
}


void MotionControlSystem::updateSideDistanceFactors()
{
	static float squared_length = square(FRONT_BACK_WHEELS_DISTANCE);

	if (curvatureOrder == 0 || !currentTrajectory[trajectoryIndex].isUpToDate())
	{
		leftSideDistanceFactor = 1;
		rightSideDistanceFactor = 1;
	}
	else
	{
		float r = 1000 / curvatureOrder;
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


void MotionControlSystem::manageStop()
{
	endOfMoveMgr.compute();
	if (endOfMoveMgr.isStopped() && (movingState == MOVING || movingState == MANUAL_MOVE))
	{
		if (currentTrajectory[trajectoryIndex].isUpToDate())
		{
			if (currentTrajectory[trajectoryIndex].isStopPoint())
			{
				movingState = STOPPED;
				updateTrajectoryIndex();
			}
			else
			{
				movingState = EXT_BLOCKED;
				clearCurrentTrajectory();
				Log::critical(33, "Erreur d'asservissement en translation");
			}
		}
		else
		{
			movingState = STOPPED;
		}
		stop();
	}
}

void MotionControlSystem::manageBlocking()
{
	leftMotorBlockingMgr.compute();
	rightMotorBlockingMgr.compute();
	if (leftMotorBlockingMgr.isBlocked() || rightMotorBlockingMgr.isBlocked())
	{
		movingState = INT_BLOCKED;
		clearCurrentTrajectory();
		stop();
		Log::critical(31, "Blocage physique d'un moteur");
	}
}

void MotionControlSystem::clearCurrentTrajectory()
{
	trajectoryFullyCompleted = true;
	TrajectoryPoint voidPoint;
	for (uint16_t i = 0; i < UINT8_MAX + 1; i++)
	{
		currentTrajectory[i] = voidPoint;
	}
}



/*
	Gestion des déplacements
*/

void MotionControlSystem::addTrajectoryPoint(const TrajectoryPoint & trajPoint, uint8_t index)
{
	bool updateIsNeeded = false;
	if (trajPoint.isStopPoint() || (currentTrajectory[index].isUpToDate() && currentTrajectory[index].isStopPoint()))
	{
		updateIsNeeded = true;
	}

	noInterrupts(); // au cas où index == trajectoryIndex
	currentTrajectory[index] = trajPoint;
	interrupts();
	currentTrajectory[(uint8_t)(index + 1)].makeObsolete();
	if (updateIsNeeded)
	{
		noInterrupts();
		updateNextStopPoint();
		interrupts();
	}

	Log::data(Log::TRAJECTORY, trajPoint);
}

MotionControlSystem::MovingState MotionControlSystem::getMovingState() const
{
	noInterrupts();
	MovingState movingStateCpy = movingState;
	interrupts();
	return movingStateCpy;
}

void MotionControlSystem::gotoNextStopPoint()
{
	if (matchTerminated)
	{
		Log::warning("Move request after match end");
	}
	else
	{
		noInterrupts();
		if (movingState == MOVING || movingState == MOVE_INIT)
		{
			Log::warning("Nested call of MotionControlSystem::gotoNextStopPoint()");
			return;
		}
		updateNextStopPoint();
		movingState = MOVE_INIT;
		trajectoryFullyCompleted = false;

		interrupts();
	}
}

void MotionControlSystem::moveUranus(bool launch)
{
	if (launch)
	{
		movingState = MANUAL_MOVE;
	}
	leftSpeedSetpoint = maxMovingSpeed;
	rightSpeedSetpoint = maxMovingSpeed;
}

void MotionControlSystem::stop() 
{
	noInterrupts();
	currentTranslation = 0;
	currentTranslation_float = 0;
	translationSetpoint = 0;
	leftSpeedSetpoint = 0;
	rightSpeedSetpoint = 0;
	leftPWM = 0;
	rightPWM = 0;
	movingSpeedSetpoint = 0;
	previousMovingSpeedSetpoint = 0;
	motor.runLeft(0);
	motor.runRight(0);
	forwardTranslationPID.resetIntegralError();
	forwardTranslationPID.resetDerivativeError();
	backwardTranslationPID.resetIntegralError();
	backwardTranslationPID.resetDerivativeError();
	leftSpeedPID.resetIntegralError();
	leftSpeedPID.resetDerivativeError();
	rightSpeedPID.resetIntegralError();
	rightSpeedPID.resetDerivativeError();
	leftMotorError = 0;
	rightMotorError = 0;
	interrupts();
}

void MotionControlSystem::highLevelStop()
{
	noInterrupts();
	movingState = HIGHLEVEL_STOP;
	clearCurrentTrajectory();
	interrupts();
	stop();
}

void MotionControlSystem::endMatchStop()
{
	noInterrupts();
	movingState = STOPPED;
	clearCurrentTrajectory();
	interrupts();
	stop();
	matchTerminated = true;
}

bool MotionControlSystem::isStopped() const
{
	noInterrupts();
	bool stopped = endOfMoveMgr.isStopped();
	interrupts();
	return stopped;
}

bool MotionControlSystem::isBreaking() const
{
	noInterrupts();
	bool breaking = endOfMoveMgr.isBreaking();
	interrupts();
	return breaking;
}

void MotionControlSystem::setMaxMovingSpeed(int32_t maxMovingSpeed_mm_sec)
{
	int32_t speed_ticks_sec = (maxMovingSpeed_mm_sec / TICK_TO_MM ) / FRONT_TICK_TO_TICK;
	noInterrupts();
	maxMovingSpeed = speed_ticks_sec;
	interrupts();
}

int32_t MotionControlSystem::getMaxMovingSpeed() const
{
	return maxMovingSpeed * TICK_TO_MM * FRONT_TICK_TO_TICK;
}

void MotionControlSystem::setMaxAcceleration(int32_t newMaxAcceleration)
{
	noInterrupts();
	maxAcceleration = newMaxAcceleration;
	interrupts();
}

int32_t MotionControlSystem::getMaxAcceleration() const
{
	return maxAcceleration;
}

void MotionControlSystem::setMaxDeceleration(int32_t newMaxDeceleration)
{
	noInterrupts();
	maxDeceleration = newMaxDeceleration;
	interrupts();
}

int32_t MotionControlSystem::getMaxDeceleration() const
{
	return maxDeceleration;
}



/**
* Getters/Setters des constantes d'asservissement en translation/rotation/vitesse
*/

void MotionControlSystem::setCurrentPIDTunings(float kp, float ki, float kd)
{
	switch (pidToSet)
	{
	case MotionControlSystem::LEFT_SPEED:
		setLeftSpeedTunings(kp, ki, kd);
		break;
	case MotionControlSystem::RIGHT_SPEED:
		setRightSpeedTunings(kp, ki, kd);
		break;
	case MotionControlSystem::SPEED:
		setLeftSpeedTunings(kp, ki, kd);
		setRightSpeedTunings(kp, ki, kd);
		break;
	case MotionControlSystem::TRANSLATION:
		setForwardTranslationTunings(kp, ki, kd);
		break;
	case MotionControlSystem::REVERSE_TRANSLATION:
		setBackwardTranslationTunings(kp, ki, kd);
		break;
	default:
		break;
	}
}
void MotionControlSystem::getCurrentPIDTunings(float &kp, float &ki, float &kd) const
{
	switch (pidToSet)
	{
	case MotionControlSystem::LEFT_SPEED:
		getLeftSpeedTunings(kp, ki, kd);
		break;
	case MotionControlSystem::RIGHT_SPEED:
		getRightSpeedTunings(kp, ki, kd);
		break;
	case MotionControlSystem::SPEED:
		float kpl, kil, kdl, kpr, kir, kdr;
		getLeftSpeedTunings(kpl, kil, kdl);
		getRightSpeedTunings(kpr, kir, kdr);
		if ((kpl != kpr || kil != kir) || kdl != kdr)
		{
			Log::warning("Left/Right speed PID tunings are different, left tunings are returned");
		}
		kp = kpl;
		ki = kil;
		kd = kdl;
		break;
	case MotionControlSystem::TRANSLATION:
		getForwardTranslationTunings(kp, ki, kd);
		break;
	case MotionControlSystem::REVERSE_TRANSLATION:
		getBackwardTranslationTunings(kp, ki, kd);
		break;
	default:
		break;
	}
}
void MotionControlSystem::getForwardTranslationTunings(float &kp, float &ki, float &kd) const {
	kp = forwardTranslationPID.getKp();
	ki = forwardTranslationPID.getKi();
	kd = forwardTranslationPID.getKd();
}
void MotionControlSystem::getBackwardTranslationTunings(float &kp, float &ki, float &kd) const {
	kp = backwardTranslationPID.getKp();
	ki = backwardTranslationPID.getKi();
	kd = backwardTranslationPID.getKd();
}
void MotionControlSystem::getLeftSpeedTunings(float &kp, float &ki, float &kd) const {
	kp = leftSpeedPID.getKp();
	ki = leftSpeedPID.getKi();
	kd = leftSpeedPID.getKd();
}
void MotionControlSystem::getRightSpeedTunings(float &kp, float &ki, float &kd) const {
	kp = rightSpeedPID.getKp();
	ki = rightSpeedPID.getKi();
	kd = rightSpeedPID.getKd();
}
void MotionControlSystem::getTrajectoryTunings(float &k1, float &k2) const {
	k1 = curvatureCorrectorK1;
	k2 = curvatureCorrectorK2;
}
void MotionControlSystem::setForwardTranslationTunings(float kp, float ki, float kd) {
	forwardTranslationPID.setTunings(kp, ki, kd);
}
void MotionControlSystem::setBackwardTranslationTunings(float kp, float ki, float kd) {
	backwardTranslationPID.setTunings(kp, ki, kd);
}
void MotionControlSystem::setLeftSpeedTunings(float kp, float ki, float kd) {
	leftSpeedPID.setTunings(kp, ki, kd);
}
void MotionControlSystem::setRightSpeedTunings(float kp, float ki, float kd) {
	rightSpeedPID.setTunings(kp, ki, kd);
}
void MotionControlSystem::setTrajectoryTunings(float k1, float k2) {
	curvatureCorrectorK1 = k1;
	curvatureCorrectorK2 = k2;
}
void MotionControlSystem::setPIDtoSet(PIDtoSet newPIDtoSet)
{
	pidToSet = newPIDtoSet;
}
MotionControlSystem::PIDtoSet MotionControlSystem::getPIDtoSet() const
{
	return pidToSet;
}
void MotionControlSystem::getPIDtoSet_str(char * str, size_t size) const
{
	char leftSpeedStr[] = "LEFT_SPEED";
	char rightSpeedStr[] = "RIGHT_SPEED";
	char speedStr[] = "SPEED";
	char translationStr[] = "TRANSLATION";
	char reverseTranslationStr[] = "R_TRANS";

	if (size == 0)
	{
		return;
	}
	else if (size < 12)
	{
		str[0] = '\0';
	}
	else
	{
		switch (pidToSet)
		{
		case MotionControlSystem::LEFT_SPEED:
			strcpy(str, leftSpeedStr);
			break;
		case MotionControlSystem::RIGHT_SPEED:
			strcpy(str, rightSpeedStr);
			break;
		case MotionControlSystem::SPEED:
			strcpy(str, speedStr);
			break;
		case MotionControlSystem::TRANSLATION:
			strcpy(str, translationStr);
			break;
		case MotionControlSystem::REVERSE_TRANSLATION:
			strcpy(str, reverseTranslationStr);
			break;
		default:
			str[0] = '\0';
			break;
		}
	}
}


/*
* Getters/Setters des variables de position haut niveau
*/
void MotionControlSystem::setPosition(const Position & newPosition)
{
	noInterrupts();
	position = newPosition;
	interrupts();
}

void MotionControlSystem::getPosition(Position & returnPos) const
{
	noInterrupts();
	returnPos = position;
	interrupts();
}

uint8_t MotionControlSystem::getTrajectoryIndex() const
{
	noInterrupts();
	uint8_t indexCpy = trajectoryIndex;
	interrupts();
	return indexCpy;
}

void MotionControlSystem::resetPosition()
{
	noInterrupts();
	position.x = 0;
	position.y = 0;
	position.orientation = 0;
	interrupts();
	stop();
}


/*
*	Réglage des blockingMgr et stoppingMgr
*/

void MotionControlSystem::setLeftMotorBmgrTunings(float sensibility, uint32_t responseTime)
{
	noInterrupts();
	leftMotorBlockingMgr.setTunings(sensibility, responseTime);
	interrupts();
}

void MotionControlSystem::setRightMotorBmgrTunings(float sensibility, uint32_t responseTime)
{
	noInterrupts();
	rightMotorBlockingMgr.setTunings(sensibility, responseTime);
	interrupts();
}

void MotionControlSystem::setEndOfMoveMgrTunings(uint32_t epsilon, uint32_t responseTime)
{
	noInterrupts();
	endOfMoveMgr.setTunings(epsilon, responseTime);
	interrupts();
}

void MotionControlSystem::getLeftMotorBmgrTunings(float & sensibility, uint32_t & responseTime) const
{
	leftMotorBlockingMgr.getTunings(sensibility, responseTime);
}

void MotionControlSystem::getRightMotorBmgrTunings(float & sensibility, uint32_t & responseTime) const
{
	rightMotorBlockingMgr.getTunings(sensibility, responseTime);
}

void MotionControlSystem::getEndOfMoveMgrTunings(uint32_t & epsilon, uint32_t & responseTime) const
{
	endOfMoveMgr.getTunings(epsilon, responseTime);
}


/*
*	Getters/Setters de débug
*/

void MotionControlSystem::getTicks(int32_t & leftFront, int32_t & rightFront, int32_t & leftBack, int32_t & rightBack)
{
	leftFront = leftMotorEncoder.read();
	rightFront = rightMotorEncoder.read();
	leftBack = leftFreeEncoder.read();
	rightBack = rightFreeEncoder.read();
}

void MotionControlSystem::saveParameters()
{
	int a = 0; // Adresse mémoire dans l'EEPROM
	float kp, ki, kd, s;
	uint32_t e, t;

	EEPROM.put(a, positionControlled);
	a += sizeof(positionControlled);
	EEPROM.put(a, leftSpeedControlled);
	a += sizeof(leftSpeedControlled);
	EEPROM.put(a, rightSpeedControlled);
	a += sizeof(rightSpeedControlled);
	EEPROM.put(a, pwmControlled);
	a += sizeof(pwmControlled);

	leftMotorBlockingMgr.getTunings(s, t);
	EEPROM.put(a, s);
	a += sizeof(s);
	EEPROM.put(a, t);
	a += sizeof(t);

	rightMotorBlockingMgr.getTunings(s, t);
	EEPROM.put(a, s);
	a += sizeof(s);
	EEPROM.put(a, t);
	a += sizeof(t);

	endOfMoveMgr.getTunings(e, t);
	EEPROM.put(a, e);
	a += sizeof(e);
	EEPROM.put(a, t);
	a += sizeof(t);

	EEPROM.put(a, maxAcceleration);
	a += sizeof(maxAcceleration);

	EEPROM.put(a, maxDeceleration);
	a += sizeof(maxDeceleration);

	kp = forwardTranslationPID.getKp();
	EEPROM.put(a, kp);
	a += sizeof(kp);
	ki = forwardTranslationPID.getKi();
	EEPROM.put(a, ki);
	a += sizeof(ki);
	kd = forwardTranslationPID.getKd();
	EEPROM.put(a, kd);
	a += sizeof(kd);

	kp = backwardTranslationPID.getKp();
	EEPROM.put(a, kp);
	a += sizeof(kp);
	ki = backwardTranslationPID.getKi();
	EEPROM.put(a, ki);
	a += sizeof(ki);
	kd = backwardTranslationPID.getKd();
	EEPROM.put(a, kd);
	a += sizeof(kd);

	kp = leftSpeedPID.getKp();
	EEPROM.put(a, kp);
	a += sizeof(kp);
	ki = leftSpeedPID.getKi();
	EEPROM.put(a, ki);
	a += sizeof(ki);
	kd = leftSpeedPID.getKd();
	EEPROM.put(a, kd);
	a += sizeof(kd);

	kp = rightSpeedPID.getKp();
	EEPROM.put(a, kp);
	a += sizeof(kp);
	ki = rightSpeedPID.getKi();
	EEPROM.put(a, ki);
	a += sizeof(ki);
	kd = rightSpeedPID.getKd();
	EEPROM.put(a, kd);
	a += sizeof(kd);

	EEPROM.put(a, curvatureCorrectorK1);
	a += sizeof(curvatureCorrectorK1);
	EEPROM.put(a, curvatureCorrectorK2);
	a += sizeof(curvatureCorrectorK2);

	EEPROM.put(a, pidToSet);
	a += sizeof(pidToSet);
}

void MotionControlSystem::loadParameters()
{
	noInterrupts();

	int a = 0; // Adresse mémoire dans l'EEPROM
	float kp, ki, kd, s;
	uint32_t e, t;

	EEPROM.get(a, positionControlled);
	a += sizeof(positionControlled);
	EEPROM.get(a, leftSpeedControlled);
	a += sizeof(leftSpeedControlled);
	EEPROM.get(a, rightSpeedControlled);
	a += sizeof(rightSpeedControlled);
	EEPROM.get(a, pwmControlled);
	a += sizeof(pwmControlled);

	EEPROM.get(a, s);
	a += sizeof(s);
	EEPROM.get(a, t);
	a += sizeof(t);
	leftMotorBlockingMgr.setTunings(s, t);

	EEPROM.get(a, s);
	a += sizeof(s);
	EEPROM.get(a, t);
	a += sizeof(t);
	rightMotorBlockingMgr.setTunings(s, t);

	EEPROM.get(a, e);
	a += sizeof(e);
	EEPROM.get(a, t);
	a += sizeof(t);
	endOfMoveMgr.setTunings(e, t);

	EEPROM.get(a, maxAcceleration);
	a += sizeof(maxAcceleration);

	EEPROM.get(a, maxDeceleration);
	a += sizeof(maxDeceleration);

	EEPROM.get(a, kp);
	a += sizeof(kp);
	EEPROM.get(a, ki);
	a += sizeof(ki);
	EEPROM.get(a, kd);
	a += sizeof(kd);
	forwardTranslationPID.setTunings(kp, ki, kd);

	EEPROM.get(a, kp);
	a += sizeof(kp);
	EEPROM.get(a, ki);
	a += sizeof(ki);
	EEPROM.get(a, kd);
	a += sizeof(kd);
	backwardTranslationPID.setTunings(kp, ki, kd);

	EEPROM.get(a, kp);
	a += sizeof(kp);
	EEPROM.get(a, ki);
	a += sizeof(ki);
	EEPROM.get(a, kd);
	a += sizeof(kd);
	leftSpeedPID.setTunings(kp, ki, kd);

	EEPROM.get(a, kp);
	a += sizeof(kp);
	EEPROM.get(a, ki);
	a += sizeof(ki);
	EEPROM.get(a, kd);
	a += sizeof(kd);
	rightSpeedPID.setTunings(kp, ki, kd);

	EEPROM.get(a, curvatureCorrectorK1);
	a += sizeof(curvatureCorrectorK1);
	EEPROM.get(a, curvatureCorrectorK2);
	a += sizeof(curvatureCorrectorK2);

	EEPROM.get(a, pidToSet);
	a += sizeof(pidToSet);

	interrupts();
}

void MotionControlSystem::loadDefaultParameters()
{
	noInterrupts();

	positionControlled = true;
	leftSpeedControlled = true;
	rightSpeedControlled = true;
	pwmControlled = true;

	leftMotorBlockingMgr.setTunings(0, 0);
	rightMotorBlockingMgr.setTunings(0, 0);
	endOfMoveMgr.setTunings(100, 100);

	maxAcceleration = 25;
	maxDeceleration = 125;

	forwardTranslationPID.setTunings(2.75, 0, 1.5);
	backwardTranslationPID.setTunings(1.75, 0, 1);
	leftSpeedPID.setTunings(0.6, 0.01, 20);
	rightSpeedPID.setTunings(0.6, 0.01, 20);
	curvatureCorrectorK1 = 0.1;
	curvatureCorrectorK2 = 12;

	pidToSet = SPEED;

	interrupts();
}

void MotionControlSystem::logAllData()
{
	static Position nonVolatilePos;
	static uint32_t lastLogTime = 0;
	if (micros() - lastLogTime > 10000)
	{
		if (micros() - lastLogTime > 13000)
		{
			//Serial.printf("LATENCE (%d)\n", micros() - lastLogTime);
		}
		lastLogTime = micros();
		noInterrupts();
		nonVolatilePos = position;
		Log::data(Log::POSITION, nonVolatilePos);
		Log::data(Log::PID_V_G, leftSpeedPID);
		Log::data(Log::PID_V_D, rightSpeedPID);
		if (maxMovingSpeed >= 0)
		{
			Log::data(Log::PID_TRANS, forwardTranslationPID);
		}
		else
		{
			Log::data(Log::PID_TRANS, backwardTranslationPID);
		}
		Log::data(Log::BLOCKING_M_G, leftMotorBlockingMgr);
		Log::data(Log::BLOCKING_M_D, rightMotorBlockingMgr);
		Log::data(Log::STOPPING_MGR, endOfMoveMgr);
		Log::data(Log::TRAJ_ERR, watchTrajErrors);
		interrupts();
	}
}

void MotionControlSystem::printCurrentTrajectory()
{
	Serial.println("Current trajectory");
	for (int i = 0; i < UINT8_MAX + 1; i++)
	{
		Serial.print(i);
		Serial.print("  ");
		Serial.println(currentTrajectory[i]);
	}
	Serial.println("End of trajectory");
}

uint32_t MotionControlSystem::getLastInterruptDuration()
{
	noInterrupts();
	uint32_t cpy = lastInterruptDuration;
	interrupts();
	return cpy;
}

uint32_t MotionControlSystem::getMaxInterruptDuration()
{
	noInterrupts();
	uint32_t cpy = maxInterruptDuration;
	interrupts();
	return cpy;
}

void MotionControlSystem::setPWM(int32_t pwm)
{
	leftPWM = pwm;
	rightPWM = pwm;
}

void MotionControlSystem::setSpeed(int32_t speed)
{
	leftSpeedSetpoint = speed;
	rightSpeedSetpoint = speed;
}

void MotionControlSystem::setTranslation(int32_t distance)
{
	translationSetpoint += distance;
}

void MotionControlSystem::moveIsStarting()
{
	endOfMoveMgr.moveIsStarting();
}

