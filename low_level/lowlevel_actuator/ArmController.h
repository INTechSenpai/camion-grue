#ifndef _ARM_CONTROLLER_h
#define _ARM_CONTROLLER_h

#include "config.h"
#include "Singleton.h"
#include "ArmPosition.h"
#include "Motor.h"
#include "MotorSensor.h"
#include "BlockingMgr.h"
#include "PID.h"
#include "dynamixel_teensy/src/Dynamixel.h"
#include "dynamixel_teensy/src/DynamixelInterface.h"
#include "dynamixel_teensy/src/DynamixelMotor.h"


class ArmController : public Singleton<ArmController>
{
public:
    ArmController() :
        hMotor(PIN_EN_MOT_H, PIN_B_MOT_H, PIN_A_MOT_H),
        hMotorSensor(FREQ_ASSERV, HMOTOR_TICK_TO_RAD, PIN_ENC_MOT_H_A, PIN_ENC_MOT_H_B, currentHSpeed),
        hMotorBlockingMgr(aimHSpeed, currentHSpeed),
        hMotorSpeedPID(currentHSpeed, hMotorPWM, aimHSpeed, FREQ_ASSERV),
        hMotorPosPID(currentHPos, aimHSpeed, aimHPos, FREQ_ASSERV),
        vMotor(PIN_EN_MOT_V, PIN_B_MOT_V, PIN_A_MOT_V),
        vMotorSensor(FREQ_ASSERV, VMOTOR_TICK_TO_MM, PIN_ENC_MOT_V_A, PIN_ENC_MOT_V_B, currentVSpeed),
        vMotorBlockingMgr(aimVSpeed, currentVSpeed),
        vMotorSpeedPID(currentVSpeed, vMotorPWM, aimVSpeed, FREQ_ASSERV),
        serialAX(SERIAL_AX12),
        headAX12(serialAX, ID_AX12_HEAD),
        plierAX12(serialAX, ID_AX12_PLIER)
    {
        hMotorBlockingMgr.setTunings(BLOCKING_SENSIBILITY, BLOCKING_DELAY);
        hMotorSpeedPID.setTunings(H_SPEED_KP, H_SPEED_KI, H_SPEED_KD);
        hMotorSpeedPID.setOutputLimits(-MAX_PWM, MAX_PWM);
        hMotorPosPID.setTunings(HMOTOR_KP, 0, HMOTOR_KD);
        hMotorPosPID.setOutputLimits(-HMOTOR_MAX_SPEED, HMOTOR_MAX_SPEED);

        vMotorBlockingMgr.setTunings(BLOCKING_SENSIBILITY, BLOCKING_DELAY);
        vMotorSpeedPID.setTunings(V_SPEED_KP, V_SPEED_KI, V_SPEED_KD);
        vMotorSpeedPID.setOutputLimits(-MAX_PWM, MAX_PWM);

        resetToOrigin();
        aimPosition.resetAXToOrigin();
        currentPosition.resetAXToOrigin();
        stop();

        serialAX.begin(SERIAL_AX12_BAUDRATE);
    }

    void init()
    {
        headAX12.init();
        headAX12.jointMode();
        headAX12.enableTorque();
        plierAX12.init();
        plierAX12.jointMode();
        plierAX12.enableTorque();
    }

    /* A appeller depuis l'interruption sur timer */
    void controlCCMotors()
    {
        hMotorSensor.compute();
        vMotorSensor.compute();
        currentHPos = hMotorSensor.getPosition();
        currentPosition.setHAngle(currentHPos);
        currentPosition.setPosMotV(vMotorSensor.getPosition());
        aimHPos = aimPosition.getHAngle();
        hMotorPosPID.compute(); // update aimHSpeed
        hMotorSpeedPID.compute(); // update hMotorPWM

        if (abs(currentPosition.getPosMotV() - aimPosition.getPosMotV()) < ARM_V_TOLERANCE)
        {
            aimVSpeed = 0;
        }
        else if (currentPosition.getPosMotV() < aimPosition.getPosMotV())
        {
            aimVSpeed = VMOTOR_MAX_SPEED;
        }
        else
        {
            aimVSpeed = -VMOTOR_MAX_SPEED;
        }
        vMotorSpeedPID.compute(); // update vMotorPWM

        //static uint8_t a = 0;
        //if (a == 0) {
        //    //Serial.print(vMotorSpeedPID);
        //    //Serial.print("  ");
        //    //Serial.println(hMotorSpeedPID);

        //    Serial.println(currentPosition.getHAngle());
        //}
        //a+=4;

        hMotorBlockingMgr.compute();
        vMotorBlockingMgr.compute();
        if (hMotorBlockingMgr.isBlocked() || vMotorBlockingMgr.isBlocked())
        {
            stopFromInterrupt();
            Serial.println("Motor blocked !"); // debug
        }

        if (moving && currentPosition.closeEnoughTo(aimPosition))
        {
            stopFromInterrupt();
            Serial.println("End of move"); // debug
        }

        if (moving)
        {
            hMotor.run((int16_t)hMotorPWM);
            vMotor.run((int16_t)vMotorPWM);
        }
        else
        {
            hMotor.breakMotor();
            vMotor.breakMotor();
            hMotorSpeedPID.resetDerivativeError();
            hMotorSpeedPID.resetIntegralError();
            vMotorSpeedPID.resetDerivativeError();
            vMotorSpeedPID.resetIntegralError();
        }
    }

