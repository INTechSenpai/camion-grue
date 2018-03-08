#ifndef _DIRECTIONCONTROLLER_h
#define _DIRECTIONCONTROLLER_h

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include <Printable.h>
#include "../Tools/Singleton.h"
#include "../Tools/Utils.h"
#include "../Config/serial_config.h"
#include "../Config/physical_dimensions.h"
#include "../Servos/dynamixel_teensy/src/Dynamixel.h"
#include "../Servos/dynamixel_teensy/src/DynamixelInterface.h"
#include "../Servos/dynamixel_teensy/src/DynamixelMotor.h"
#include "../CommunicationServer/CommunicationServer.h"


/* IDs des AX12 */
#define ID_AX12_FRONT_LEFT  0
#define ID_AX12_FRONT_RIGHT 1
#define ID_AX12_BACK_LEFT   2
#define ID_AX12_BACK_RIGHT  3

/* Periode d'actualisation d'une requête AX12 (4 requêtes au total) */
#define CONTROL_PERIOD	3125 // µs

#define MAGIC_CORRECTOR	1

/* Angles des AX12 correspondant à des roues alignées vers l'avant */
#define ANGLE_ORIGIN	150

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
		serialAX(SERIAL_AX12),
        frontLeftMotor(serialAX, ID_AX12_FRONT_LEFT),
        frontRightMotor(serialAX, ID_AX12_FRONT_RIGHT),
        backLeftMotor(serialAX, ID_AX12_BACK_LEFT),
        backRightMotor(serialAX, ID_AX12_BACK_RIGHT)
	{
        serialAX.begin(SERIAL_AX12_BAUDRATE, 50);
		aimCurvature = 0;
		updateAimAngles();
		realLeftAngle = ANGLE_ORIGIN;
		realRightAngle = ANGLE_ORIGIN;
		updateRealCurvature();
        frontLeftMotor.init();
        frontRightMotor.init();
        backLeftMotor.init();
        backRightMotor.init();
        frontLeftMotor.enableTorque();
        frontRightMotor.enableTorque();
        backLeftMotor.enableTorque();
        backRightMotor.enableTorque();
        frontLeftMotor.jointMode();
        frontRightMotor.jointMode();
        backLeftMotor.jointMode();
        backRightMotor.jointMode();
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
				frontLeftMotor.goalPositionDegree(aimLeftAngle + 1);
				counter++;
			}
			else if (counter == 1)
			{
				frontRightMotor.goalPositionDegree(aimRightAngle + 1);
				counter++;
			}
            else if (counter == 2)
            {
                backLeftMotor.goalPositionDegree(2 * ANGLE_ORIGIN - aimLeftAngle + 1);
                counter++;
            }
            else if (counter == 3)
            {
                backRightMotor.goalPositionDegree(2 * ANGLE_ORIGIN - aimRightAngle + 1);
                counter++;
            }
			else if (counter == 4)
			{
				uint16_t frontAngle = frontLeftMotor.currentPositionDegree();
                uint16_t backAngle = backLeftMotor.currentPositionDegree();
				if (frontAngle <= 300 && backAngle <= 300)
				{
					realLeftAngle = (frontAngle + 2 * ANGLE_ORIGIN - backAngle) / 2;
				}
                else if (frontAngle <= 300)
                {
                    realLeftAngle = frontAngle;
                }
                else if (backAngle <= 300)
                {
                    realLeftAngle = 2 * ANGLE_ORIGIN - backAngle;
                }
				counter++;
			}
			else
			{
                uint16_t frontAngle = frontRightMotor.currentPositionDegree();
                uint16_t backAngle = backRightMotor.currentPositionDegree();
                if (frontAngle <= 300 && backAngle <= 300)
                {
                    realRightAngle = (frontAngle + 2 * ANGLE_ORIGIN - backAngle) / 2;
                }
                else if (frontAngle <= 300)
                {
                    realRightAngle = frontAngle;
                }
                else if (backAngle <= 300)
                {
                    realRightAngle = 2 * ANGLE_ORIGIN - backAngle;
                }
				counter = 0;
			}
			updateRealCurvature();

            //Server.sendData(DIRECTION, ?);
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
		if (realLeftAngle == ANGLE_ORIGIN)
		{
			leftCurvature = 0;
		}
		else
		{
			float leftAngle_rad = ((float)realLeftAngle - ANGLE_ORIGIN) * PI / 180;
			leftCurvature = -1000 / (FRONT_BACK_WHEELS_DISTANCE / tanf(leftAngle_rad) - DIRECTION_ROTATION_POINT_Y);
		}

		if (realRightAngle == ANGLE_ORIGIN)
		{
			rightCurvature = 0;
		}
		else
		{
			float rightAngle_rad = ((float)realRightAngle - ANGLE_ORIGIN) * PI / 180;
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
		aimLeftAngle = (uint16_t)((ANGLE_ORIGIN + leftAngle_rad * 180 / PI) + 0.5);
		aimRightAngle = (uint16_t)((ANGLE_ORIGIN + rightAngle_rad * 180 / PI) + 0.5);

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
	DynamixelInterface serialAX;
	DynamixelMotor frontLeftMotor;
	DynamixelMotor frontRightMotor;
	DynamixelMotor backLeftMotor;
	DynamixelMotor backRightMotor;

	/* Indique que le robot est en train de faire un scann */
	volatile bool scanning;
	uint32_t scannLastIterationTime;
	uint32_t scannBeginTime;
	bool scannPhaseLeft;
};


#endif

