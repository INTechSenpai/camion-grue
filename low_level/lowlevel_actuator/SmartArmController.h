#ifndef _SMART_ARM_CONTROLLER_h
#define _SMART_ARM_CONTROLLER_h

#include "ArmController.h"
#include "ArmPosition.h"
#include "ArmStatus.h"
#include "ToF_shortRange.h"
#include "config.h"
#include "CommandId.h"

#define ARM_SENSORS_UPDATE_PERIOD   20  // ms
#define CUBE_DETECT_MIN             60  // mm
#define CUBE_DETECT_MAX             160 // mm
#define CUBE_INSIDE_THRESHOLD       60  // mm


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
        currentCommand = ACTUATOR_NO_COMMAND;
        currentCommandStep = 0;
        commandArgAngle = 0;
        commandArgHeight = 0;
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
        static uint32_t sensorLastUpdateTime = 0;
        uint32_t now = millis();
        if (now - sensorLastUpdateTime > ARM_SENSORS_UPDATE_PERIOD)
        {
            sensorLastUpdateTime = now;
            intSensorValue = intSensor.getMesure();
            extSensorValue = extSensor.getMesure();
            if (intSensorValue == (SensorValue)SENSOR_DEAD || extSensorValue == (SensorValue)SENSOR_DEAD)
            {
                status |= ARM_STATUS_SENSOR_ERR;
            }
        }
        armControler.controlServos();

        if (currentCommand != ACTUATOR_NO_COMMAND)
        {
            switch (currentCommand)
            {
            case ACTUATOR_GO_TO_HOME:
                go_to_home();
                break;
            case ACTUATOR_TAKE_CUBE_SMART:
                take_cube_smart();
                break;
            case ACTUATOR_TAKE_CUBE_FIXED:
                take_cube_smart(false);
                break;
            case ACTUATOR_STORE_CUBE_INSIDE:
                store_cube_inside();
                break;
            case ACTUATOR_STORE_CUBE_TOP:
                store_cube_top();
                break;
            case ACTUATOR_TAKE_CUBE_STORAGE:
                take_cube_storage();
                break;
            case ACTUATOR_PUT_CUBE_SMART:
                put_cube_smart();
                break;
            case ACTUATOR_PUT_CUBE_FIXED:
                put_cube_smart(false);
                break;
            default:
                Serial.println("SmartArmController: unknown command");
                stopCommand();
                break;
            }
        }
    }

    bool isMoving()
    {
        return currentCommand != ACTUATOR_NO_COMMAND;
    }

    ArmStatus getStatus()
    {
        return status | armControler.getStatus();
    }

    void executeCommand(CommandId id, float angle = 0, int32_t height = 0)
    {
        if (currentCommand != ACTUATOR_NO_COMMAND)
        {
            Serial.println("SmartArmController: command already running");
            return;
        }
        status = ARM_STATUS_OK;
        currentCommand = id;
        currentCommandStep = 0;
        commandArgAngle = angle;
        commandArgHeight = height;
    }

    void emergencyStop()
    {
        armControler.stop();
        stopCommand();
    }

    bool isCubeInPlier()
    {
        if (intSensorValue == (SensorValue)SENSOR_DEAD)
        {
            return true;
        }
        else if (intSensorValue == (SensorValue)SENSOR_NOT_UPDATED)
        {
            return false;
        }
        else if (intSensorValue == (SensorValue)OBSTACLE_TOO_CLOSE)
        {
            return true;
        }
        else if (intSensorValue == (SensorValue)NO_OBSTACLE)
        {
            return false;
        }
        else
        {
            return intSensorValue < CUBE_INSIDE_THRESHOLD;
        }
    }

