#ifndef _MOTIONCONTROLSYSTEM_h
#define _MOTIONCONTROLSYSTEM_h

#include "TrajectoryFollower.h"
#include "MoveState.h"
#include "Position.h"
#include "TrajectoryPoint.h"
#include "MotionControlTunings.h"
#include "../Tools/Singleton.h"
#include <vector>


#define FREQ_ASSERV		1000					// Fr�quence d'asservissement (Hz)
#define PERIOD_ASSERV	(1000000 / FREQ_ASSERV)	// P�riode d'asservissement (�s)
#define TRAJECTORY_STEP 20			            // Distance entre deux points d'une trajectoire (mm)


class MotionControlSystem : public Singleton<MotionControlSystem>
{
public:
	MotionControlSystem() : 
		trajectoryFollower(FREQ_ASSERV, position, moveStatus)
	{
		travellingToDestination = false;
		trajectoryIndex = 0;
		trajectoryComplete = false;
		moveStatus = MOVE_OK;
	}


	/*
		#################################################
		#  M�thodes � appeller durant une interruption  #
		#################################################
	*/

	void control()
	{
		static bool wasTravellingToDestination = false;

        trajectoryFollower.control();
		if (travellingToDestination)
		{
            MovePhase movePhase = trajectoryFollower.getMovePhase();

            if (currentTrajectory.size() > trajectoryIndex)
            {
			    if (!wasTravellingToDestination)
			    {// D�marrage du suivi de trajectoire
				    trajectoryFollower.setTrajectoryPoint(currentTrajectory.at(trajectoryIndex));
				    trajectoryFollower.startMove();
				    wasTravellingToDestination = true;
			    }
            
                if (movePhase == MOVING && !currentTrajectory.at(trajectoryIndex).isStopPoint())
                {
                    Position trajPoint = currentTrajectory.at(trajectoryIndex).getPosition();
                    float sign;
                    if (trajectoryFollower.isMovingForward()) {
                        sign = 1;
                    }
                    else {
                        sign = -1;
                    }

                    if (((position.x - trajPoint.x) * cosf(trajPoint.orientation) + (position.y - trajPoint.y) * sinf(trajPoint.orientation)) * sign > 0)
                    {
                        if (currentTrajectory.size() > trajectoryIndex + 1)
                        {
                            trajectoryIndex++;
                            updateDistanceToTravel();
                            trajectoryFollower.setTrajectoryPoint(currentTrajectory.at(trajectoryIndex));
                        }
                        else
                        {
                            moveStatus |= EMPTY_TRAJ;
                            stop_and_clear_trajectory();
                        }
                    }
                }
                else if (movePhase == MOVE_ENDED)
                {
                    if (currentTrajectory.at(trajectoryIndex).isEndOfTrajectory() || moveStatus != MOVE_OK)
                    {
                        stop_and_clear_trajectory();
                        travellingToDestination = false;
                        wasTravellingToDestination = false;
                    }
                    else
                    {
                        if (currentTrajectory.size() > trajectoryIndex + 1)
                        {
                            trajectoryIndex++;
                            updateDistanceToTravel();
                            trajectoryFollower.setTrajectoryPoint(currentTrajectory.at(trajectoryIndex));
                            trajectoryFollower.startMove();
                        }
                        else
                        {
                            moveStatus |= EMPTY_TRAJ;
                            stop_and_clear_trajectory();
                            travellingToDestination = false;
                            wasTravellingToDestination = false;
                        }
                    }
                }
            }
            else if (movePhase != BREAKING)
            {
                moveStatus |= EMPTY_TRAJ;
                stop_and_clear_trajectory();
                if (movePhase == MOVE_ENDED)
                {
                    travellingToDestination = false;
                    wasTravellingToDestination = false;
                }
            }
		}
	}


	/*
		###################################################
		#  M�thodes � appeller dans la boucle principale  #
		###################################################
	*/

	void followTrajectory()
	{
		noInterrupts();
		moveStatus = MOVE_OK;
		travellingToDestination = true;
		interrupts();
	}

	void stop_and_clear_trajectory()
	{
        // todo (v�rifier que j'ai pens�  tout)
        // tofix: interrupt/nointerrupt ?
        MovePhase movePhase = trajectoryFollower.getMovePhase();
        if (movePhase != MOVE_ENDED)
        {
            trajectoryFollower.emergency_stop();
        }
        trajectoryIndex = 0;
        currentTrajectory.clear();
	}

	bool isMovingToDestination() const
	{
		noInterrupts();
		bool moving = travellingToDestination;
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
	{// todo : check if endOfTrajecory
		if (index < currentTrajectory.size() && index >= trajectoryIndex)
		{
			if (index == trajectoryIndex && travellingToDestination)
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
    void updateDistanceToTravel()
    {
        size_t i;
        for (i = trajectoryIndex; i < currentTrajectory.size(); i++)
        {
            if (currentTrajectory.at(i).isStopPoint())
            {
                float distanceToDrive = ((float)i - (float)trajectoryIndex + 1) * TRAJECTORY_STEP;
                trajectoryFollower.setDistanceToDrive(distanceToDrive);
                return;
            }
        }
        trajectoryFollower.setInfiniteDistanceToDrive();
    }

	TrajectoryFollower trajectoryFollower;
	volatile Position position;
	volatile MoveStatus moveStatus;
	volatile bool travellingToDestination;  // Indique si le robot est en train de parcourir la trajectoire courante
	volatile size_t trajectoryIndex;  // Point courant de la trajectoire courante

	std::vector<TrajectoryPoint> currentTrajectory;
	bool trajectoryComplete;
};


#endif