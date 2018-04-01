#ifndef _ORDERLONG_h
#define _ORDERLONG_h

#include <vector>
#include "Serializer.h"
#include "../Locomotion/MotionControlSystem.h"
#include "../Locomotion/MoveState.h"
#include "../CommunicationServer/CommunicationServer.h"
#include "../Tools/Singleton.h"

class OrderLong
{
public:
    OrderLong() :
        motionControlSystem(MotionControlSystem::Instance()),
        finished(true)
    {}

    void launch(const std::vector<uint8_t> & arg)
    {
        finished = false;
        _launch(arg);
    }

    /* Lancement de l'ordre long. L'argument correspond à un input (NEW_ORDER). */
    virtual void _launch(const std::vector<uint8_t> &) = 0;

    /* Méthode exécutée en boucle durant l'exécution de l'odre. */
    virtual void onExecute() = 0;

    /* Méthode indiquant si l'odre long a fini son exécution ou non. */
    bool isFinished()
    {
        return finished;
    }

    /* Méthode à appeler une fois que l'odre est terminé. L'argument est un output, il correspond au contenu du EXECUTION_END. */
    virtual void terminate(std::vector<uint8_t> &) = 0;

protected:
    MotionControlSystem & motionControlSystem;
    bool finished;
};


// ### Définition des ordres longs ###

/*
class Rien : public OrderLong, public Singleton<Rien>
{
public:
    Rien() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == EXPECTED_SIZE)
        {
            // process input
        }
        else
        {
            Server.printf_err("Rien: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        
    }
    void terminate(std::vector<uint8_t> & output)
    {

    }
};
//*/


class FollowTrajectory : public OrderLong, public Singleton<FollowTrajectory>
{
public:
    FollowTrajectory() { status = MOVE_OK; }
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 0)
        {
            motionControlSystem.followTrajectory();
        }
        else
        {
            Server.printf_err("FollowTrajectory: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        if (!motionControlSystem.isMovingToDestination())
        {
            status = motionControlSystem.getMoveStatus();
            finished = true;
        }
    }
    void terminate(std::vector<uint8_t> & output)
    {
        Serializer::writeInt((int32_t)status, output);
    }

private:
    MoveStatus status;
};


class Stop : public OrderLong, public Singleton<Stop>
{
public:
    Stop() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 0)
        {
            Server.printf("stop");
            motionControlSystem.stop_and_clear_trajectory();
        }
        else
        {
            Server.printf_err("Stop: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        if (!motionControlSystem.isMovingToDestination())
        {
            finished = true;
        }
    }
    void terminate(std::vector<uint8_t> & output) {}
};


class WaitForJumper : public OrderLong, public Singleton<WaitForJumper>
{
public:
    WaitForJumper() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 0)
        {
            // TODO
        }
        else
        {
            Server.printf_err("WaitForJumper: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = true; // TODO
    }
    void terminate(std::vector<uint8_t> & output) {}
};


class StartChrono : public OrderLong, public Singleton<StartChrono>
{
public:
    StartChrono() { chrono = 0; }
    void _launch(const std::vector<uint8_t> & input)
    {
        chrono = millis();
    }
    void onExecute()
    {
        if (millis() - chrono > 100000)
        {
            finished = true;
        }
    }
    void terminate(std::vector<uint8_t> & output)
    {
        motionControlSystem.stop_and_clear_trajectory();
        // TODO: stop actuators
        // Maybe: prevent HL from giving orders
    }

private:
    uint32_t chrono;
};


#endif