private:
    void stopCommand()
    {
        currentCommand = ACTUATOR_NO_COMMAND;
        currentCommandStep = 0;
        commandArgAngle = 0;
        commandArgHeight = 0;
    }

    void waitForMoveCompletion()
    {
        if (!armControler.isMoving())
        {
            currentCommandStep++;
        }
    }

    void go_to_home()
    {
        switch (currentCommandStep)
        {
        case 0:
            // D�gagement du bras pour �viter la cabine si n�cessaire
            armControler.getCurrentPosition(armPosition);
            float hAngle = armPosition.getHAngle();
            if (abs(hAngle) < ARM_H_ANGLE_CABIN && armPosition.getHeadGlobalAngle() > 0)
            {
                if (hAngle > 0)
                {
                    armPosition.setHAngle(ARM_H_ANGLE_CABIN);
                }
                else
                {
                    armPosition.setHAngle(-ARM_H_ANGLE_CABIN);
                }
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            else
            {
                currentCommandStep += 2;
            }
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Repli du bras avec angleH constant
            armControler.getCurrentPosition(armPosition);
            armPosition.setVAngle(0);
            armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
            armPosition.setPlierPos(25);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Alignement du bras au dessus de la cabine
            armControler.getCurrentPosition(armPosition);
            armPosition.setHAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            stopCommand();
            break;
        default:
            Serial.println("Err unhandled command step");
            stopCommand();
            break;
        }
    }

    void take_cube_smart(bool useSensor = true)
    {
        switch (currentCommandStep)
        {
        case 0:
            if (abs(commandArgAngle) < ARM_H_ANGLE_CABIN || abs(commandArgAngle) > ARM_MAX_H_ANGLE)
            {
                Serial.println("take_cube_smart: Invalid horizontal angle");
                status |= ARM_STATUS_UNREACHABLE;
                stopCommand();
            }
            else
            {
                armControler.getCurrentPosition(armPosition);
                armPosition.setHAngle(commandArgAngle);
                armPosition.setVAngle(0);
                armPosition.setPlierPos(ARM_MAX_PLIER_POS);
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            break;
        case 1:
            armControler.getCurrentPosition(armPosition);
            float hAngle = armPosition.getHAngle();
            if (abs(hAngle) > ARM_H_ANGLE_CABIN)
            {
                float headAngle = armPosition.getHeadGlobalAngle();
                armControler.getAimPosition(armPosition);
                armPosition.setHeadGlobalAngle(ARM_HEAD_SCAN_ANGLE);
                if (headAngle > 0)
                {
                    armPosition.setHAngle(hAngle);
                    currentCommandStep++;
                }
                else
                {
                    currentCommandStep += 3;
                }
                armControler.setAimPosition(armPosition);
            }
            break;
        case 2:
            waitForMoveCompletion();
            break;
        case 3:
            // Rotation horizontale
            armControler.getCurrentPosition(armPosition);
            armPosition.setHAngle(commandArgAngle);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 4:
            // Ajustement de la rotation horizontale
            if (!useSensor)
            {
                currentCommandStep++;
            }
            else if (extSensorValue == (SensorValue)SENSOR_DEAD)
            {
                status |= ARM_STATUS_SENSOR_ERR;
                currentCommandStep++;
            }
            else if (extSensorValue > CUBE_DETECT_MIN && extSensorValue < CUBE_DETECT_MAX)
            {
                armControler.getCurrentPosition(armPosition);
                float hAngle = armPosition.getHAngle();
                if (hAngle > 0) {
                    hAngle += 0.1;
                }
                else {
                    hAngle -= 0.1;
                }
                armPosition.setHAngle(hAngle);
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            else if (!armControler.isMoving())
            {
                status |= ARM_STATUS_NO_DETECTION;
                currentCommandStep += 2;
            }
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Abaissement du bras
            armControler.getCurrentPosition(armPosition);
            armPosition.setVAngle(ARM_V_ANGLE_STAGE_0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            // Positionnement de la t�te du bras
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadGlobalAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 9:
            waitForMoveCompletion();
            break;
        case 10:
            // Fermeture de la pince
            armControler.getCurrentPosition(armPosition);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 11:
            waitForMoveCompletion();
            break;
        case 12:
            // Lev�e du cube
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadLocalAngle(ARM_HEAD_L_ANGLE_TRANSPORT);
            armPosition.setVAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 13:
            waitForMoveCompletion();
            break;
        case 14:
            stopCommand();
            break;
        default:
            Serial.println("Err unhandled command step");
            stopCommand();
            break;
        }
    }


    void store_cube_inside()
    {
        switch (currentCommandStep)
        {
        case 0:
            // Lev�e du cube
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadLocalAngle(ARM_HEAD_L_ANGLE_TRANSPORT);
            armPosition.setVAngle(ARM_V_ANGLE_STORAGE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Placement au dessus de la zone de stockage
            armControler.getCurrentPosition(armPosition);
            armPosition.setHAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
        case 4:
            // D�pose du cube
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadGlobalAngle(ARM_HEAD_G_ANGLE_STORAGE);
            armPosition.setPlierPos(25);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Rangement du bras
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
            armPosition.setVAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            stopCommand();
            break;
        default:
            Serial.println("Err unhandled command step");
            stopCommand();
            break;
        }
    }

    void store_cube_top()
    {
        switch (currentCommandStep)
        {
        case 0:
            // Position initiale
            armControler.getCurrentPosition(armPosition);
            armPosition.setHAngle(ARM_H_ANGLE_CABIN);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Lev�e du cube
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadLocalAngle(ARM_MIN_HEAD_ANGLE);
            armPosition.setVAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Alignement du bras
            armControler.getCurrentPosition(armPosition);
            armPosition.setHAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            stopCommand();
            break;
        default:
            Serial.println("Err unhandled command step");
            stopCommand();
            break;
        }
    }

    void take_cube_storage()
    {
        switch (currentCommandStep)
        {
        case 0:
            // Lev�e du bras
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
            armPosition.setPlierPos(ARM_MAX_PLIER_POS);
            armPosition.setVAngle(ARM_V_ANGLE_STORAGE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Alignement du bras
            armControler.getCurrentPosition(armPosition);
            armPosition.setHAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Abaissement de la t�te du bras
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadGlobalAngle(ARM_HEAD_G_ANGLE_STORAGE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Pr�hension du cube
            armControler.getCurrentPosition(armPosition);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            armControler.getCurrentPosition(armPosition);
            armPosition.setHeadLocalAngle(ARM_HEAD_L_ANGLE_TRANSPORT);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 9:
            waitForMoveCompletion();
            break;
        case 10:
            stopCommand();
            break;
        default:
            Serial.println("Err unhandled command step");
            stopCommand();
            break;
        }
    }

    void put_cube_smart(bool useSensor = true)
    {
        switch (currentCommandStep)
        {
        case 0:
            armControler.getCurrentPosition(armPosition);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            break;
        default:
            Serial.println("Err unhandled command step");
            stopCommand();
            break;
        }
    }

    ArmController & armControler;
    ToF_shortRange intSensor;
    SensorValue intSensorValue;
    ToF_shortRange extSensor;
    SensorValue extSensorValue;
    ArmStatus status;
    CommandId currentCommand;
    uint8_t currentCommandStep;
    float commandArgAngle;
    int32_t commandArgHeight;
    ArmPosition armPosition;
};


#endif
