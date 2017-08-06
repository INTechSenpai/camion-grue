#ifndef _DIRECTIONCONTROLLER_h
#define _DIRECTIONCONTROLLER_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include "Singleton.h"
#include "physical_dimensions.h"
#include <Printable.h>
#include "InterfaceAX12.h"
#include "DynamixelMotor.h"
#include "ax12config.h"
#include "Log.h"


/* Periode d'actualisation d'une requête AX12 (4 requêtes au total) */
#define CONTROL_PERIOD	3125 // µs

#define MAGIC_CORRECTOR	0.937

/* Angles des AX12 correspondant à des roues alignées vers l'avant */
#define LEFT_ANGLE_ORIGIN	150
#define RIGHT_ANGLE_ORIGIN	150

#define ANGLE_MIN_LEFT	84
#define ANGLE_MIN_RIGHT	105
#define ANGLE_MAX_LEFT	193
#define ANGLE_MAX_RIGHT	216

#define SCANN_PERIOD		100		// ms
#define SCANN_DELTA			1		// m^-1
#define SCANN_UPPER_BOUND	(5.5)	// m^-1
#define SCANN_LOWER_BOUND	(-5.5)	// m^-1
#define SCANN_TIMEOUT		3000	// ms


class DirectionController : public Singleton<DirectionController>, public Printable
{
public:
	DirectionController() :
		serialAX(InterfaceAX12::Instance()),
		leftMotor(serialAX.serial, ID_LEFT_AX12),
		rightMotor(serialAX.serial, ID_RIGHT_AX12)
	{
		aimCurvature = 0;
		updateAimAngles();
		realLeftAngle = LEFT_ANGLE_ORIGIN;
		realRightAngle = RIGHT_ANGLE_ORIGIN;
		updateRealCurvature();
		leftMotor.init();
		rightMotor.init();
		leftMotor.enableTorque();
		rightMotor.enableTorque();
		leftMotor.jointMode();
		rightMotor.jointMode();
		scanning = false;
	}

	void control()
	{
		static uint32_t lastUpdateTime = 0;
		static uint8_t counter = 0;
		
		if (micros() - lastUpdateTime >= CONTROL_PERIOD)
		{
			lastUpdateTime = micros();
			updateAimAngles();
			if (counter == 0)
			{
				leftMotor.goalPositionDegree(aimLeftAngle + 1);
				counter = 1;
			}
			else if (counter == 1)
			{
				rightMotor.goalPositionDegree(aimRightAngle + 1);
				counter = 2;
			}
			else if (counter == 2)
			{
				uint16_t angle = leftMotor.currentPositionDegree();
				if (angle <= 300)
				{
					realLeftAngle = angle;
				}
				counter = 3;
			}
			else
			{
				uint16_t angle = rightMotor.currentPositionDegree();
				if (angle <= 300)
				{
					realRightAngle = angle;
				}
				counter = 0;
			}
			updateRealCurvature();

			Log::data(Log::DIRECTION, *this);
		}
	}
	
	void setAimCurvature(float curvature)
	{
		scanning = false;
		aimCurvature = curvature * MAGIC_CORRECTOR;
	}
	float getRealCurvature() const
	{
		return realCurvature / MAGIC_CORRECTOR;
	}
	uint16_t getLeftAngle() const
	{
		return realLeftAngle;
	}
	uint16_t getRightAngle() const
	{
		return realRightAngle;
	}
	void setLeftAngle(uint16_t angle)
	{
		aimLeftAngle = angle;
	}
	void setRightAngle(uint16_t angle)
	{
		aimRightAngle = angle;
	}

	size_t printTo(Print& p) const
	{
		return p.printf("%g_%g_%u_%u_%u_%u", aimCurvature, realCurvature, realLeftAngle, realRightAngle, aimLeftAngle, aimRightAngle);
	}

