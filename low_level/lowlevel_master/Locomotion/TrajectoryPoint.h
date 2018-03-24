#ifndef _TRAJECTORY_POINT_h
#define _TRAJECTORY_POINT_h

#include <Printable.h>
#include "Position.h"

class TrajectoryPoint : public Printable
{
public:
	TrajectoryPoint()
	{
		/* Default TrajectoryPoint */
		stopPoint = true;
		endOfTrajectory = true;
		curvature = 0;
		algebricMaxSpeed = 0;
	}

	TrajectoryPoint(const Position & aPos, float aCurvature, float aSpeed, bool isStopPoint, bool isEndOfTraj)
	{
		position = aPos;
		curvature = aCurvature;
		algebricMaxSpeed = aSpeed;
		stopPoint = isStopPoint;
		endOfTrajectory = isEndOfTraj;
	}

	Position getPosition() const
	{
		return position;
	}

	bool isStopPoint() const
	{
		return stopPoint;
	}

	bool isEndOfTrajectory() const
	{
		return endOfTrajectory;
	}

	float getCurvature() const
	{
		return curvature;
	}

	float getAlgebricMaxSpeed() const
	{
		return algebricMaxSpeed;
	}

	size_t printTo(Print& p) const
	{
		size_t count = 0;
		count += p.print(position);
		count += p.printf("_%g_%g_%d_%d", curvature, algebricMaxSpeed, stopPoint, endOfTrajectory);
		return count;
	}

private:
	Position position;
	bool stopPoint;
	bool endOfTrajectory;
	float curvature; // m^-1
	float algebricMaxSpeed; // mm/s
};


#endif

