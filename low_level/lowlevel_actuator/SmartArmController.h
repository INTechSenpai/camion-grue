#ifndef _SMART_ARM_CONTROLLER_h
#define _SMART_ARM_CONTROLLER_h

#include "ArmController.h"
#include "ArmPosition.h"
#include "ArmStatus.h"
#include "ToF_shortRange.h"
#include "config.h"


class SmartArmControler
{
public:
    SmartArmControler() :
        armControler(ArmController::Instance()),
        intSensor("int", 42, PIN_EN_TOF_INT, 15, 70),
        extSensor("ext", 43, PIN_EN_TOF_EXT, 15, 160)
    {
        intSensorValue = (SensorValue)SENSOR_DEAD;
        extSensorValue = (SensorValue)SENSOR_DEAD;
        status = ARM_STATUS_OK;
    }

    int init()
    {
        int ret = armControler.init();
        if (intSensor.powerON() == 0 && extSensor.powerON() == 0)
        {
            return ret;
        }
        else
        {
            return -1;
        }
    }

    void control()
    {

    }

    bool isMoving()
    {
        return armControler.isMoving();
    }

    ArmStatus getStatus()
    {
        return status | armControler.getStatus();
    }


private:
    ArmController & armControler;
    ToF_shortRange intSensor;
    SensorValue intSensorValue;
    ToF_shortRange extSensor;
    SensorValue extSensorValue;
    ArmStatus status;
};


#endif
