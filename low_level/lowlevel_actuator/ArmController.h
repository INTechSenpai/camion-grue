#ifndef _ARM_CONTROLLER_h
#define _ARM_CONTROLLER_h

#include "config.h"
#include "Singleton.h"
#include "ArmPosition.h"
#include "ArmStatus.h"
#include "Motor.h"
#include "MotorSensor.h"
#include "BlockingMgr.h"
#include "PID.h"
#include "dynamixel_teensy/src/Dynamixel.h"
#include "dynamixel_teensy/src/DynamixelInterface.h"
#include "dynamixel_teensy/src/DynamixelMotor.h"

#define PLIER_MAX_TORQUE        950     // [unit� interne d'AX12]
#define PLIER_OVERLOAD_TIMEOUT  500     // [ms]
#define HEAD_OVERLOAD_TIMEOUT   300     // [ms]


class ArmController : public Singleton<ArmController>
{
public:
    ArmController() :
        hMotor(PIN_EN_MOT_H, PIN_B_MOT_H, PIN_A_MOT_H),
        hMotorSensor(FREQ_ASSERV, HMOTOR_TICK_TO_RAD, PIN_ENC_MOT_H_A, PIN_ENC_MOT_H_B, currentHSpeed),
        hMotorBlockingMgr(aimHSpeed, currentHSpeed),
        hMotorSpeedPID(currentHSpeed, hMotorPWM, aimHSpeed, FREQ_ASSERV),
        hMotorPosPID(currentHPos, aimHSpeed, aimHPos, FREQ_ASSERV),
        vMotor(PIN_EN_MOT_V, PIN_A_MOT_V, PIN_B_MOT_V),
        vMotorSensor(FREQ_ASSERV, VMOTOR_TICK_TO_MM, PIN_ENC_MOT_V_B, PIN_ENC_MOT_V_A, currentVSpeed),
        vMotorBlockingMgr(aimVSpeed, currentVSpeed),
        vMotorSpeedPID(currentVSpeed, vMotorPWM, aimVSpeed, FREQ_ASSERV),
        serialAX(SERIAL_AX12),
        headAX12(serialAX, ID_AX12_HEAD),
        plierAX12(serialAX, ID_AX12_PLIER)
    {
        hMotorBlockingMgr.setTunings(BLOCKING_SENSIBILITY, BLOCKING_DELAY, BLOCKING_SPEED_THRESHOLD);
        hMotorSpeedPID.setTunings(H_SPEED_KP, H_SPEED_KI, H_SPEED_KD);
        hMotorSpeedPID.setOutputLimits(-MAX_PWM, MAX_PWM);
        hMotorPosPID.setTunings(HMOTOR_KP, 0, HMOTOR_KD);
        hMotorPosPID.setOutputLimits(-HMOTOR_MAX_SPEED, HMOTOR_MAX_SPEED);

        vMotorBlockingMgr.setTunings(0, BLOCKING_DELAY, 0);
        vMotorSpeedPID.setTunings(V_SPEED_KP, V_SPEED_KI, V_SPEED_KD);
        vMotorSpeedPID.setOutputLimits(-MAX_PWM, MAX_PWM);

        resetToOrigin();
        aimPosition.resetAXToOrigin();
        currentPosition.resetAXToOrigin();
        stopFromInterrupt();
        status = ARM_STATUS_OK;
        moveTimer = 0;
        manualMode = false;
        plierOverloadTimer = 0;
        plierOverloadStarted = false;
        headOverloadTimer = 0;
        headOverloadStarted = false;

        serialAX.begin(SERIAL_AX12_BAUDRATE);
    }

