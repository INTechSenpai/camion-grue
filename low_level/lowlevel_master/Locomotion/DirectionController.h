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
#define ANGLE_ORIGIN	149

#define ANGLE_MIN	90
#define ANGLE_MAX	210

#define SCANN_PERIOD		100		// ms
#define SCANN_DELTA			1		// m^-1
#define SCANN_UPPER_BOUND	(5.5)	// m^-1
#define SCANN_LOWER_BOUND	(-5.5)	// m^-1
#define SCANN_TIMEOUT		3000	// ms


enum DirectionControllerStatus
{
    DIRECTION_CONTROLLER_OK,
    DIRECTION_CONTROLLER_MOTOR_BLOCKED
};


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
        serialAX.begin(SERIAL_AX12_BAUDRATE, SERIAL_AX12_TIMEOUT);
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
	}

    DirectionControllerStatus control()
	{
		static uint32_t lastUpdateTime = 0;
		static uint8_t counter = 0;
        DirectionControllerStatus ret = DIRECTION_CONTROLLER_OK;
		
		if (micros() - lastUpdateTime >= CONTROL_PERIOD)
		{
            DynamixelStatus dynamixelStatus = DYN_STATUS_OK;
			lastUpdateTime = micros();
			updateAimAngles();
			if (counter == 0)
			{
                dynamixelStatus = frontLeftMotor.goalPositionDegree(aimLeftAngle) != DYN_STATUS_OK;
				counter++;
			}
			else if (counter == 1)
			{
                dynamixelStatus = frontRightMotor.goalPositionDegree(aimRightAngle);
				counter++;
			}
            else if (counter == 2)
            {
                dynamixelStatus = backLeftMotor.goalPositionDegree(2 * ANGLE_ORIGIN - aimLeftAngle);
                counter++;
            }
            else if (counter == 3)
            {
                dynamixelStatus = backRightMotor.goalPositionDegree(2 * ANGLE_ORIGIN - aimRightAngle);
                counter++;
            }
			else if (counter == 4)
			{
                uint16_t frontAngle;
                uint16_t backAngle;
                dynamixelStatus = frontLeftMotor.currentPositionDegree(frontAngle);
                dynamixelStatus |= backLeftMotor.currentPositionDegree(backAngle);
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
                uint16_t frontAngle;
                uint16_t backAngle;
                dynamixelStatus = frontRightMotor.currentPositionDegree(frontAngle);
                dynamixelStatus |= backRightMotor.currentPositionDegree(backAngle);
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

            if (dynamixelStatus != DYN_STATUS_OK)
            {
                Server.printf_err("DirectionController: errno %u on operation #%u\n", dynamixelStatus, counter);
            }
            if (dynamixelStatus & (DYN_STATUS_OVERLOAD_ERROR | DYN_STATUS_OVERHEATING_ERROR))
            {
                ret = DIRECTION_CONTROLLER_MOTOR_BLOCKED;
            }

            Server.print(DIRECTION, *this);
		}

        return ret;
	}

    void recover()
    {
        frontLeftMotor.recoverTorque();
        frontRightMotor.recoverTorque();
        backLeftMotor.recoverTorque();
        backRightMotor.recoverTorque();
    }
	
	void setAimCurvature(float curvature)
	{
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
		return p.printf("%u_%g_%g", millis(), aimCurvature, realCurvature);
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

		if (aimLeftAngle < ANGLE_MIN)
		{
			aimLeftAngle = ANGLE_MIN;
		}
		else if (aimLeftAngle > ANGLE_MAX)
		{
			aimLeftAngle = ANGLE_MAX;
		}

		if (aimRightAngle < ANGLE_MIN)
		{
			aimRightAngle = ANGLE_MIN;
		}
		else if (aimRightAngle > ANGLE_MAX)
		{
			aimRightAngle = ANGLE_MAX;
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
};


#endif

