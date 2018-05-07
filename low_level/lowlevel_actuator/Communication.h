#ifndef _COMMUNICATION_h
#define _COMMUNICATION_h

#include "SmartArmController.h"
#include "InternalCom.h"
#include "CommandId.h"
#include "Serializer.h"
#include "config.h"
#include <vector>


class Communication
{
public:
    Communication() :
        internalCom(MASTER_SERIAL, BAUDRATE_SERIAL)
    {
        anglesAvailable = false;
        scoreAvailable = false;
        leftSensorAngle = 0;
        rightSensorAngle = 0;
        score = 0;
    }

    int init()
    {
        return smartArmControler.init();
    }

    void listenAndExecute()
    {
        smartArmControler.control();
        internalCom.listen();

        if (internalCom.available())
        {
            InternalMessage message = internalCom.getMessage();
            size_t index = 0;
            switch ((CommandId)message.getInstruction())
            {
            case ACTUATOR_ACK:
                Serial.println("Error: received ACK");
                break;
            case ACTUATOR_BOARD_REPORT:
                Serial.println("Error: received actuator report");
                break;
            case ACTUATOR_GO_TO_HOME:
                if (message.size() == 0)
                {
                    smartArmControler.executeCommand(ACTUATOR_GO_TO_HOME);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_GO_TO_HOME wrong args size");
                }
                break;
            case ACTUATOR_TAKE_CUBE_SMART:
                if (message.size() == 4)
                {
                    float angle = Serializer::readFloat(message.getPayload(), index);
                    smartArmControler.executeCommand(ACTUATOR_TAKE_CUBE_SMART, angle);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_TAKE_CUBE_SMART wrong args size");
                }
                break;
            case ACTUATOR_TAKE_CUBE_FIXED:
                if (message.size() == 4)
                {
                    float angle = Serializer::readFloat(message.getPayload(), index);
                    smartArmControler.executeCommand(ACTUATOR_TAKE_CUBE_FIXED, angle);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_TAKE_CUBE_FIXED wrong args size");
                }
                break;
            case ACTUATOR_STORE_CUBE_INSIDE:
                if (message.size() == 0)
                {
                    smartArmControler.executeCommand(ACTUATOR_STORE_CUBE_INSIDE);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_STORE_CUBE_INSIDE wrong args size");
                }
                break;
            case ACTUATOR_STORE_CUBE_TOP:
                if (message.size() == 0)
                {
                    smartArmControler.executeCommand(ACTUATOR_STORE_CUBE_TOP);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_STORE_CUBE_TOP wrong args size");
                }
                break;
            case ACTUATOR_TAKE_CUBE_STORAGE:
                if (message.size() == 0)
                {
                    smartArmControler.executeCommand(ACTUATOR_TAKE_CUBE_STORAGE);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_TAKE_CUBE_STORAGE wrong args size");
                }
                break;
            case ACTUATOR_PUT_CUBE_SMART:
                if (message.size() == 8)
                {
                    float angle = Serializer::readFloat(message.getPayload(), index);
                    int32_t floor = Serializer::readInt(message.getPayload(), index);
                    smartArmControler.executeCommand(ACTUATOR_PUT_CUBE_SMART, angle, floor);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_PUT_CUBE_SMART wrong args size");
                }
                break;
            case ACTUATOR_PUT_CUBE_FIXED:
                if (message.size() == 8)
                {
                    float angle = Serializer::readFloat(message.getPayload(), index);
                    int32_t floor = Serializer::readInt(message.getPayload(), index);
                    smartArmControler.executeCommand(ACTUATOR_PUT_CUBE_FIXED, angle, floor);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_PUT_CUBE_FIXED wrong args size");
                }
                break;
            case ACTUATOR_SET_ARM_POSITION:
                Serial.println("Launch ACTUATOR_SET_ARM_POSITION");
                if (message.size() == 16)
                {
                    ArmPosition position;
                    float angleH = Serializer::readFloat(message.getPayload(), index);
                    float angleV = Serializer::readFloat(message.getPayload(), index);
                    float angleHead = Serializer::readFloat(message.getPayload(), index);
                    float plierPos = Serializer::readFloat(message.getPayload(), index);
                    position.setHAngle(angleH);
                    position.setVAngle(angleV);
                    position.setHeadLocalAngle(angleHead);
                    position.setPlierPos(plierPos);
                    smartArmControler.setArmPosition(position);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_SET_ARM_POSITION wrong args size");
                }
                break;
            case ACTUATOR_SET_SENSORS_ANGLES:
                if (message.size() == 8)
                {
                    leftSensorAngle = Serializer::readFloat(message.getPayload(), index);
                    rightSensorAngle = Serializer::readFloat(message.getPayload(), index);
                    anglesAvailable = true;
                }
                else
                {
                    Serial.println("ACTUATOR_SET_SENSORS_ANGLES wrong args size");
                }
                break;
            case ACTUATOR_SET_SCORE:
                if (message.size() == 4)
                {
                    score = Serializer::readInt(message.getPayload(), index);
                    scoreAvailable = true;
                }
                else
                {
                    Serial.println("ACTUATOR_SET_SCORE wrong args size");
                }
                break;
            case ACTUATOR_STOP:
                smartArmControler.emergencyStop();
                break;
            case ACTUATOR_PUSH_BUTTON:
                if (message.size() == 4)
                {
                    float angle = Serializer::readFloat(message.getPayload(), index);
                    smartArmControler.executeCommand(ACTUATOR_PUSH_BUTTON, angle);
                    sendAck();
                }
                else
                {
                    Serial.println("ACTUATOR_PUSH_BUTTON wrong args size");
                }
                break;
            default:
                Serial.println("Unknown command received");
                break;
            }
        }
    }

    void sendReport(int32_t tof_g, int32_t tof_d, float angleTG, float angleTD)
    {
        std::vector<uint8_t> payload;
        bool moving = smartArmControler.isMoving();
        int32_t status = (int32_t)smartArmControler.getStatus();
        bool cubeInPlier = smartArmControler.isCubeInPlier();
        ArmPosition position;
        smartArmControler.getArmPosition(position);
        Serializer::writeBool(moving, payload);
        Serializer::writeInt(status, payload);
        Serializer::writeBool(cubeInPlier, payload);
        Serializer::writeInt(tof_g, payload);
        Serializer::writeInt(tof_d, payload);
        Serializer::writeFloat(angleTG, payload);
        Serializer::writeFloat(angleTD, payload);
        Serializer::writeFloat(position.getHAngle(), payload);
        Serializer::writeFloat(position.getVAngle(), payload);
        Serializer::writeFloat(position.getHeadLocalAngle(), payload);
        Serializer::writeFloat(position.getPlierPos(), payload);

        InternalMessage message(ACTUATOR_BOARD_REPORT, payload);
        internalCom.sendMessage(message);
    }

    void sendAck()
    {
        InternalMessage message(ACTUATOR_ACK, 0, nullptr);
        internalCom.sendMessage(message);
    }

    bool newSensorsAngle() const { return anglesAvailable; }
    float getLeftSensorAngle() { anglesAvailable = false; return leftSensorAngle; }
    float getRightSensorAngle() { anglesAvailable = false; return rightSensorAngle; }
    bool newScoreAvailable() const { return scoreAvailable; }
    int32_t getScore() { scoreAvailable = false; return score; }

private:
    SmartArmControler smartArmControler;
    InternalCom internalCom;

    bool anglesAvailable;
    bool scoreAvailable;
    float leftSensorAngle;
    float rightSensorAngle;
    int32_t score;
};


#endif