    /* A appeller depuis la boucle principale */
    void controlServos()
    {
        static uint8_t counter = 0;
        static uint32_t lastCallTime = 0;
        
        if (millis() - lastCallTime > AX12_CONTROL_PERIOD)
        {
            lastCallTime = millis();
            DynamixelStatus lastStatus = DYN_STATUS_OK;
            if (counter == 0)
            { // Set position AX12 head
                noInterrupts();
                uint16_t p = (uint16_t)aimPosition.getHeadLocalAngleDeg();
                interrupts();
                lastStatus = headAX12.goalPositionDegree(p);
                counter = 1;
            }
            else if (counter == 1)
            { // Set position AX12 plier
                noInterrupts();
                uint16_t p = (uint16_t)aimPosition.getPlierAngleDeg();
                interrupts();
                lastStatus = plierAX12.goalPositionDegree(p);
                counter = 2;
            }
            else if (counter == 2)
            { // Get position AX12 head
                uint16_t angle;
                lastStatus = headAX12.currentPositionDegree(angle);
                if (angle <= 300)
                {
                    noInterrupts();
                    currentPosition.setHeadLocalAngleDeg((float)angle);
                    interrupts();
                }
                counter = 3;
            }
            else if (counter == 3)
            { // Get position AX12 plier
                uint16_t angle;
                lastStatus = plierAX12.currentPositionDegree(angle);
                if (angle < 300)
                {
                    noInterrupts();
                    currentPosition.setPlierAngleDeg((float)angle);
                    interrupts();
                }
                counter = 4;
            }
            else
            { // Get torque AX12 plier
                uint16_t torque = 0;
                lastStatus = plierAX12.read(0x28, torque);
                torque &= 1023;
                // todo

                counter = 0;
            }

            if (lastStatus != DYN_STATUS_OK)
            {
                Serial.printf("AX12 NOT OK: errno %u c=%u\n", lastStatus, counter);
            }
        }
    }

    void setAimPosition(const ArmPosition & position)
    {
        noInterrupts();
        aimPosition = position;
        moving = true;
        interrupts();
        Serial.println("Start moving");
    }

    void getCurrentPosition(ArmPosition & position) const
    {
        noInterrupts();
        position = currentPosition;
        interrupts();
    }

    bool isMoving() const
    {
        noInterrupts();
        bool ret = moving;
        interrupts();
        return ret;
    }

    void stop()
    {
        noInterrupts();
        stopFromInterrupt();
        interrupts();
    }

    void resetToOrigin()
    {
        noInterrupts();
        aimPosition.resetCCToOrigin();
        currentPosition.resetCCToOrigin();
        currentHPos = currentPosition.getHAngle();
        hMotorSensor.setPosition(currentHPos);
        vMotorSensor.setPosition(currentPosition.getVAngle());
        interrupts();
    }

private:
    void stopFromInterrupt()
    {
        aimPosition = currentPosition;
        currentHSpeed = 0;
        aimHSpeed = 0;
        hMotorPWM = 0;
        currentHPos = currentPosition.getHAngle();
        aimHPos = currentHPos;
        currentVSpeed = 0;
        aimVSpeed = 0;
        vMotorPWM = 0;
        moving = false;
        Serial.println("Stop");
    }

    ArmPosition aimPosition;
    ArmPosition currentPosition;
    bool moving;

    /* Contrôle du mouvement horizontal */
    Motor hMotor;
    MotorSensor hMotorSensor;
    BlockingMgr hMotorBlockingMgr;
    PID hMotorSpeedPID;
    PID hMotorPosPID;
    float currentHSpeed;
    float aimHSpeed;
    float hMotorPWM;
    float currentHPos;
    float aimHPos;

    /* Contrôle du mouvement vertical */
    Motor vMotor;
    MotorSensor vMotorSensor;
    BlockingMgr vMotorBlockingMgr;
    PID vMotorSpeedPID;
    float currentVSpeed;
    float aimVSpeed;
    float vMotorPWM;

    /* Contrôle du mouvement de la pince */
    DynamixelInterface serialAX;
    DynamixelMotor headAX12;
    DynamixelMotor plierAX12;

};


#endif
