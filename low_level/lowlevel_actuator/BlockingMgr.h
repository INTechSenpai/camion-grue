#ifndef _BLOCKINGMGR_h
#define _BLOCKINGMGR_h

#include <Printable.h>


/*
	Permet de détecter le blocage physique d'un moteur disposant d'un encodeur 
*/

class BlockingMgr : public Printable
{
public:
	BlockingMgr(volatile float const & aimSpeed, volatile float const & realSpeed) :
		aimSpeed(aimSpeed),
		realSpeed(realSpeed)
	{
		sensibility = 0;
		responseTime = 0;
        speedThreshold = 0;
		beginTime = 0;
		blocked = false;
	}

	inline void compute()
	{
		if (abs(aimSpeed) > speedThreshold && abs(realSpeed) < abs(aimSpeed)*sensibility)
		{
			if (!blocked)
			{
				blocked = true;
				beginTime = millis();
			}
		}
		else
		{
			blocked = false;
		}
	}

	void setTunings(float sensibility, uint32_t responseTime, float speedThreshold)
	{
		this->sensibility = sensibility;
		this->responseTime = responseTime;
        this->speedThreshold = speedThreshold;
	}

	void getTunings(float & sensibility, uint32_t & responseTime, float & speedThreshold) const
	{
		sensibility = this->sensibility;
		responseTime = this->responseTime;
        speedThreshold = this->speedThreshold;
	}

	inline bool isBlocked() const
	{
        bool ret = blocked && (millis() - beginTime > responseTime);
        if (ret)
        {
            Serial.printf("aimS=%g realS=%g\n", aimSpeed, realSpeed);
        }
        return ret;
	}

	size_t printTo(Print& p) const
	{
		return p.printf("%u_%g_%g_%d", millis(), aimSpeed, realSpeed, isBlocked());
	}

private:
	volatile float const & aimSpeed;
	volatile float const & realSpeed;

	float sensibility; // Entre 0 et 1. Le seuil "vitesse insuffisante" vaut aimSpeed*sensibility
	uint32_t responseTime; // ms
    float speedThreshold;

	bool blocked;
	uint32_t beginTime;
};


#endif
