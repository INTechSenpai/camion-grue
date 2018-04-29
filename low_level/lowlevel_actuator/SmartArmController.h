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
        //while (ret != 0)
        //{
        //    ret = armControler.init();
        //}
        int failCount = 0;
        while (intSensor.powerON() != 0)
        {
            failCount++;
            if (failCount >= 10)
            {
                return -1;
            }
            Serial.println("intSensor retry");
        }
        failCount = 0;
        while (extSensor.powerON() != 0)
        {
            failCount++;
            if (failCount >= 10)
            {
                return -1;
            }
            Serial.println("extSensor retry");
        }
        return ret;
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
            case ACTUATOR_SET_ARM_POSITION:
                set_arm_position();
                break;
            default:
                Serial.println("SmartArmController: unknown command");
                stopCommand();
                break;
            }
        }
    }

    bool isMoving() const
    {
        return currentCommand != ACTUATOR_NO_COMMAND;
    }

    ArmStatus getStatus() const
    {
        return status | armControler.getStatus();
    }

    void getArmPosition(ArmPosition & position) const
    {
        armControler.getCurrentPosition(position);
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

    void setArmPosition(ArmPosition position)
    {
        if (currentCommand != ACTUATOR_NO_COMMAND)
        {
            Serial.println("SmartArmController: command already running");
            return;
        }
        status = ARM_STATUS_OK;
        currentCommand = ACTUATOR_SET_ARM_POSITION;
        currentCommandStep = 0;
        commandArgAngle = 0;
        commandArgHeight = 0;
        armControler.setAimPosition(position);
    }

    void emergencyStop()
    {
        armControler.stop();
        stopCommand();
    }

    bool isCubeInPlier() const
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
        {
            // Dégagement du bras pour éviter la cabine si nécessaire
            armControler.getCurrentPositionSpecial(armPosition);
            float hAngle = armPosition.getHAngle();
            if (abs(hAngle) < ARM_H_ANGLE_MANIP && armPosition.getHeadGlobalAngle() > 0)
            {
                if (hAngle > 0)
                {
                    armPosition.setHAngle(ARM_H_ANGLE_MANIP);
                }
                else
                {
                    armPosition.setHAngle(-ARM_H_ANGLE_MANIP);
                }
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            else
            {
                currentCommandStep += 2;
            }
            break;
        }
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Repli du bras avec angleH constant
            armControler.getCurrentPositionSpecial(armPosition);
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
            armControler.getCurrentPositionSpecial(armPosition);
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
            if (abs(commandArgAngle) < ARM_H_ANGLE_MANIP || abs(commandArgAngle) > ARM_MAX_H_ANGLE)
            {
                Serial.println("take_cube_smart: Invalid horizontal angle");
                status |= ARM_STATUS_UNREACHABLE;
                stopCommand();
            }
            else
            {
                armControler.getCurrentPositionSpecial(armPosition);
                armPosition.setHAngle(commandArgAngle);
                armPosition.setVAngle(0);
                armPosition.setPlierPos(ARM_MAX_PLIER_POS);
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            break;
        case 1:
        {
            armControler.getCurrentPositionSpecial(armPosition);
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
        }
        case 2:
            waitForMoveCompletion();
            break;
        case 3:
            // Rotation horizontale
            armControler.getCurrentPositionSpecial(armPosition);
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
                armControler.getCurrentPositionSpecial(armPosition);
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
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setVAngle(ARM_V_ANGLE_STAGE_0_DOWN);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            // Positionnement de la tête du bras
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 9:
            waitForMoveCompletion();
            break;
        case 10:
            // Fermeture de la pince
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 11:
            waitForMoveCompletion();
            break;
        case 12:
            // Levée du cube
            armControler.getCurrentPositionSpecial(armPosition);
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
            // Levée du cube
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_HEAD_L_ANGLE_TRANSPORT);
            armPosition.setVAngle(ARM_V_ANGLE_STORAGE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            Serial.println("CASE 0");
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Placement au dessus de la zone de stockage
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            Serial.println("CASE 2");
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Dépose du cube
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(ARM_HEAD_G_ANGLE_STORAGE);
            armPosition.setPlierPos(25);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            Serial.println("CASE 4");
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Rangement du bras
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
            armPosition.setVAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            Serial.println("CASE 6");
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
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHAngle(ARM_H_ANGLE_MANIP);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Levée du cube
            armControler.getCurrentPositionSpecial(armPosition);
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
            armControler.getCurrentPositionSpecial(armPosition);
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
            // Levée du bras
            armControler.getCurrentPositionSpecial(armPosition);
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
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Abaissement de la tête du bras
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(ARM_HEAD_G_ANGLE_STORAGE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Préhension du cube
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            armControler.getCurrentPositionSpecial(armPosition);
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
            if ((abs(commandArgAngle) < ARM_H_ANGLE_MANIP && commandArgHeight < 2) ||
                (abs(commandArgAngle) >= ARM_H_ANGLE_MANIP && commandArgHeight > 2) ||
                (abs(commandArgAngle) > ARM_MAX_H_ANGLE))
            {
                Serial.println("put_cube_smart: Invalid arguments");
                status |= ARM_STATUS_UNREACHABLE;
                stopCommand();
            }
            armControler.getCurrentPositionSpecial(armPosition);
            switch (commandArgHeight)
            {
            case 0:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_0_UP);
                break;
            case 1:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_1_UP);
                break;
            case 2:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_2_UP);
                break;
            case 3:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_3_UP);
                break;
            case 4:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_4_UP);
                break;
            default:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_2_UP);
                break;
            }
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            armControler.getCurrentPositionSpecial(armPosition);
            if (abs(commandArgAngle) > ARM_H_ANGLE_MANIP)
            {
                armPosition.setHAngle(commandArgAngle);
                currentCommandStep = 7;
            }
            else if (armPosition.getHeadGlobalAngle() > 0)
            {
                armPosition.setHAngle(commandArgAngle);
                armPosition.setHeadGlobalAngle(HALF_PI);
                currentCommandStep++;
            }
            else if (commandArgAngle >= 0)
            {
                armPosition.setHAngle(ARM_H_ANGLE_MANIP + 0.17); // +10° pour être sûr
                currentCommandStep++;
            }
            else
            {
                armPosition.setHAngle(-ARM_H_ANGLE_MANIP - 0.17); // +10° pour être sûr
                currentCommandStep++;
            }
            armControler.setAimPosition(armPosition);
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(HALF_PI);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHAngle(commandArgAngle);
            armControler.setAimPosition(armPosition);
            currentCommandStep = 8;
            break;
        case 7:
            armControler.getCurrentPositionSpecial(armPosition);
            if (abs(armPosition.getHAngle()) > ARM_H_ANGLE_MANIP)
            {
                armControler.getAimPosition(armPosition);
                armPosition.setHeadGlobalAngle(0);
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            break;
        case 8:
            waitForMoveCompletion();
            break;
        case 9:
            armControler.getCurrentPositionSpecial(armPosition);
            switch (commandArgHeight)
            {
            case 0:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_0_DOWN);
                break;
            case 1:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_1_DOWN);
                break;
            case 2:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_2_DOWN);
                break;
            case 3:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_3_DOWN);
                break;
            case 4:
                armPosition.setVAngle(ARM_V_ANGLE_STAGE_4_DOWN);
                break;
            default:
                break;
            }
            if (abs(commandArgAngle) > ARM_H_ANGLE_MANIP)
            {
                armPosition.setHeadGlobalAngle(0);
            }
            else
            {
                armPosition.setHeadGlobalAngle(HALF_PI);
            }
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 10:
            waitForMoveCompletion();
            break;
        case 11:
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MAX_PLIER_POS);
            armControler.setAimPosition(armPosition);
            break;
        case 12:
            waitForMoveCompletion();
            break;
        case 13:
            armControler.getCurrentPositionSpecial(armPosition);
            if (abs(commandArgAngle) > ARM_H_ANGLE_MANIP)
            {
                armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
            }
            else
            {
                armPosition.setHeadLocalAngle(ARM_MIN_HEAD_ANGLE);
            }
            armControler.setAimPosition(armPosition);
            break;
        case 14:
            waitForMoveCompletion();
            break;
        case 15:
            stopCommand();
            break;
        default:
            Serial.println("Err unhandled command step");
            stopCommand();
            break;
        }
    }

    void set_arm_position()
    {
        switch (currentCommandStep)
        {
        case 0:
            waitForMoveCompletion();
            break;
        case 1:
            stopCommand();
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
