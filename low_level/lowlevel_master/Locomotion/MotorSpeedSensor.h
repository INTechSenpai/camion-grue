#ifndef _MOTOR_SPEED_SENSOR_h
#define _MOTOR_SPEED_SENSOR_h

#include "Encoder.h"
#include "../Config/odometry_config.h"
#include "../Tools/Average.h"


class MotorSpeedSensor
{
public:
    MotorSpeedSensor(
        float freqAsserv,
        uint8_t pin_a,
        uint8_t pin_b,
        volatile float & motorSpeed
    ):
        freqAsserv(freqAsserv),
        encoder(pin_a, pin_b),
        motorSpeed(motorSpeed)
    {
        motorTicks = 0;
        previousMotorTicks = 0;
        deltaMotorTicks = 0;
    }

    void compute()
    {
        motorTicks = encoder.read();                        // R�cup�ration des donn�es de l'encodeur        
        deltaMotorTicks = motorTicks - previousMotorTicks;  // Calcul du mouvement de la roue depuis le dernier asservissement
        previousMotorTicks = motorTicks;
        motorSpeed = (float)deltaMotorTicks * freqAsserv * TICK_TO_MM * MOTOR_TICK_TO_TICK; // Mise � jour de la vitesse
        averageSpeed.add(motorSpeed);
        motorSpeed = averageSpeed.value();
    }

private:
    const float freqAsserv;	// Fr�quence d'appel de la m�thode 'compute'. Utilis�e pour le calcul des vitesses.
    Encoder encoder;
    volatile float & motorSpeed;    // Vitesse instantan�e moyenn�e du moteur. Unit� : mm/s
    int32_t motorTicks;
    int32_t previousMotorTicks;
    int32_t deltaMotorTicks;
    Average<float, AVERAGE_SPEED_SIZE> averageSpeed;
};


#endif
