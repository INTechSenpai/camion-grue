#ifndef _SMART_ARM_CONTROLLER_h
#define _SMART_ARM_CONTROLLER_h

#include "ArmController.h"
#include "ArmPosition.h"
#include "ArmStatus.h"
#include "ToF_shortRange.h"
#include "config.h"
#include "CommandId.h"

#define ARM_SENSORS_UPDATE_PERIOD   20  // ms
#define CUBE_DETECT_MIN             30  // mm
#define CUBE_DETECT_MAX             90  // mm
#define CUBE_INSIDE_THRESHOLD_LOW   60  // mm
#define CUBE_INSIDE_THRESHOLD_HIGH  75  // mm
#define PILE_DETECTION_THRESHOLD    80  // mm

#define CUBE_MANIPULATION_SPEED     150 // [unité interne d'AX12]

#define FULL_MOVE_TIMEOUT           15000   // ms


class SmartArmControler
{
public:
    SmartArmControler() :
        armControler(ArmController::Instance()),
        intSensor("int", 42, PIN_EN_TOF_INT, 15, 70),
        extSensor("ext", 43, PIN_EN_TOF_EXT, 15, 200)
    {
        intSensorValue = (SensorValue)SENSOR_DEAD;
        extSensorValue = (SensorValue)SENSOR_DEAD;
        status = ARM_STATUS_OK;
        currentCommand = ACTUATOR_NO_COMMAND;
        currentCommandStep = 0;
        commandArgAngle = 0;
        commandArgHeight = 0;
        fullMoveTimer = 0;
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
            bool cubeWasInPlier = isCubeInPlier();
            sensorLastUpdateTime = now;
            intSensorValue = intSensor.getMesure();
            extSensorValue = extSensor.getMesure();
            if (intSensorValue == (SensorValue)SENSOR_DEAD || extSensorValue == (SensorValue)SENSOR_DEAD)
            {
                status |= ARM_STATUS_SENSOR_ERR;
            }
            if (!cubeWasInPlier && isCubeInPlier())
            {
                Serial.println("CUBE DETECTED IN PLIER");
                //armControler.setHeadSpeed(CUBE_MANIPULATION_SPEED);
            }
            else if (cubeWasInPlier && !isCubeInPlier())
            {
                Serial.println("CUBE LEFT PLIER");
                //armControler.setHeadSpeed(0); // unlimited speed
            }
        }
        armControler.controlServos();

        if (currentCommand != ACTUATOR_NO_COMMAND)
        {
            if (armControler.getStatus() != ARM_STATUS_OK)
            {
                Serial.print("ARM status not OK: ");
                Serial.println((int32_t)armControler.getStatus());
                stopCommand();
                return;
            }
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
            case ACTUATOR_PUSH_BUTTON:
                push_button();
                break;
            case ACTUATOR_PUSH_BEE:
                push_bee();
                break;
            case ACTUATOR_TAKE_CUBE_HUMAN:
                take_cube_from_human();
                break;
            default:
                Serial.println("SmartArmController: unknown command");
                stopCommand();
                break;
            }

