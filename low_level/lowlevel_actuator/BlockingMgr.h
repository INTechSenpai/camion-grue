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
		beginTime = 0;
		blocked = false;
	}

	inline void compute()
	{
		if (abs(realSpeed) < abs(aimSpeed)*sensibility)
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

	void setTunings(float sensibility, uint32_t responseTime)
	{
		this->sensibility = sensibility;
		this->responseTime = responseTime;
	}

	void getTunings(float & sensibility, uint32_t & responseTime) const
	{
		sensibility = this->sensibility;
		responseTime = this->responseTime;
	}

	inline bool isBlocked() const
	{
		return blocked && (millis() - beginTime > responseTime);
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

	bool blocked;
	uint32_t beginTime;
};


#endif
