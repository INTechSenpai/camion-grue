#ifndef _USER_INPUT_CONTROLER_h
#define _USER_INPUT_CONTROLER_h

#include "ArmController.h"
#include "Adafruit_LEDBackpack.h"

#define USER_INPUT_UPDATE_PERIOD    100  // ms


class UserInputControler
{
public:
    UserInputControler()
    {
        currentScore = 0;
        lastUpdateTime = 0;
    }

    void init()
    {
        scoreDisplay.begin(0x70);
    }

    void control()
    {
        if (millis() - lastUpdateTime > USER_INPUT_UPDATE_PERIOD)
        {
            static int c = 0;
            lastUpdateTime = millis();
            scoreDisplay.println(c);
            c++;
        }
    }

    void setScore(int32_t score)
    {
        currentScore = score;
    }

private:
    int32_t currentScore;
    Adafruit_7segment scoreDisplay;
    uint32_t lastUpdateTime;

};

#endif