    int init()
    {
        headAX12.init();
        headAX12.jointMode();
        headAX12.enableTorque();
        plierAX12.init();
        plierAX12.jointMode();
        plierAX12.enableTorque();
        if (headAX12.statusReturnLevel() == 2 && plierAX12.statusReturnLevel() == 2)
        {
            return 0;
        }
        else
        {
            return -1;
        }
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

        hMotorBlockingMgr.compute();
        vMotorBlockingMgr.compute();

        if (manualMode)
        {
            hMotor.run(manualHPWM);
            vMotor.run(manualVPWM);
        }
        else
        {
            if (hMotorBlockingMgr.isBlocked())
            {
                status |= ARM_STATUS_HBLOCKED;
                stopFromInterrupt();
                Serial.println("H-Motor blocked !"); // debug
            }
            if (vMotorBlockingMgr.isBlocked())
            {
                status |= ARM_STATUS_VBLOCKED;
                stopFromInterrupt();
                Serial.println("V-Motor blocked !"); // debug
            }

            if (moving && currentPosition.closeEnoughTo(aimPosition) && abs(currentHSpeed) < HMOTOR_MIN_SPEED)
            {
                stopFromInterrupt();
                Serial.println("End of move"); // debug
            }

            if (moving && millis() - moveTimer > ARM_MOVE_TIMEOUT)
            {
                status |= ARM_STATUS_TIMEOUT;
                Serial.println("End of move by timeout"); // debug
                Serial.print("Aim= ");
                Serial.println(aimPosition);
                Serial.print("Real= ");
                Serial.println(currentPosition);
                Serial.print("H-speed= ");
                Serial.println(currentHSpeed);
                stopFromInterrupt();
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
                float globalAim = aimPosition.getHeadGlobalAngle();
                float globalReal = currentPosition.getHeadGlobalAngle();
                float horizontalReal = currentPosition.getHAngle();
                interrupts();
                if (globalAim > -0.2 || globalReal < -0.2 || abs(horizontalReal) > ARM_H_ANGLE_MANIP - 0.2)
                {
                    lastStatus = headAX12.goalPositionDegree(p);
                }
                else
                {
                    lastStatus = headAX12.goalPositionDegree(190);
                }
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
            else if (counter == 4)
            { // Get torque AX12 plier
                uint16_t torque = 0;
                lastStatus = plierAX12.read(0x28, torque);
                torque &= 1023;

                if (!plierOverloadStarted && torque > PLIER_MAX_TORQUE)
                {
                    plierOverloadStarted = true;
                    plierOverloadTimer = millis();
                }
                else if (plierOverloadStarted && torque <= PLIER_MAX_TORQUE)
                {
                    plierOverloadStarted = false;
                }
                else if (plierOverloadStarted && millis() - plierOverloadTimer > PLIER_OVERLOAD_TIMEOUT)
                {
                    plierOverloadStarted = false;
                    noInterrupts();
                    aimPosition.setPlierAngle(currentPosition.getPlierAngle());
                    uint16_t p = (uint16_t)aimPosition.getPlierAngleDeg();
                    interrupts();
                    lastStatus = plierAX12.goalPositionDegree(p);

                    Serial.println("STOPPED PLIER ON POSITION");
                }
                counter = 5;
            }
            else
            { // Get torque AX12 head
                uint16_t torque = 0;
                lastStatus = headAX12.read(0x28, torque);
                torque &= 1023;
                if (!headOverloadStarted && torque > PLIER_MAX_TORQUE)
                {
                    headOverloadStarted = true;
                    headOverloadTimer = millis();
                }
                else if (headOverloadStarted && torque <= PLIER_MAX_TORQUE)
                {
                    headOverloadStarted = false;
                }
                else if (headOverloadStarted && millis() - headOverloadTimer > HEAD_OVERLOAD_TIMEOUT)
                {
                    headOverloadStarted = false;
                    noInterrupts();
                    aimPosition.setHeadLocalAngle(currentPosition.getHeadLocalAngle());
                    uint16_t p = (uint16_t)aimPosition.getHeadLocalAngleDeg();
                    interrupts();
                    lastStatus = headAX12.goalPositionDegree(p);

                    Serial.println("STOPPED HEAD ON POSITION");
                }
                counter = 0;
            }


            if (lastStatus == DYN_STATUS_OVERHEATING_ERROR || lastStatus == DYN_STATUS_OVERLOAD_ERROR)
            {
                noInterrupts();
                status |= ARM_STATUS_AXBLOCKED;
                interrupts();
                Serial.printf("AX12 OVERLOAD: errno %u c=%u\n", lastStatus, counter);
                headAX12.recoverTorque();
                plierAX12.recoverTorque();
            }
            else if (lastStatus != DYN_STATUS_OK)
            {
                noInterrupts();
                status |= ARM_STATUS_AXERR;
                interrupts();
                Serial.printf("AX12 NOT OK: errno %u c=%u\n", lastStatus, counter);
            }
        }
    }

    void setAimPosition(const ArmPosition & position)
    {
        noInterrupts();
        aimPosition = position;
        moving = true;
        moveTimer = millis();
        status = ARM_STATUS_OK;
        interrupts();
        Serial.println("Start moving");
    }

    void getAimPosition(ArmPosition & position) const
    {
        noInterrupts();
        position = aimPosition;
        interrupts();
    }

    void getCurrentPosition(ArmPosition & position) const
    {
        noInterrupts();
        position = currentPosition;
        interrupts();
    }

    void getCurrentPositionSpecial(ArmPosition & position) const
    {
        noInterrupts();
        position = currentPosition;
        position.setHeadLocalAngle(aimPosition.getHeadLocalAngle());
        position.setPlierAngle(aimPosition.getPlierAngle());
        interrupts();
    }

    bool isMoving() const
    {
        noInterrupts();
        bool ret = moving;
        interrupts();
        return ret;
    }

    ArmStatus getStatus() const
    {
        noInterrupts();
        ArmStatus ret = status;
        interrupts();
        return ret;
    }

    void resetStatus()
    {
        noInterrupts();
        status = ARM_STATUS_OK;
        interrupts();
    }

    void setHeadSpeed(uint16_t speed)
    {
        headAX12.speed(speed);
    }

    void stop()
    {
        noInterrupts();
        status |= ARM_STATUS_MANUAL_STOP;
        stopFromInterrupt();
        interrupts();
    }

    void resetToOrigin()
    {
        noInterrupts();
        aimPosition.resetCCToOrigin();
        currentPosition.resetCCToOrigin();
        hMotorSensor.setPosition(currentPosition.getHAngle());
        vMotorSensor.setPosition(currentPosition.getPosMotV());
        interrupts();
    }

    void setManualMode(bool enable)
    {
        if (enable != manualMode)
        { 
            if (!enable)
            {
                hMotorSpeedPID.resetDerivativeError();
                hMotorSpeedPID.resetIntegralError();
                vMotorSpeedPID.resetDerivativeError();
                vMotorSpeedPID.resetIntegralError();
            }
            noInterrupts();
            manualMode = enable;
            interrupts();
        }
		noInterrupts();
        stopFromInterrupt();
		interrupts();
    }

    void setHPWM(int16_t pwm)
    {
        noInterrupts();
        manualHPWM = pwm;
        interrupts();
    }

    void setVPWM(int16_t pwm)
    {
        noInterrupts();
        manualVPWM = pwm;
        interrupts();
    }

    void incrManualHeadAngle(float inc)
    {
		aimPosition.setHeadLocalAngleDeg(aimPosition.getHeadLocalAngleDeg() + inc);
    }

    void incrManualPlierAngle(float inc)
    {
		aimPosition.setPlierAngleDeg(constrain(aimPosition.getPlierAngleDeg() + inc, 
			ARM_MIN_PLIER_ANGLE_DEG, ARM_MAX_PLIER_ANGLE_DEG));
    }

private:
    void stopFromInterrupt()
    {
		aimPosition.setHAngle(currentPosition.getHAngle());
		aimPosition.setVAngle(currentPosition.getVAngle());
		if (abs(aimPosition.getHeadLocalAngle() - currentPosition.getHeadLocalAngle()) > ARM_AX12_TOLERANCE)
		{
			aimPosition.setHeadLocalAngle(currentPosition.getHeadLocalAngle());
		}
		if (abs(aimPosition.getPlierAngle() - currentPosition.getPlierAngle()) > ARM_AX12_TOLERANCE)
		{
			aimPosition.setPlierAngle(currentPosition.getPlierAngle());
		}

        currentHSpeed = 0;
        aimHSpeed = 0;
        hMotorPWM = 0;
        currentHPos = currentPosition.getHAngle();
        aimHPos = currentHPos;
        currentVSpeed = 0;
        aimVSpeed = 0;
        vMotorPWM = 0;
        moving = false;
        manualHPWM = 0;
        manualVPWM = 0;
    }

    ArmPosition aimPosition;
    ArmPosition currentPosition;
    bool moving;
    ArmStatus status;
    uint32_t moveTimer;

    bool manualMode;
    int16_t manualHPWM;
    int16_t manualVPWM;

    /* Contr�le du mouvement horizontal */
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

    /* Contr�le du mouvement vertical */
    Motor vMotor;
    MotorSensor vMotorSensor;
    BlockingMgr vMotorBlockingMgr;
    PID vMotorSpeedPID;
    float currentVSpeed;
    float aimVSpeed;
    float vMotorPWM;

    /* Contr�le du mouvement de la pince */
    DynamixelInterface serialAX;
    DynamixelMotor headAX12;
    DynamixelMotor plierAX12;
    uint32_t plierOverloadTimer;
    bool plierOverloadStarted;
    uint32_t headOverloadTimer;
    bool headOverloadStarted;

};


#endif