            if (millis() - fullMoveTimer > FULL_MOVE_TIMEOUT)
            {
                status |= ARM_STATUS_TIMEOUT;
                stopCommand();
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

    uint8_t getCommandStep() const
    {
        return currentCommandStep;
    }

    void executeCommand(CommandId id, float angle = 0, int32_t height = 0)
    {
        if (currentCommand != ACTUATOR_NO_COMMAND)
        {
            Serial.println("SmartArmController::executeCommand command already running");
            return;
        }
        Serial.print("Run command ");
        Serial.println(id);
        status = ARM_STATUS_OK;
        armControler.resetStatus();
        currentCommand = id;
        currentCommandStep = 0;
        commandArgAngle = angle;
        commandArgHeight = height;
        fullMoveTimer = millis();
    }

    void setArmPosition(ArmPosition position)
    {
        if (currentCommand != ACTUATOR_NO_COMMAND)
        {
            Serial.println("SmartArmController::setArmPosition command already running");
            return;
        }
        Serial.println("Run command ACTUATOR_SET_ARM_POSITION");
        status = ARM_STATUS_OK;
        armControler.resetStatus();
        currentCommand = ACTUATOR_SET_ARM_POSITION;
        currentCommandStep = 0;
        commandArgAngle = 0;
        commandArgHeight = 0;
        armControler.setAimPosition(position);
        fullMoveTimer = millis();
    }

    void emergencyStop()
    {
        armControler.stop();
        stopCommand();
    }

    bool isCubeInPlier() const
    {
        static bool wasInPlier = false;
        bool ret = wasInPlier;
        if (intSensorValue == (SensorValue)SENSOR_DEAD)
        {
            ret = true;
        }
        else if (intSensorValue == (SensorValue)OBSTACLE_TOO_CLOSE)
        {
            ret = true;
        }
        else if (intSensorValue == (SensorValue)NO_OBSTACLE)
        {
            ret = false;
        }
        else if (wasInPlier && intSensorValue > CUBE_INSIDE_THRESHOLD_HIGH)
        {
            ret = false;
        }
        else if (!wasInPlier && intSensorValue < CUBE_INSIDE_THRESHOLD_LOW)
        {
            ret = true;
        }

        wasInPlier = ret;
        return ret;
    }

private:
    void stopCommand()
    {
        Serial.println("StopCommand");
        currentCommand = ACTUATOR_NO_COMMAND;
        currentCommandStep = 0;
        commandArgAngle = 0;
        commandArgHeight = 0;
        armControler.setHeadSpeed(0); // Unlimited speed
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
                if (commandArgAngle == 0)
                {
                    if (hAngle > 0)
                    {
                        armPosition.setHAngle(ARM_H_ANGLE_CLOSED_MANIP);
                    }
                    else
                    {
                        armPosition.setHAngle(-ARM_H_ANGLE_CLOSED_MANIP);
                    }
                }
                else if (commandArgAngle > 0)
                {
                    armPosition.setHAngle(ARM_H_ANGLE_CLOSED_MANIP);
                }
                else
                {
                    armPosition.setHAngle(-ARM_H_ANGLE_CLOSED_MANIP);
                }
            }
            if (armPosition.getVAngle() < ARM_V_ANGLE_ORIGIN)
            {
                armPosition.setVAngle(ARM_V_ANGLE_ORIGIN);
            }
            armPosition.setPlierPos(25);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        }
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Repli du bras avec angleH constant
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
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
            // Abaissement du bras pour toucher la cabine
            armControler.getCurrentPositionSpecial(armPosition);
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

