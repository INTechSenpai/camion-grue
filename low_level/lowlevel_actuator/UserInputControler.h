#ifndef _USER_INPUT_CONTROLER_h
#define _USER_INPUT_CONTROLER_h

#include "ArmController.h"
#include "ButtonReader.h"
#include "Adafruit_LEDBackpack.h"

#define USER_INPUT_UPDATE_PERIOD    20  // ms

#define SLOW_MOVE_PWM   800
#define FAST_MOVE_PWM   1023
#define SLOW_MOVE_INC   4
#define FAST_MOVE_INC   4


class UserInputControler
{
public:
    UserInputControler() :
        armControler(ArmController::Instance())
    {
        currentScore = 0;
        lastUpdateTime = 0;
        currentMode = NONE;
		headContinuousInc = 0;
		plierContinuousInc = 0;
		displayingScore = false;
		scoreDisplayTimer = 0;
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
				headContinuousInc = 0;
				plierContinuousInc = 0;
				armControler.setManualMode(false);
                break;
            case FORWARD:
				if (currentMode != NONE)
				{
					armControler.setManualMode(true);
				}
                switch (currentMode)
                {
                case UserInputControler::HORIZONTAL:
                    armControler.setHPWM(-SLOW_MOVE_PWM);
                    break;
                case UserInputControler::VERTICAL:
                    armControler.setVPWM(SLOW_MOVE_PWM);
                    break;
                case UserInputControler::HEAD:
                    armControler.incrManualHeadAngle(-SLOW_MOVE_INC);
					headContinuousInc = 0;
                    break;
                case UserInputControler::PLIER:
                    armControler.incrManualPlierAngle(SLOW_MOVE_INC);
					plierContinuousInc = 0;
                    break;
                default:
                    break;
                }
                break;
            case FAST_FORWARD:
				if (currentMode != NONE)
				{
					armControler.setManualMode(true);
				}
                switch (currentMode)
                {
                case UserInputControler::HORIZONTAL:
                    armControler.setHPWM(-FAST_MOVE_PWM);
                    break;
                case UserInputControler::VERTICAL:
                    armControler.setVPWM(FAST_MOVE_PWM);
                    break;
                case UserInputControler::HEAD:
					headContinuousInc = -FAST_MOVE_INC;
                    break;
                case UserInputControler::PLIER:
					plierContinuousInc = FAST_MOVE_INC;
                    break;
                default:
                    break;
                }
                break;
            case BACKWARD:
				if (currentMode != NONE)
				{
					armControler.setManualMode(true);
				}
                switch (currentMode)
                {
                case UserInputControler::HORIZONTAL:
                    armControler.setHPWM(SLOW_MOVE_PWM);
                    break;
                case UserInputControler::VERTICAL:
                    armControler.setVPWM(-SLOW_MOVE_PWM);
                    break;
                case UserInputControler::HEAD:
                    armControler.incrManualHeadAngle(SLOW_MOVE_INC);
					headContinuousInc = 0;
                    break;
                case UserInputControler::PLIER:
                    armControler.incrManualPlierAngle(-SLOW_MOVE_INC);
					plierContinuousInc = 0;
                    break;
                default:
                    break;
                }
                break;
            case FAST_BACKWARD:
				if (currentMode != NONE)
				{
					armControler.setManualMode(true);
				}
                switch (currentMode)
                {
                case UserInputControler::HORIZONTAL:
                    armControler.setHPWM(FAST_MOVE_PWM);
                    break;
                case UserInputControler::VERTICAL:
                    armControler.setVPWM(-FAST_MOVE_PWM);
                    break;
                case UserInputControler::HEAD:
					headContinuousInc = FAST_MOVE_INC;
                    break;
                case UserInputControler::PLIER:
					plierContinuousInc = -FAST_MOVE_INC;
                    break;
                default:
                    break;
                }
                break;
            case MODE_UP:
                currentMode = (ControlMode)(((int)currentMode + 1) % 5);
				displayingScore = true;
				scoreDisplayTimer = 0;
				Serial.print("Mode=");
				Serial.println(currentMode);
                break;
            case MODE_DOWN:
                currentMode = (ControlMode)(((int)currentMode + 4) % 5);
				displayingScore = true;
				scoreDisplayTimer = 0;
				Serial.print("Mode=");
				Serial.println(currentMode);
                break;
            case RESET:
				armControler.resetToOrigin();
				scoreDisplay.println(8888);
				scoreDisplay.writeDisplay();
				digitalWrite(PIN_DEL_GYRO_G, HIGH);
				digitalWrite(PIN_DEL_GYRO_D, HIGH);
				delay(500);
				digitalWrite(PIN_DEL_GYRO_G, LOW);
				digitalWrite(PIN_DEL_GYRO_D, LOW);
				delay(500);
				digitalWrite(PIN_DEL_GYRO_G, HIGH);
				digitalWrite(PIN_DEL_GYRO_D, HIGH);
				delay(500);
				digitalWrite(PIN_DEL_GYRO_G, LOW);
				digitalWrite(PIN_DEL_GYRO_D, LOW);
				displayingScore = false;
				scoreDisplayTimer = 0;
                break;
            default:
                break;
            }

			if (headContinuousInc != 0)
			{
				armControler.incrManualHeadAngle(headContinuousInc);
			}
			if (plierContinuousInc != 0)
			{
				armControler.incrManualPlierAngle(plierContinuousInc);
			}
        }

		if (millis() - scoreDisplayTimer > 1000)
		{
			scoreDisplayTimer = millis();
			displayingScore = !displayingScore;
			scoreDisplay.clear();
			if (displayingScore)
			{
				scoreDisplay.println(currentScore);
			}
			else
			{
				switch (currentMode)
				{
				case UserInputControler::NONE:
					scoreDisplay.println(currentScore);
					break;
				case UserInputControler::HORIZONTAL:
					scoreDisplay.writeDigitRaw(4, 0x40);
					scoreDisplay.writeDigitRaw(3, 0x40);
					scoreDisplay.writeDigitRaw(1, 0x40);
					scoreDisplay.writeDigitRaw(0, 0x40);
					break;
				case UserInputControler::VERTICAL:
					scoreDisplay.writeDigitRaw(4, 0x30);
					scoreDisplay.writeDigitRaw(3, 0x30);
					scoreDisplay.writeDigitRaw(1, 0x30);
					scoreDisplay.writeDigitRaw(0, 0x30);
					break;
				case UserInputControler::HEAD:
					scoreDisplay.writeDigitRaw(4, 0x0F);
					scoreDisplay.writeDigitRaw(3, 0x09);
					scoreDisplay.writeDigitRaw(1, 0x00);
					scoreDisplay.writeDigitRaw(0, 0x00);
					break;
				case UserInputControler::PLIER:
					scoreDisplay.writeDigitRaw(4, 0x46);
					scoreDisplay.writeDigitRaw(3, 0x40);
					scoreDisplay.writeDigitRaw(1, 0x40);
					scoreDisplay.writeDigitRaw(0, 0x70);
					break;
				default:
					break;
				}
			}
			scoreDisplay.writeDisplay();
		}
    }

    void setScore(int32_t score)
    {
        currentScore = score;
		scoreDisplayTimer = 0;
		displayingScore = false;
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

	float headContinuousInc;
	float plierContinuousInc;

	bool displayingScore;
	uint32_t scoreDisplayTimer;
};

#endif
