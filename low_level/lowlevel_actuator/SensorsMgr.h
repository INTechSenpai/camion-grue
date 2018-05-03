#ifndef _SENSORS_MGR_h
#define _SENSORS_MGR_h

#include "ToF_longRange.h"
#include "XL320.h"
#include "config.h"

#define SERVO_LEFT_ORIGIN   (1.0472)
#define SERVO_RIGHT_ORIGIN  (4.1888)

#define SERVO_UPDATE_PERIOD 50  // ms


class SensorsMgr
{
public:
    SensorsMgr() :
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
        SERIAL_XL320.begin(SERIAL_XL320_BAUDRATE);
        SERIAL_XL320.setTimeout(10);
        servos.begin(SERIAL_XL320);
        servos.setJointSpeed(ID_XL320_LEFT, 1023);
        servos.setJointSpeed(ID_XL320_RIGHT, 1023);
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
            servos.moveJoint(ID_XL320_LEFT, aimLeft);
            servos.moveJoint(ID_XL320_RIGHT, aimRight);
        }
    }

    void getSensorsData(int32_t & tof_g, int32_t & tof_d, float & angleTG, float & angleTD)
    {
        tof_g = sensorLeft.getMesure();
        tof_d = sensorRight.getMesure();
        int newLeftAngle = servos.getJointPosition(ID_XL320_LEFT);
        int newRightAngle = servos.getJointPosition(ID_XL320_RIGHT);
        if (newLeftAngle >= 0)
        {
            currentLeftAngle = xl_to_rad((uint16_t)newLeftAngle) - SERVO_LEFT_ORIGIN;
        }
        if (newRightAngle >= 0)
        {
            currentRightAngle = xl_to_rad((uint16_t)newRightAngle) - SERVO_RIGHT_ORIGIN;
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
