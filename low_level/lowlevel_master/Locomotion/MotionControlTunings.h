#ifndef _MOTION_CONTROL_TUNINGS_h
#define _MOTION_CONTROL_TUNINGS_h

#include "Printable.h"


class MotionControlTunings : public Printable
{
public:
	MotionControlTunings()
	{
		setDefault();
	}

	void setDefault()
	{
        maxAcceleration = 2500;
        maxDeceleration = 12000;
        maxCurvature = 10;

        blockingSensibility = 0;
        blockingResponseTime = 500;

        stoppedSpeed = 5;
        stoppingResponseTime = 100;

        curvatureK1 = 0.1;
        curvatureK2 = 12;

        translationKp = 2.75;
        translationKd = 1.5;

        speedKp = 3;
        speedKi = 0.05;
        speedKd = 100;
	}

    size_t printTo(Print& p) const
    {
        size_t ret = 0;
        ret += p.printf("maxAcceleration=%g\n", maxAcceleration);
        ret += p.printf("maxDeceleration=%g\n", maxDeceleration);
        ret += p.printf("maxCurvature=%g\n", maxCurvature);
        ret += p.printf("blockingSensibility=%g\n", blockingSensibility);
        ret += p.printf("blockingResponseTime=%u\n", blockingResponseTime);
        ret += p.printf("stoppedSpeed=%g\n", stoppedSpeed);
        ret += p.printf("stoppingResponseTime=%u\n", stoppingResponseTime);
        ret += p.printf("curvatureK1=%g\n", curvatureK1);
        ret += p.printf("curvatureK2=%g\n", curvatureK2);
        ret += p.printf("translationKp=%g\n", translationKp);
        ret += p.printf("translationKd=%g\n", translationKd);
        ret += p.printf("speedKp=%g\n", speedKp);
        ret += p.printf("speedKi=%g\n", speedKi);
        ret += p.printf("speedKd=%g\n", speedKd);

        return ret;
    }

    float maxAcceleration;      // mm*s^-2
    float maxDeceleration;      // mm*s^-2
    float maxCurvature;         // m^-1

    float blockingSensibility;      // 0: aucune  ---> 1: ultra sensible
    uint32_t blockingResponseTime;  // ms

    float stoppedSpeed;             // Vitesse en dessous de laquelle on considère être à l'arrêt. mm/s
    uint32_t stoppingResponseTime;  // ms

    float curvatureK1;
    float curvatureK2;

    float translationKp;
    float translationKd;

    float speedKp;
    float speedKi;
    float speedKd;

};


#endif
