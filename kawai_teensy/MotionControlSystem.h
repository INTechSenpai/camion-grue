#ifndef _MOTIONCONTROLSYSTEM_h
#define _MOTIONCONTROLSYSTEM_h

#include "TrajectoryFollower.h"
#include "MoveState.h"
#include "Position.h"
#include "TrajectoryPoint.h"
#include "MotionControlTunings.h"
#include "Singleton.h"
#include <vector>


#define FREQ_ASSERV		1000					// Fréquence d'asservissement (Hz)
#define PERIOD_ASSERV	(1000000 / FREQ_ASSERV)	// Période d'asservissement (µs)


class MotionControlSystem : public Singleton<MotionControlSystem>
{
public:
	MotionControlSystem() : 
		trajectoryFollower(FREQ_ASSERV, position, moveStatus)
	{
		movingToDestination = false;
		trajectoryIndex = 0;
		trajectoryComplete = false;
		moveStatus = MOVE_OK;
	}


	/*
		#################################################
		#  Méthodes à appeller durant une interruption  #
		#################################################
	*/

	void control()
	{
		static bool wasMovingToDestination = false;

		trajectoryFollower.control();
		if (movingToDestination)
		{
			if (!wasMovingToDestination)
			{
				trajectoryFollower.setTrajectoryPoint(currentTrajectory.at(trajectoryIndex));
				trajectoryFollower.startMove();
				
				wasMovingToDestination = true;
			}

		}
	}


	/*
		###################################################
		#  Méthodes à appeller dans la boucle principale  #
		###################################################
	*/

	void followTrajectory()
	{
		noInterrupts();
		moveStatus = MOVE_OK;
		movingToDestination = true;
		interrupts();
	}

	void stop_and_clear_trajectory()
	{

	}

	bool isMovingToDestination() const
	{
		noInterrupts();
		bool moving = movingToDestination;
		interrupts();
		return moving;
	}

	void appendToTrajectory(TrajectoryPoint trajectoryPoint)
	{
		if (!trajectoryComplete)
		{
			noInterrupts();
			currentTrajectory.push_back(trajectoryPoint);
			interrupts();
			if (trajectoryPoint.isEndOfTrajectory())
			{
				trajectoryComplete = true;
			}
		}
		else
		{
			// todo: throw error
		}
	}

	void updateTrajectory(size_t index, TrajectoryPoint trajectoryPoint)
	{
		if (index < currentTrajectory.size() && index >= trajectoryIndex)
		{
			if (index == trajectoryIndex && movingToDestination)
			{
				// todo: throw error
			}
			else
			{
				noInterrupts();
				currentTrajectory.at(index) = trajectoryPoint;
				interrupts();
			}
		}
		else
		{
			// todo: throw error
		}
	}

	Position getPosition() const
	{
		static Position p; 
		noInterrupts();
		p = position;
		interrupts();
		return p;
	}

	void setPosition(Position p)
	{
		noInterrupts();
		position = p;
		interrupts();
	}

	MoveStatus getMoveStatus() const
	{
		static MoveStatus ms;
		noInterrupts();
		ms = moveStatus;
		interrupts();
		return ms;
	}

	void setMotionControlLevel(uint8_t level)
	{
		trajectoryFollower.setMotionControlLevel(level);
	}

	uint8_t getMotionControlLevel() const
	{
		return trajectoryFollower.getMotionControlLevel();
	}

	void setTunings(MotionControlTunings const & tunings)
	{
		trajectoryFollower.setTunings(tunings);
	}

	MotionControlTunings getTunings()
	{
		return trajectoryFollower.getTunings();
	}


private:
	TrajectoryFollower trajectoryFollower;
	volatile Position position;
	volatile MoveStatus moveStatus;
	volatile bool movingToDestination;
	volatile size_t trajectoryIndex;

	std::vector<TrajectoryPoint> currentTrajectory;
	bool trajectoryComplete;
};


#endif