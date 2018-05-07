#ifndef _SENSORS_MGR_h
#define _SENSORS_MGR_h

#include "ToF_longRange.h"
#include "XL320.h"
#include "config.h"

#define SERVO_LEFT_ORIGIN   (1.4)
#define SERVO_RIGHT_ORIGIN  (4.3)

#define SERVO_UPDATE_PERIOD 50  // ms


class SensorsMgr
{
public:
    SensorsMgr() :
        servos(SERIAL_XL320),
        sensorLeft(44, PIN_EN_TOF_G),
        sensorRight(45, PIN_EN_TOF_D)
    {
        aimLeft = 512;
        aimRight = 512;
        currentLeftAngle = HALF_PI;
        currentRightAngle = -HALF_PI;
    }

    int init()
    {
        servos.begin(SERIAL_XL320_BAUDRATE, 4);
        servos.setSpeed(ID_XL320_LEFT, 1023);
        servos.setSpeed(ID_XL320_RIGHT, 1023);
        sensorLeft.powerON("Left");
        sensorRight.powerON("Right");
        return 0;
    }

    void updateServos()
    {
        static uint32_t lastUpdateTime = 0;
        if (millis() - lastUpdateTime > SERVO_UPDATE_PERIOD)
        {
            lastUpdateTime = millis();
            servos.setPosition(ID_XL320_LEFT, aimLeft);
            servos.setPosition(ID_XL320_RIGHT, aimRight);
        }
    }

    void getSensorsData(int32_t & tof_g, int32_t & tof_d, float & angleTG, float & angleTD)
    {
        uint16_t rightRotatingSpeed = servos.getPresentSpeed(ID_XL320_RIGHT);
        uint16_t leftRotatingSpeed = servos.getPresentSpeed(ID_XL320_LEFT);
        if (leftRotatingSpeed != UINT16_MAX && (leftRotatingSpeed & 1023) < 10)
        {
            tof_g = sensorLeft.getMesure();
            uint16_t newLeftAngle = servos.getPresentPosition(ID_XL320_LEFT);
            if (newLeftAngle < 1024)
            {
                currentLeftAngle = xl_to_rad(newLeftAngle) - SERVO_LEFT_ORIGIN;
            }
        }
        else
        {
            tof_g = 0;
        }

        if (rightRotatingSpeed != UINT16_MAX && (rightRotatingSpeed & 1023) < 10)
        {
            tof_d = sensorRight.getMesure();
            uint16_t newRightAngle = servos.getPresentPosition(ID_XL320_RIGHT);
            if (newRightAngle < 1024)
            {
                currentRightAngle = xl_to_rad(newRightAngle) - SERVO_RIGHT_ORIGIN;
            }
        }
        else
        {
            tof_d = 0;
        }
        angleTG = currentLeftAngle;
        angleTD = currentRightAngle;
    }

    void setAimAngles(float angleG, float angleD)
    {
        aimLeft = rad_to_xl(angleG + SERVO_LEFT_ORIGIN);
        aimRight = rad_to_xl(angleD + SERVO_RIGHT_ORIGIN);
    }

private:
    uint16_t rad_to_xl(float angle_rad)
    {
        float angle_xl = constrain(angle_rad * 1023 / 5.236, 0, 1023);
        return (uint16_t)angle_xl;
    }

    float xl_to_rad(uint16_t angle_xl)
    {
        return (float)angle_xl * 5.236 / 1023;
    }

    XL320 servos;
    uint16_t aimLeft;           // [Dynamixel internal unit]
    uint16_t aimRight;          // [Dynamixel internal unit]
    float currentLeftAngle;     // [rad]
    float currentRightAngle;    // [rad]

    ToF_longRange sensorLeft;
    ToF_longRange sensorRight;
};


#endif
