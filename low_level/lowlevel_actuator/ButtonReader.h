#ifndef _BUTTON_READER_h
#define _BUTTON_READER_h

#include "config.h"

/* Seuils de différenciation des boutons analogiques */
#define THRESHOLD_1 164 // under: >>
#define THRESHOLD_2 512 // under: >
#define THRESHOLD_3 859 // under: O

#define RESET_PRESS_DURATION    2000    // ms
#define DEBOUNCE_DURATION       100     // ms


enum ButtonSignal
{
    NONE,
    STOP,
    FORWARD,
    FAST_FORWARD,
    BACKWARD,
    FAST_BACKWARD,
    MODE_UP,
    MODE_DOWN,
    RESET
};


class ButtonReader
{
public:
    ButtonReader()
    {
        signal = NONE;
        currentState = STOP;
        lastState = STOP;
        resetPressed = false;
        resetTimer = 0;
        disableModeButtons = false;
        debounceTimer = 0;
    }

    void update()
    {
        int buttonR = interpretAnalog(analogRead(PIN_BUTTON_GROUP_1));
        int buttonF = interpretAnalog(analogRead(PIN_BUTTON_GROUP_2));
        currentState = STOP;
        if (buttonF == 1 && buttonR == 1)
        {
            currentState = RESET;
        }
        else if (buttonR == 0)
        {
            if (buttonF == 3)
            {
                currentState = FAST_FORWARD;
            }
            else if (buttonF == 2)
            {
                currentState = FORWARD;
            }
            else if (buttonF == 1)
            {
                currentState = MODE_UP;
            }
        }
        else if (buttonF == 0)
        {
            if (buttonR == 3)
            {
                currentState = FAST_BACKWARD;
            }
            else if (buttonR == 2)
            {
                currentState = BACKWARD;
            }
            else if (buttonR == 1)
            {
                currentState = MODE_DOWN;
            }
        }

        if (currentState != lastState)
        {
            if (triggerOnRelease(currentState))
            {
                debounceTimer = millis();
            }
            if (currentState == STOP)
            {
                if (triggerOnRelease(lastState) && !disableModeButtons && millis() - debounceTimer > DEBOUNCE_DURATION)
                {
                    signal = lastState;
                }
                else
                {
                    signal = STOP;
                    disableModeButtons = false;
                }
            }
            else if (currentState == RESET)
            {
                resetPressed = true;
                resetTimer = millis();
                disableModeButtons = true;
            }
            else if (triggerOnPress(currentState))
            {
                signal = currentState;
            }
            lastState = currentState;
        }

        if (resetPressed)
        {
            if (currentState != RESET)
            {
                resetPressed = false;
            }
            else if (millis() - resetTimer > RESET_PRESS_DURATION)
            {
                resetPressed = false;
                signal = RESET;
            }
        }
    }

    ButtonSignal getSignal()
    {
        ButtonSignal ret = signal;
        signal = NONE;
        return ret;
    }

private:
    int interpretAnalog(int analogValue)
    {
        if (analogValue < THRESHOLD_1)
        {
            return 3;
        }
        else if (analogValue < THRESHOLD_2)
        {
            return 2;
        }
        else if (analogValue < THRESHOLD_3)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    bool triggerOnPress(ButtonSignal bs)
    {
        return (bs == FAST_FORWARD || bs == FORWARD) || (bs == FAST_BACKWARD || bs == BACKWARD);
    }

    bool triggerOnRelease(ButtonSignal bs)
    {
        return bs == MODE_UP || bs == MODE_DOWN;
    }

    ButtonSignal signal;
    ButtonSignal currentState, lastState;
    bool resetPressed;
    uint32_t resetTimer;
    bool disableModeButtons;
    uint32_t debounceTimer;
};


#endif
