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
            // todo: handle message
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

    bool newSensorsAngle() const { return anglesAvailable; }
    float getLeftSensorAngle() const { return leftSensorAngle; }
    float getRightSensorAngle() const { return rightSensorAngle; }
    bool newScoreAvailable() const { return scoreAvailable; }
    int32_t getScore() const { return score; }

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
