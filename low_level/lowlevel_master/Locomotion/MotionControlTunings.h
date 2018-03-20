#ifndef _MOTION_CONTROL_TUNINGS_h
#define _MOTION_CONTROL_TUNINGS_h


class MotionControlTunings
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

        speedKp = 0.6;
        speedKi = 0.01;
        speedKd = 20;
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
