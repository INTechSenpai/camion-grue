#ifndef _SLAVE_ACTUATOR_h
#define _SLAVE_ACTUATOR_h

#include "InternalCom.h"
#include "../Config/serial_config.h"
#include "../CommunicationServer/Serializer.h"
#include "../Tools/Singleton.h"
#include <vector>

#define ID_FRAME_ACTUATOR_ACK                   0x02
#define ID_FRAME_ACTUATOR_BOARD_REPORT          0x03
#define ID_FRAME_ACTUATOR_GO_TO_HOME            0x04
#define ID_FRAME_ACTUATOR_TAKE_CUBE_SMART       0x05
#define ID_FRAME_ACTUATOR_TAKE_CUBE_FIXED       0x06
#define ID_FRAME_ACTUATOR_STORE_CUBE_INSIDE     0x07
#define ID_FRAME_ACTUATOR_STORE_CUBE_TOP        0x08
#define ID_FRAME_ACTUATOR_TAKE_CUBE_STORAGE     0x09
#define ID_FRAME_ACTUATOR_PUT_CUBE_SMART        0x0A
#define ID_FRAME_ACTUATOR_PUT_CUBE_FIXED        0x0B
#define ID_FRAME_ACTUATOR_SET_ARM_POSITION      0x0C
#define ID_FRAME_ACTUATOR_SET_SENSORS_ANGLES    0x0D
#define ID_FRAME_ACTUATOR_SET_SCORE             0x0E
#define ID_FRAME_ACTUATOR_STOP                  0x0F

#define ACTUATOR_STATUS_OK                      0x0000
#define ACTUATOR_STATUS_COM_ERR                 0x0100
#define ACTUATOR_STATUS_CUBE_MISSED             0x0200

#define ACTUATOR_COM_TIMEOUT                    20      // ms
#define MOVING_LOCK_DURATION                    100     // ms


class SlaveActuator : public Singleton<SlaveActuator>
{
public:
    SlaveActuator() :
        internalCom(SERIAL_ACTUATOR, SERIAL_ACTUATOR_BAUDRATE)
    {
        dataAvailable = false;
        moving = false;
        status = 0;
        cubeInPlier = false;
        angleH = 0;
        angleV = 0;
        angleHead = 0;
        posPlier = 0;
        ackReceived = false;
        movingLockTimer = 0;
    }

    void listen()
    {
        internalCom.listen();
        if (internalCom.available())
        {
            lastMessage = internalCom.getMessage();
            if (lastMessage.getInstruction() == ID_FRAME_ACTUATOR_ACK)
            {
                ackReceived = true;
                return;
            }
            if (lastMessage.getInstruction() != ID_FRAME_ACTUATOR_BOARD_REPORT)
            {
                return;
            }
            if (lastMessage.size() != 38)
            {
                return;
            }

            size_t index = 0;
            bool newMovingState = Serializer::readBool(lastMessage.getPayload(), index);
            if (millis() - movingLockTimer > MOVING_LOCK_DURATION)
            {
                moving = newMovingState;
            }

            status = Serializer::readInt(lastMessage.getPayload(), index);
            cubeInPlier = Serializer::readBool(lastMessage.getPayload(), index);
            int32_t tof_g = Serializer::readInt(lastMessage.getPayload(), index);
            int32_t tof_d = Serializer::readInt(lastMessage.getPayload(), index);
            float angle_g = Serializer::readFloat(lastMessage.getPayload(), index);
            float angle_d = Serializer::readFloat(lastMessage.getPayload(), index);
            angleH = Serializer::readFloat(lastMessage.getPayload(), index);
            angleV = Serializer::readFloat(lastMessage.getPayload(), index);
            angleHead = Serializer::readFloat(lastMessage.getPayload(), index);
            posPlier = Serializer::readFloat(lastMessage.getPayload(), index);

            sensorsData.clear();
            Serializer::writeInt(tof_g, sensorsData);
            Serializer::writeInt(tof_d, sensorsData);
            Serializer::writeFloat(angle_g, sensorsData);
            Serializer::writeFloat(angle_d, sensorsData);
            Serializer::writeFloat(angleH, sensorsData);

            dataAvailable = true;
        }
    }

    bool sensorDataAvailable()
    {
        return dataAvailable;
    }

    void getSensorsValues(std::vector<uint8_t> & output)
    {
        output = sensorsData;
        dataAvailable = false;
    }

    void getArmPosition(float &hAngle, float &vAngle, float &headAngle, float &plierPos) const
    {
        hAngle = angleH;
        vAngle = angleV;
        headAngle = angleHead;
        plierPos = posPlier;
    }

    bool isMoving()
    {
        return moving;
    }

    int32_t getStatus()
    {
        return status;
    }

