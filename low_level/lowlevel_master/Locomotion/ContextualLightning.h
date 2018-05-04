#ifndef CONTEXTUAL_LIGHTNING_h
#define CONTEXTUAL_LIGHTNING_h

#include "MotionControlSystem.h"
#include "../SlaveCommunication/SlaveSensorLed.h"

#define CONTEXTUAL_LIGHTNING_UPDATE_PERIOD  50  // ms
#define TURNING_THRESHOLD                   0.5 // m^-1


class ContextualLightning
{
public:
    ContextualLightning() :
        motionControlSystem(MotionControlSystem::Instance()),
        slaveSensorLed(SlaveSensorLed::Instance())
    {
        movingForward = true;
        breaking = false;
        turningLeft = false;
        turningRight = false;
    }

    void update()
    {
        static uint32_t lastUpdateTime = 0;
        if (millis() - lastUpdateTime > CONTEXTUAL_LIGHTNING_UPDATE_PERIOD)
        {
            lastUpdateTime = millis();

            movingForward = motionControlSystem.isMovingForward();
            breaking = motionControlSystem.isBreaking();
            float curvature = motionControlSystem.getCurvature();
            if (curvature < -TURNING_THRESHOLD) {
                turningLeft = false;
                turningRight = true;
            }
            else if (curvature > TURNING_THRESHOLD) {
                turningLeft = true;
                turningRight = false;
            }
            else {
                turningLeft = false;
                turningRight = false;
            }
            sendUpdates();
        }
    }

private:
    void sendUpdates()
    {
        static bool wasMovingForward = true;
        static bool wasBreaking = false;
        static bool wasTurningLeft = false;
        static bool wasTurningRight = false;

        if (wasMovingForward != movingForward) {
            if (movingForward) {
                slaveSensorLed.setLightOff(SlaveSensorLed::REVERSE_LIGHT);
            }
            else {
                slaveSensorLed.setLightOn(SlaveSensorLed::REVERSE_LIGHT);
            }
            wasMovingForward = movingForward;
        }

        if (wasBreaking != breaking) {
            if (breaking) {
                slaveSensorLed.setLightOn(SlaveSensorLed::STOP_LIGHT);
            }
            else {
                slaveSensorLed.setLightOff(SlaveSensorLed::STOP_LIGHT);
            }
            wasBreaking = breaking;
        }

        if (wasTurningLeft != turningLeft) {
            if (turningLeft) {
                slaveSensorLed.setLightOn(SlaveSensorLed::TURN_LEFT);
            }
            else {
                slaveSensorLed.setLightOff(SlaveSensorLed::TURN_LEFT);
            }
            wasTurningLeft = turningLeft;
        }

        if (wasTurningRight != turningRight) {
            if (turningRight) {
                slaveSensorLed.setLightOn(SlaveSensorLed::TURN_RIGHT);
            }
            else {
                slaveSensorLed.setLightOff(SlaveSensorLed::TURN_RIGHT);
            }
            wasTurningRight = turningRight;
        }
    }

    const MotionControlSystem & motionControlSystem;
    SlaveSensorLed & slaveSensorLed;
    
    bool movingForward;
    bool breaking;
    bool turningLeft;
    bool turningRight;
};


#endif
