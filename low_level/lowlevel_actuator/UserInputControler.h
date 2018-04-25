#ifndef _USER_INPUT_CONTROLER_h
#define _USER_INPUT_CONTROLER_h

#include "ArmController.h"
#include "ButtonReader.h"
#include "Adafruit_LEDBackpack.h"

#define USER_INPUT_UPDATE_PERIOD    20  // ms

#define SLOW_MOVE_PWM   800
#define FAST_MOVE_PWM   1023
#define SLOW_MOVE_INC   1
#define FAST_MOVE_INC   5


class UserInputControler
{
public:
    UserInputControler() :
        armControler(ArmController::Instance())
    {
        currentScore = 0;
        lastUpdateTime = 0;
        currentMode = NONE;
    }

    void init()
    {
        scoreDisplay.begin(0x70);
        scoreDisplay.clear();
        scoreDisplay.println(0);
        scoreDisplay.writeDisplay();
    }

    void control()
    {
        if (millis() - lastUpdateTime > USER_INPUT_UPDATE_PERIOD)
        {
            lastUpdateTime = millis();
            buttonReader.update();
            ButtonSignal signal = buttonReader.getSignal();
            switch (signal)
            {
            case NONE:
                break;
            case STOP:
                armControler.setManualMode(false);
                break;
            case FORWARD:
                armControler.setManualMode(true);
                switch (currentMode)
                {
                case UserInputControler::HORIZONTAL:
                    armControler.setHPWM(-SLOW_MOVE_PWM);
                    break;
                case UserInputControler::VERTICAL:
                    armControler.setVPWM(-SLOW_MOVE_PWM);
                    break;
                case UserInputControler::HEAD:
                    armControler.incrManualHeadAngle(SLOW_MOVE_INC);
                    break;
                case UserInputControler::PLIER:
                    armControler.incrManualPlierAngle(SLOW_MOVE_INC);
                    break;
                default:
                    break;
                }
                break;
            case FAST_FORWARD:
                armControler.setManualMode(true);
                switch (currentMode)
                {
                case UserInputControler::HORIZONTAL:
                    armControler.setHPWM(-FAST_MOVE_PWM);
                    break;
                case UserInputControler::VERTICAL:
                    armControler.setVPWM(-FAST_MOVE_PWM);
                    break;
                case UserInputControler::HEAD:
                    armControler.incrManualHeadAngle(FAST_MOVE_INC);
                    break;
                case UserInputControler::PLIER:
                    armControler.incrManualPlierAngle(FAST_MOVE_INC);
                    break;
                default:
                    break;
                }
                break;
            case BACKWARD:
                armControler.setManualMode(true);
                switch (currentMode)
                {
                case UserInputControler::HORIZONTAL:
                    armControler.setHPWM(SLOW_MOVE_PWM);
                    break;
                case UserInputControler::VERTICAL:
                    armControler.setVPWM(SLOW_MOVE_PWM);
                    break;
                case UserInputControler::HEAD:
                    armControler.incrManualHeadAngle(-SLOW_MOVE_INC);
                    break;
                case UserInputControler::PLIER:
                    armControler.incrManualPlierAngle(-SLOW_MOVE_INC);
                    break;
                default:
                    break;
                }
                break;
            case FAST_BACKWARD:
                armControler.setManualMode(true);
                switch (currentMode)
                {
                case UserInputControler::HORIZONTAL:
                    armControler.setHPWM(FAST_MOVE_PWM);
                    break;
                case UserInputControler::VERTICAL:
                    armControler.setVPWM(FAST_MOVE_PWM);
                    break;
                case UserInputControler::HEAD:
                    armControler.incrManualHeadAngle(-FAST_MOVE_INC);
                    break;
                case UserInputControler::PLIER:
                    armControler.incrManualPlierAngle(-FAST_MOVE_INC);
                    break;
                default:
                    break;
                }
                break;
            case MODE_UP:
                currentMode = (ControlMode)(((int)currentMode + 1) % 5);
                break;
            case MODE_DOWN:
                currentMode = (ControlMode)(((int)currentMode + 4) % 5);
                break;
            case RESET:
                // todo
                break;
            default:
                break;
            }



        }
    }

    void setScore(int32_t score)
    {
        currentScore = score;
    }

private:
    enum ControlMode
    {
        NONE = 0,
        HORIZONTAL = 1,
        VERTICAL = 2,
        HEAD = 3,
        PLIER = 4
    };

    ArmController & armControler;
    int32_t currentScore;
    Adafruit_7segment scoreDisplay;
    uint32_t lastUpdateTime;
    ControlMode currentMode;
    ButtonReader buttonReader;
};

#endif