    bool isCubeInPlier()
    {
        return cubeInPlier;
    }

    void goToHome()
    {
        std::vector<uint8_t> payload;
        sendMoveCommand(ID_FRAME_ACTUATOR_GO_TO_HOME, payload);
    }

    void takeCubeUsingSensor(float angle)
    {
        std::vector<uint8_t> payload;
        Serializer::writeFloat(angle, payload);
        sendMoveCommand(ID_FRAME_ACTUATOR_TAKE_CUBE_SMART, payload);
    }

    void takeCubeFixed(float angle)
    {
        std::vector<uint8_t> payload;
        Serializer::writeFloat(angle, payload);
        sendMoveCommand(ID_FRAME_ACTUATOR_TAKE_CUBE_FIXED, payload);
    }

    void storeCubeInside()
    {
        std::vector<uint8_t> payload;
        sendMoveCommand(ID_FRAME_ACTUATOR_STORE_CUBE_INSIDE, payload);
    }

    void storeCubeOnTop()
    {
        std::vector<uint8_t> payload;
        sendMoveCommand(ID_FRAME_ACTUATOR_STORE_CUBE_TOP, payload);
    }

    void takeCubeFromStorage()
    {
        std::vector<uint8_t> payload;
        sendMoveCommand(ID_FRAME_ACTUATOR_TAKE_CUBE_STORAGE, payload);
    }

    void putCubeUsingSensor(float angle, int32_t floor)
    {
        std::vector<uint8_t> payload;
        Serializer::writeFloat(angle, payload);
        Serializer::writeInt(floor, payload);
        sendMoveCommand(ID_FRAME_ACTUATOR_PUT_CUBE_SMART, payload);
    }

    void putCubeFixed(float angle, int32_t floor)
    {
        std::vector<uint8_t> payload;
        Serializer::writeFloat(angle, payload);
        Serializer::writeInt(floor, payload);
        sendMoveCommand(ID_FRAME_ACTUATOR_PUT_CUBE_FIXED, payload);
    }

    void goToPosition(float hAngle, float vAngle, float headAngle, float plierPos)
    {
        std::vector<uint8_t> payload;
        Serializer::writeFloat(hAngle, payload);
        Serializer::writeFloat(vAngle, payload);
        Serializer::writeFloat(headAngle, payload);
        Serializer::writeFloat(plierPos, payload);
        sendMoveCommand(ID_FRAME_ACTUATOR_SET_ARM_POSITION, payload);
    }

    void setSensorsAngles(float leftAngle, float rightAngle)
    {
        std::vector<uint8_t> payload;
        Serializer::writeFloat(leftAngle, payload);
        Serializer::writeFloat(rightAngle, payload);
        sendCommand(ID_FRAME_ACTUATOR_SET_SENSORS_ANGLES, payload);
    }

    void setDisplayedScore(int32_t score)
    {
        std::vector<uint8_t> payload;
        Serializer::writeInt(score, payload);
        sendCommand(ID_FRAME_ACTUATOR_SET_SCORE, payload);
    }

    void stop()
    {
        std::vector<uint8_t> payload;
        sendCommand(ID_FRAME_ACTUATOR_STOP, payload);
    }

private:
    void waitForAck()
    {
        uint32_t startWaitTime = millis();
        while (!ackReceived)
        {
            listen();
            if (millis() - startWaitTime > ACTUATOR_COM_TIMEOUT)
            {
                status |= ACTUATOR_STATUS_COM_ERR;
                return;
            }
        }
        moving = true;
        movingLockTimer = millis();
    }

    void sendMoveCommand(uint8_t commandId, const std::vector<uint8_t> & payload)
    {
        status &= ~(ACTUATOR_STATUS_COM_ERR);
        sendCommand(commandId, payload);
        waitForAck();
    }

    void sendCommand(uint8_t commandId, const std::vector<uint8_t> & payload)
    {
        InternalMessage message(commandId, payload);
        internalCom.sendMessage(message);
    }

    InternalCom internalCom;
    InternalMessage lastMessage;
    bool dataAvailable;
    std::vector<uint8_t> sensorsData;

    /* Etat de la grue */
    bool moving;
    int32_t status;
    bool cubeInPlier;
    float angleH;       // [rad] 0: vers l'avant ; >0: vers la gauche du robot
    float angleV;       // [rad] 0: horizontal ; >0 vers le haut
    float angleHead;    // [rad] dans le repère de l'AX12 ; MAX: replié ; MIN: déployé
    float posPlier;     // [mm]  0: pince fermée ; MAX: pince ouverte

    /* Acquittement */
    bool ackReceived;

    /* This is not a hack */
    uint32_t movingLockTimer;
};


#endif
