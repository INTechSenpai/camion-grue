#ifndef _MOTOR_SENSOR_h
#define _MOTOR_SENSOR_h

#include "Encoder.h"
#include "config.h"
#include "Average.h"


class MotorSensor
{
public:
    MotorSensor(
        float freqAsserv,
        float tickToOutputUnit,
        uint8_t pin_a,
        uint8_t pin_b,
        volatile float & motorSpeed
    ):
        freqAsserv(freqAsserv),
        tickToOutputUnit(tickToOutputUnit),
        encoder(pin_a, pin_b),
        motorSpeed(motorSpeed)
    {
        reset();
    }

    void compute()
    {
        motorTicks = encoder.read();                        // Récupération des données de l'encodeur        
        deltaMotorTicks = motorTicks - previousMotorTicks;  // Calcul du mouvement de la roue depuis le dernier asservissement
        previousMotorTicks = motorTicks;
        motorSpeed = (float)deltaMotorTicks * freqAsserv * tickToOutputUnit;    // Mise à jour de la vitesse
        averageSpeed.add(motorSpeed);
        motorSpeed = averageSpeed.value();
    }

    float getPosition()
    {
        return (float)motorTicks * tickToOutputUnit;
    }

    void setPosition(float position)
    {
        int32_t ticks = (int32_t)(position / tickToOutputUnit);
        encoder.write(ticks);
        motorTicks = ticks;
        previousMotorTicks = ticks;
        deltaMotorTicks = 0;
    }

    void reset()
    {
        encoder.write(0);
        motorTicks = 0;
        previousMotorTicks = 0;
        deltaMotorTicks = 0;
    }

private:
    const float freqAsserv;	// Fréquence d'appel de la méthode 'compute'. Utilisée pour le calcul des vitesses.
    const float tickToOutputUnit;   // Constante permettant la conversion des ticks en l'unitée voulue U (unité: U/tick)
    Encoder encoder;
    volatile float & motorSpeed;    // Vitesse instantanée moyennée du moteur. Unité : U/s (U étant l'unité choisie par l'utilisateur)
    int32_t motorTicks;
    int32_t previousMotorTicks;
    int32_t deltaMotorTicks;
    Average<float, AVERAGE_SPEED_SIZE> averageSpeed;
};


#endif