    void take_cube_smart(bool useSensor = true)
    {
        static uint8_t extSensorMinValue = UINT8_MAX;
        static float optimalAngle = 0;

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
                if (commandArgAngle > 0)
                {
                    armPosition.setHAngle(ARM_H_ANGLE_CLOSED_MANIP);
                }
                else
                {
                    armPosition.setHAngle(-ARM_H_ANGLE_CLOSED_MANIP);
                }
                if (armPosition.getVAngle() < ARM_V_ANGLE_ORIGIN)
                {
                    armPosition.setVAngle(ARM_V_ANGLE_ORIGIN);
                }
                armPosition.setPlierPos(ARM_MIN_PLIER_POS);
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
        {
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(ARM_HEAD_SCAN_ANGLE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        }
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setVAngle(ARM_V_ANGLE_STAGE_0_DOWN);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Rotation horizontale
            if (useSensor)
            {
                armControler.getCurrentPositionSpecial(armPosition);
                if (commandArgAngle > 0)
                {
                    armPosition.setHAngle(ARM_MAX_H_ANGLE);
                }
                else
                {
                    armPosition.setHAngle(-ARM_MAX_H_ANGLE);
                }
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            else
            {
                currentCommandStep = 7;
            }
            
            break;
        case 7:
            extSensorMinValue = UINT8_MAX;
            optimalAngle = commandArgAngle;
            currentCommandStep++;
            break;
        case 8:
            // Ajustement de la rotation horizontale via capteur
            
            /* Debug start */
            //{
            //static uint8_t last_sensor_val = 0;
            //if (extSensorValue != last_sensor_val)
            //{
            //    Serial.printf("Sensor: %u\n", extSensorValue);
            //    last_sensor_val = extSensorValue;
            //}
            //}
            /* End debug */

            if (extSensorValue == (SensorValue)SENSOR_DEAD)
            {
                status |= ARM_STATUS_SENSOR_ERR;
            }
            else if (extSensorValue >= CUBE_DETECT_MIN && extSensorValue < extSensorMinValue)
            {
                if (extSensorValue <= CUBE_DETECT_MAX)
                {
                    armControler.getCurrentPositionSpecial(armPosition);
                    optimalAngle = armPosition.getHAngle();
                }
                extSensorMinValue = extSensorValue;
            }

            if (!armControler.isMoving())
            {
                if (extSensorMinValue <= CUBE_DETECT_MAX)
                {
                    commandArgAngle = optimalAngle;
                    Serial.printf("optimalAngle=%g\n", optimalAngle);
                }
                else
                {
                    status |= ARM_STATUS_NO_DETECTION;
                }
                currentCommandStep++;
            }
            break;
        case 9:
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MAX_PLIER_POS);
            armPosition.setHAngle(commandArgAngle);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 10:
            waitForMoveCompletion();
            break;
        case 11:
            // Positionnement de la tête du bras
            armControler.setHeadSpeed(CUBE_MANIPULATION_SPEED);
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 12:
            armControler.getCurrentPosition(armPosition);
            if (useSensor && isCubeInPlier())
            {
                float currentGlobalAngle = armPosition.getHeadGlobalAngle();
                Serial.printf("New head angle: %g\n", currentGlobalAngle + 0.4);
                armPosition.setHeadGlobalAngle(currentGlobalAngle + 0.4);
                armControler.setAimPosition(armPosition);
                currentCommandStep++;
            }
            else
            {
                waitForMoveCompletion();
            }
            break;
        case 13:
            waitForMoveCompletion();
            break;
        case 14:
            // Fermeture de la pince
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 15:
            waitForMoveCompletion();
            break;
        case 16:
            // Levée du cube
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_HEAD_L_ANGLE_TRANSPORT);
            armPosition.setVAngle(ARM_V_ANGLE_ORIGIN);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 17:
            waitForMoveCompletion();
            break;
        case 18:
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
            armControler.setHeadSpeed(CUBE_MANIPULATION_SPEED);
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_HEAD_L_ANGLE_TRANSPORT);
            armPosition.setVAngle(ARM_V_ANGLE_STORAGE_UP);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
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
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Dépose du cube
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setVAngle(ARM_V_ANGLE_STORAGE_DOWN);
            armPosition.setHeadGlobalAngle(ARM_HEAD_G_ANGLE_STORAGE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Lacher du cube
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(25);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            // Rangement du bras
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
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

    void store_cube_top()
    {
        switch (currentCommandStep)
        {
        case 0:
            // Position initiale
            float angleManip;
            if (isCubeInPlier())
            {
                angleManip = ARM_H_ANGLE_MANIP;
                armControler.setHeadSpeed(CUBE_MANIPULATION_SPEED);
            }
            else
            {
                angleManip = ARM_H_ANGLE_CLOSED_MANIP;
            }
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            if (armPosition.getHeadGlobalAngle() < 0.79 || abs(armPosition.getHAngle()) > ARM_H_ANGLE_CABIN)
            {
                if (commandArgAngle == 0)
                {
                    if (armPosition.getHAngle() >= 0)
                    {
                        armPosition.setHAngle(angleManip);
                    }
                    else
                    {
                        armPosition.setHAngle(-angleManip);
                    }
                }
                else if (commandArgAngle > 0)
                {
                    armPosition.setHAngle(angleManip);
                }
                else
                {
                    armPosition.setHAngle(-angleManip);
                }
            }
            if (armPosition.getVAngle() < ARM_V_ANGLE_ORIGIN)
            {
                armPosition.setVAngle(ARM_V_ANGLE_ORIGIN);
            }
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Levée du cube au dessus de la tête
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_MIN_HEAD_ANGLE);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Alignement du bras
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setVAngle(ARM_V_ANGLE_ORIGIN);
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
            // Dégagement du bras sur le côté si besoin
            armControler.getCurrentPositionSpecial(armPosition);
            if (abs(armPosition.getHAngle()) < ARM_H_ANGLE_MANIP && armPosition.getHeadGlobalAngle() > -0.1)
            {
                armPosition.setPlierPos(ARM_MIN_PLIER_POS);
                if (commandArgAngle == 0)
                {
                    if (armPosition.getHAngle() >= 0)
                    {
                        armPosition.setHAngle(ARM_H_ANGLE_CLOSED_MANIP);
                    }
                    else
                    {
                        armPosition.setHAngle(-ARM_H_ANGLE_CLOSED_MANIP);
                    }
                }
                else if (commandArgAngle > 0)
                {
                    armPosition.setHAngle(ARM_H_ANGLE_CLOSED_MANIP);
                }
                else
                {
                    armPosition.setHAngle(-ARM_H_ANGLE_CLOSED_MANIP);
                }
            }
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Levée du bras
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
            armPosition.setVAngle(ARM_V_ANGLE_STORAGE_DOWN);
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
            armPosition.setPlierPos(ARM_MAX_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Abaissement de la tête du bras
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(ARM_HEAD_G_ANGLE_STORAGE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            // Préhension du cube
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 9:
            waitForMoveCompletion();
            break;
        case 10:
            // Levée du cube
            armControler.setHeadSpeed(CUBE_MANIPULATION_SPEED);
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setVAngle(ARM_V_ANGLE_STORAGE_UP);
            armPosition.setHeadLocalAngle(ARM_HEAD_L_ANGLE_TRANSPORT);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 11:
            waitForMoveCompletion();
            break;
        case 12:
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
        static bool pileDestroyed = false;

        switch (currentCommandStep)
        {
        case 0:
            if (abs(commandArgAngle) < ARM_H_ANGLE_MANIP || (commandArgHeight > 2 || commandArgHeight < 0))
            {
                Serial.println("put_cube_smart: Invalid arguments");
                status |= ARM_STATUS_UNREACHABLE;
                stopCommand();
                return;
            }
            else if (!isCubeInPlier())
            {
                Serial.println("put_cube_smart: No cube in plier");
                status |= ARM_STATUS_NO_DETECTION;
                stopCommand();
                return;
            }
            // Placement en position de manipulation
            armControler.setHeadSpeed(CUBE_MANIPULATION_SPEED);
            armControler.getCurrentPositionSpecial(armPosition);
            if (commandArgAngle > 0)
            {
                armPosition.setHAngle(ARM_H_ANGLE_MANIP);
            }
            else
            {
                armPosition.setHAngle(-ARM_H_ANGLE_MANIP);
            }
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Positionnement du cube en mode "transport" et réglage de la hauteur
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_HEAD_L_ANGLE_TRANSPORT);
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
            default:
                break;
            }
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Positionnement à l'angle H voulu
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHAngle(commandArgAngle);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Positionnement du cube au dessus de la pile
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            // Descente du cube
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
            default:
                break;
            }
            armPosition.setHeadGlobalAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 9:
            waitForMoveCompletion();
            break;
        case 10:
            // On lache le cube
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MAX_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 11:
            waitForMoveCompletion();
            break;
        case 12:
            // On relève légèrement la pince
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
            default:
                break;
            }
            armPosition.setHeadGlobalAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 13:
            waitForMoveCompletion();
            break;
        case 14:
            pileDestroyed = intSensorValue == (SensorValue)NO_OBSTACLE || intSensorValue > PILE_DETECTION_THRESHOLD;
            currentCommandStep++;
            break;
        case 15:
            // On replie la pince
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 16:
            waitForMoveCompletion();
            break;
        case 17:
            // On referme la pince pour avoir moins de chances de tout casser ensuite
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 18:
            waitForMoveCompletion();
            break;
        case 19:
            if (pileDestroyed)
            {
                status |= ARM_STATUS_CUBE_MISSED;
            }
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


    /*
        Valeurs d'angle Vertical du bras en fonction de la distance mesurée par le capteur avant
        angle = -0.0025 * distance + 0.25
        [mm]    [rad]
        100     0
        80      0.05
        60      0.10

        version réhaussée
        angle = -0.0025 * distance + 0.35
    */
    void push_button()
    {
        switch (currentCommandStep)
        {
        case 0:
            // Décalage du bras sur le côté
            armControler.getCurrentPositionSpecial(armPosition);
            if (commandArgHeight == 0)
            {
                if (armPosition.getHAngle() >= 0)
                {
                    armPosition.setHAngle(ARM_H_ANGLE_MANIP);
                }
                else
                {
                    armPosition.setHAngle(-ARM_H_ANGLE_MANIP);
                }
            }
            else if (commandArgHeight > 0)
            {
                armPosition.setHAngle(ARM_H_ANGLE_MANIP);
            }
            else
            {
                armPosition.setHAngle(-ARM_H_ANGLE_MANIP);
            }
            armPosition.setVAngle(max(commandArgAngle, ARM_V_ANGLE_ORIGIN));
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            // Semi-déploiement de la pince
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(0.2);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            // Placement du bars en face du robot
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHAngle(0);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 5:
            waitForMoveCompletion();
            break;
        case 6:
            // Descente du bras si besoin
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setVAngle(max(commandArgAngle, 0));
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 7:
            waitForMoveCompletion();
            break;
        case 8:
            // Appui sur le bouton
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(0.8);
            armControler.setAimPosition(armPosition);
            armControler.enableHeadOverheadTimer();
            currentCommandStep++;
            break;
        case 9:
            waitForMoveCompletion();
            break;
        case 10:
            // Relache du bouton
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHeadGlobalAngle(0.2);
            armPosition.setVAngle(ARM_V_ANGLE_ORIGIN);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 11:
            waitForMoveCompletion();
            break;
        case 12:
            stopCommand();
            break;
        default:
            Serial.println("Err unhandled command step");
            stopCommand();
            break;
        }
    }

    void push_bee()
    {
        switch (currentCommandStep)
        {
        case 0:
            // dégagement du bras sur le côté opposé
            armControler.getCurrentPositionSpecial(armPosition);
            if (commandArgAngle > 0)
            {
                armPosition.setHAngle(-ARM_H_ANGLE_CLOSED_MANIP);
            }
            else
            {
                armPosition.setHAngle(ARM_H_ANGLE_CLOSED_MANIP);
            }
            armPosition.setPlierPos(ARM_MIN_PLIER_POS);
            if (armPosition.getVAngle() < ARM_V_ANGLE_ORIGIN)
            {
                armPosition.setVAngle(ARM_V_ANGLE_ORIGIN);
            }
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 1:
            waitForMoveCompletion();
            break;
        case 2:
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setVAngle(ARM_V_ANGLE_ORIGIN);
            armPosition.setHeadGlobalAngle(1.18);
            armControler.setAimPosition(armPosition);
            currentCommandStep++;
            break;
        case 3:
            waitForMoveCompletion();
            break;
        case 4:
            armControler.getCurrentPositionSpecial(armPosition);
            armPosition.setHAngle(commandArgAngle);
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

    /* TODO */
    void take_cube_from_human()
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
    uint32_t fullMoveTimer;
};


#endif
