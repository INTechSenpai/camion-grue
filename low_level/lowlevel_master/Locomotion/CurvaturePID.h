#ifndef _CURVATURE_PID_h
#define _CURVATURE_PID_h


#include <Printable.h>
#include "TrajectoryPoint.h"
#include "Position.h"


class CurvaturePID : public Printable
{
public:
	CurvaturePID(
		volatile Position const & position, 
		volatile float & curvatureOrder, 
		TrajectoryPoint const & trajectoryPoint
	) :
		currentPosition(position),
		curvatureOrder(curvatureOrder),
		trajectoryPoint(trajectoryPoint)
	{
		setCurvatureLimits(-20, 20);
		setTunings(0, 0);
		posError = 0;
		orientationError = 0;
	}

	inline void compute(bool movingForward)
	{
		/* Asservissement sur trajectoire */

		Position posConsigne = trajectoryPoint.getPosition();
		float trajectoryCurvature = trajectoryPoint.getCurvature();
		posError = -(currentPosition.x - posConsigne.x) * sinf(posConsigne.orientation) + (currentPosition.y - posConsigne.y) * cosf(posConsigne.orientation);
		orientationError = fmodulo(currentPosition.orientation - posConsigne.orientation, TWO_PI);

		if (orientationError > PI)
		{
			orientationError -= TWO_PI;
		}

		if (movingForward)
		{
			curvatureOrder = trajectoryCurvature - k1 * posError - k2 * orientationError;
		}
		else
		{
			curvatureOrder = trajectoryCurvature - k1 * posError + k2 * orientationError;
		}
	}

	void setCurvatureLimits(float min, float max)
	{
		if (min >= max)
		{
			return;
		}

		outMin = min;
		outMax = max;

		if (curvatureOrder > outMax)
		{
			curvatureOrder = outMax;
		}
		else if (curvatureOrder < outMin)
		{
			curvatureOrder = outMin;
		}
	}

	void setTunings(float k1, float k2)
	{
		if (k1 < 0 || k2 < 0)
		{
			this->k1 = 0;
			this->k2 = 0;
		}
		this->k1 = k1;
		this->k2 = k2;
	}

	float getPositionError()
	{
		return posError;
	}

	float getOrientationError()
	{
		return orientationError;
	}

	size_t printTo(Print& p) const
	{
		return p.printf("%u_%g_%g_%g", millis(), posError, orientationError, curvatureOrder);
	}

private:
	volatile Position const & currentPosition;	// Position courante du robot
	TrajectoryPoint const & trajectoryPoint;	// Point de trajectoire courant (consigne à suivre)
	volatile float & curvatureOrder;			// Courbure consigne. Unitée : m^-1

	float k1, k2;
	float posError, orientationError;
	float outMax, outMin;
};


#endif