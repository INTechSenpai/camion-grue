#ifndef _MOTIONCONTROLSYSTEM_h
#define _MOTIONCONTROLSYSTEM_h

#include "TrajectoryFollower.h"
#include "MoveState.h"
#include "Position.h"
#include "TrajectoryPoint.h"
#include "MotionControlTunings.h"
#include "../Tools/Singleton.h"
#include "../CommunicationServer/CommunicationServer.h"
#include <vector>


#define FREQ_ASSERV		1000					// Fréquence d'asservissement (Hz)
#define PERIOD_ASSERV	(1000000 / FREQ_ASSERV)	// Période d'asservissement (µs)
#define TRAJECTORY_STEP 20			            // Distance entre deux points d'une trajectoire (mm)

#define TRAJECTORY_EDITION_SUCCESS  0
#define TRAJECTORY_EDITION_FAILURE  1


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
		#  Méthodes à appeller durant une interruption  #
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
			    {// Démarrage du suivi de trajectoire
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
                            stop_and_clear_trajectory_from_interrupt();
                        }
                    }
                }
                else if (movePhase == MOVE_ENDED)
                {
                    if (currentTrajectory.at(trajectoryIndex).isEndOfTrajectory() || moveStatus != MOVE_OK)
                    {
                        stop_and_clear_trajectory_from_interrupt();
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
                            stop_and_clear_trajectory_from_interrupt();
                            travellingToDestination = false;
                            wasTravellingToDestination = false;
                        }
                    }
                }
            }
            else if (movePhase != BREAKING)
            {
                stop_and_clear_trajectory_from_interrupt();
                if (movePhase == MOVE_ENDED)
                {
                    travellingToDestination = false;
                    wasTravellingToDestination = false;
                }
                else
                {
                    moveStatus |= EMPTY_TRAJ;
                }
            }
		}
	}

private:
    void stop_and_clear_trajectory_from_interrupt()
    {
        // todo (vérifier que j'ai pensé  tout)
        trajectoryFollower.emergency_stop_from_interrupt();
        trajectoryIndex = 0;
        currentTrajectory.clear();
    }

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


	/*
		###################################################
		#  Méthodes à appeller dans la boucle principale  #
		###################################################
	*/

 public:
	void followTrajectory()
	{
        if (trajectoryFollower.isTrajectoryControlled())
        {
            noInterrupts();
            moveStatus = MOVE_OK;
            travellingToDestination = true;
            interrupts();
        }
        else
        {
            Server.printf_err("MotionControlSystem::followTrajectory : trajectory is not controlled\n");
        }
	}

    void startManualMove()
    {
        if (!trajectoryFollower.isTrajectoryControlled())
        {
            trajectoryFollower.startMove();
        }
        else
        {
            Server.printf_err("MotionControlSystem::startManualMove : trajectory is controlled\n");
        }
    }

	void stop_and_clear_trajectory()
	{
        noInterrupts();
        stop_and_clear_trajectory_from_interrupt();
        interrupts();
	}

	bool isMovingToDestination() const
	{
		noInterrupts();
		bool moving = travellingToDestination;
		interrupts();
		return moving;
	}

	uint8_t appendToTrajectory(TrajectoryPoint trajectoryPoint)
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
            return TRAJECTORY_EDITION_SUCCESS;
		}
		else
		{
            return TRAJECTORY_EDITION_FAILURE;
		}
	}

	uint8_t updateTrajectory(size_t index, TrajectoryPoint trajectoryPoint)
	{
		if (index < currentTrajectory.size() && index >= trajectoryIndex)
		{
			if (index == trajectoryIndex && travellingToDestination)
			{
                return TRAJECTORY_EDITION_FAILURE;
			}
			else
			{
				noInterrupts();
                if (currentTrajectory.at(index).isEndOfTrajectory())
                {
                    trajectoryComplete = false;
                }
				currentTrajectory.at(index) = trajectoryPoint;
                if (trajectoryPoint.isEndOfTrajectory())
                {
                    trajectoryComplete = true;
                    if (currentTrajectory.size() > index + 1)
                    {
                        currentTrajectory.erase(currentTrajectory.begin() + index + 1, currentTrajectory.end());
                    }
                }
				interrupts();

                return TRAJECTORY_EDITION_SUCCESS;
			}
		}
		else
		{
            return TRAJECTORY_EDITION_FAILURE;
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

    void setPWM(int16_t pwm)
    {
        noInterrupts();
        trajectoryFollower.setPWM(pwm);
        interrupts();
    }

    void setMaxSpeed(float speed)
    {
        noInterrupts();
        trajectoryFollower.setMaxSpeed(speed);
        interrupts();
    }

    void setDistanceToDrive(float distance)
    {
        noInterrupts();
        trajectoryFollower.setDistanceToDrive(distance);
        interrupts();
    }

    void setCurvature(float curvature)
    {
        noInterrupts();
        trajectoryFollower.setCurvature(curvature);
        interrupts();
    }

    void setMonitoredMotor(MonitoredMotor m)
    {
        trajectoryFollower.setMonitoredMotor(m);
    }

    void sendLogs()
    {
        trajectoryFollower.sendLogs();
    }

private:
	TrajectoryFollower trajectoryFollower;
	volatile Position position;
	volatile MoveStatus moveStatus;
	volatile bool travellingToDestination;  // Indique si le robot est en train de parcourir la trajectoire courante
	volatile size_t trajectoryIndex;  // Point courant de la trajectoire courante

	std::vector<TrajectoryPoint> currentTrajectory;
	bool trajectoryComplete;
};


#endif