	/* Fait tourner les roues doucement vers la gauche, puis vers la droite. Renvoie vrai si la procédure est terminée, faux sinon */
	bool scann(bool launch)
	{
		if (launch)
		{
			scanning = true;
			scannLastIterationTime = 0;
			scannBeginTime = millis();
			scannPhaseLeft = true;
			return false;
		}
		else if (scanning)
		{
			if (millis() - scannBeginTime > SCANN_TIMEOUT)
			{
				scanning = false;
				return true;
			}

			if (millis() - scannLastIterationTime > SCANN_PERIOD)
			{
				scannLastIterationTime = millis();
				if (scannPhaseLeft)
				{
					aimCurvature = SCANN_UPPER_BOUND;
					if (ABS(aimCurvature - realCurvature) < 0.3)
					{
						scannPhaseLeft = false;
					}
					return false;
				}
				else
				{
					aimCurvature -= SCANN_DELTA;
					if (aimCurvature < SCANN_LOWER_BOUND)
					{
						scanning = false;
						return true;
					}
					return false;
				}
			}
			else
			{
				return false;
			}
		}
		else
		{
			return true;
		}
	}


private:
	void updateRealCurvature()
	{
		float leftCurvature, rightCurvature;
		if (realLeftAngle == LEFT_ANGLE_ORIGIN)
		{
			leftCurvature = 0;
		}
		else
		{
			float leftAngle_rad = ((float)realLeftAngle - LEFT_ANGLE_ORIGIN) * PI / 180;
			leftCurvature = -1000 / (FRONT_BACK_WHEELS_DISTANCE / tanf(leftAngle_rad) - DIRECTION_ROTATION_POINT_Y);
		}

		if (realRightAngle == RIGHT_ANGLE_ORIGIN)
		{
			rightCurvature = 0;
		}
		else
		{
			float rightAngle_rad = ((float)realRightAngle - RIGHT_ANGLE_ORIGIN) * PI / 180;
			rightCurvature = -1000 / (FRONT_BACK_WHEELS_DISTANCE / tanf(rightAngle_rad) + DIRECTION_ROTATION_POINT_Y);
		}
		noInterrupts();
		realCurvature = (leftCurvature + rightCurvature) / 2;
		interrupts();
	}

	void updateAimAngles()
	{
		noInterrupts();
		float aimCurvature_cpy = aimCurvature;
		interrupts();
		float leftAngle_rad, rightAngle_rad;
		if (aimCurvature_cpy == 0)
		{
			leftAngle_rad = 0;
			rightAngle_rad = 0;
		}
		else
		{
			float bendRadius; // en mm
			bendRadius = (1 / aimCurvature_cpy) * 1000;
			if (aimCurvature_cpy > 0)
			{
				leftAngle_rad = -atan2f(FRONT_BACK_WHEELS_DISTANCE, bendRadius - DIRECTION_ROTATION_POINT_Y);
				rightAngle_rad = -atan2f(FRONT_BACK_WHEELS_DISTANCE, bendRadius + DIRECTION_ROTATION_POINT_Y);
			}
			else
			{
				leftAngle_rad = atan2f(FRONT_BACK_WHEELS_DISTANCE, DIRECTION_ROTATION_POINT_Y - bendRadius);
				rightAngle_rad = atan2f(FRONT_BACK_WHEELS_DISTANCE, -DIRECTION_ROTATION_POINT_Y - bendRadius);
			}
		}
		aimLeftAngle = (uint16_t)((LEFT_ANGLE_ORIGIN + leftAngle_rad * 180 / PI) + 0.5);
		aimRightAngle = (uint16_t)((RIGHT_ANGLE_ORIGIN + rightAngle_rad * 180 / PI) + 0.5);

		if (aimLeftAngle < ANGLE_MIN_LEFT)
		{
			aimLeftAngle = ANGLE_MIN_LEFT;
		}
		else if (aimLeftAngle > ANGLE_MAX_LEFT)
		{
			aimLeftAngle = ANGLE_MAX_LEFT;
		}

		if (aimRightAngle < ANGLE_MIN_RIGHT)
		{
			aimRightAngle = ANGLE_MIN_RIGHT;
		}
		else if (aimRightAngle > ANGLE_MAX_RIGHT)
		{
			aimRightAngle = ANGLE_MAX_RIGHT;
		}
	}
	
	/* Courbure, en m^-1 */
	volatile float aimCurvature;
	volatile float realCurvature;

	/* Angles des AX12, en degrés */
	uint16_t aimLeftAngle;
	uint16_t aimRightAngle;
	uint16_t realLeftAngle;
	uint16_t realRightAngle;

	/* Les AX12 de direction */
	InterfaceAX12 & serialAX;
	DynamixelMotor leftMotor;
	DynamixelMotor rightMotor;

	/* Indique que le robot est en train de faire un scann */
	volatile bool scanning;
	uint32_t scannLastIterationTime;
	uint32_t scannBeginTime;
	bool scannPhaseLeft;
};


#endif

