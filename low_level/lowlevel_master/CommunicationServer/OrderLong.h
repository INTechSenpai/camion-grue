#ifndef _ORDERLONG_h
#define _ORDERLONG_h

#include <vector>
#include "Serializer.h"
#include "../Locomotion/MotionControlSystem.h"
#include "../Locomotion/MoveState.h"
#include "../CommunicationServer/CommunicationServer.h"
#include "../SlaveCommunication/SlaveActuator.h"
#include "../SlaveCommunication/SlaveSensorLed.h"
#include "../Tools/Singleton.h"

class OrderLong
{
public:
    OrderLong() :
        motionControlSystem(MotionControlSystem::Instance()),
        slaveActuator(SlaveActuator::Instance()),
        slaveSensorLed(SlaveSensorLed::Instance()),
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
    SlaveActuator & slaveActuator;
    SlaveSensorLed & slaveSensorLed;
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
            Server.printf(SPY_ORDER, "FollowTrajectory\n");
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
        Server.printf(SPY_ORDER, "End FollowTrajectory with status %u\n", status);
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
            Server.printf(SPY_ORDER, "Stop");
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
        Server.printf(SPY_ORDER, "WaitForJumper\n");
        pinMode(PIN_GET_JUMPER, INPUT_PULLUP);
        state = WAIT_FOR_INSERTION;
        debounceTimer = 0;
    }
    void onExecute()
    {
        uint8_t jumperDetected = digitalRead(PIN_GET_JUMPER);
        switch (state)
        {
        case WaitForJumper::WAIT_FOR_INSERTION:
            if (jumperDetected)
            {
                state = WAIT_FOR_REMOVAL;
                slaveSensorLed.setLightOn((uint8_t)SlaveSensorLed::FLASHING);
            }
            break;
        case WaitForJumper::WAIT_FOR_REMOVAL:
            if (!jumperDetected)
            {
                state = WAIT_FOR_DEBOUNCE_TIMER;
                debounceTimer = millis();
            }
            break;
        case WaitForJumper::WAIT_FOR_DEBOUNCE_TIMER:
            if (jumperDetected)
            {
                state = WAIT_FOR_REMOVAL;
            }
            else if (millis() - debounceTimer > 100)
            {
                finished = true;
            }
            break;
        default:
            break;
        }
    }
    void terminate(std::vector<uint8_t> & output) {}

private:
    enum JumperState
    {
        WAIT_FOR_INSERTION,
        WAIT_FOR_REMOVAL,
        WAIT_FOR_DEBOUNCE_TIMER
    };

    JumperState state;
    uint32_t debounceTimer;
};


class StartChrono : public OrderLong, public Singleton<StartChrono>
{
public:
    StartChrono() { chrono = 0; }
    void _launch(const std::vector<uint8_t> & input)
    {
        Server.printf(SPY_ORDER, "StartChrono");
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
        slaveActuator.stop();
        // todo Maybe: prevent HL from giving orders
        slaveSensorLed.setLightOn((uint8_t)(SlaveSensorLed::TURN_LEFT | SlaveSensorLed::TURN_RIGHT));
    }

private:
    uint32_t chrono;
};


/*
    Contrôle de l'actionneur
*/


class ActGoToHome : public OrderLong, public Singleton<ActGoToHome>
{
public:
    ActGoToHome() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 0)
        {
            slaveActuator.goToHome();
        }
        else
        {
            Server.printf_err("ActGoToHome: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        Serializer::writeInt(slaveActuator.getStatus(), output);
    }
};


class ActTakeCubeUsingSensor : public OrderLong, public Singleton<ActTakeCubeUsingSensor>
{
public:
    ActTakeCubeUsingSensor() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 4)
        {
            size_t index = 0;
            float angle = Serializer::readFloat(input, index);
            slaveActuator.takeCubeUsingSensor(angle);
        }
        else
        {
            Server.printf_err("ActTakeCubeUsingSensor: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        int32_t ret = slaveActuator.getStatus();
        if (!slaveActuator.isCubeInPlier())
        {
            ret |= ACTUATOR_STATUS_CUBE_MISSED;
        }
        Serializer::writeInt(ret, output);
    }
};


class ActTakeCubeFixed : public OrderLong, public Singleton<ActTakeCubeFixed>
{
public:
    ActTakeCubeFixed() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 4)
        {
            size_t index = 0;
            float angle = Serializer::readFloat(input, index);
            slaveActuator.takeCubeFixed(angle);
        }
        else
        {
            Server.printf_err("ActTakeCubeFixed: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        int32_t ret = slaveActuator.getStatus();
        if (!slaveActuator.isCubeInPlier())
        {
            ret |= ACTUATOR_STATUS_CUBE_MISSED;
        }
        Serializer::writeInt(ret, output);
    }
};


class ActStoreInside : public OrderLong, public Singleton<ActStoreInside>
{
public:
    ActStoreInside() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 0)
        {
            slaveActuator.storeCubeInside();
        }
        else
        {
            Server.printf_err("ActStoreInside: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        Serializer::writeInt(slaveActuator.getStatus(), output);
    }
};


class ActStoreOnTop : public OrderLong, public Singleton<ActStoreOnTop>
{
public:
    ActStoreOnTop() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 0)
        {
            slaveActuator.storeCubeOnTop();
        }
        else
        {
            Server.printf_err("ActStoreOnTop: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        Serializer::writeInt(slaveActuator.getStatus(), output);
    }
};


class ActTakeFromStorage : public OrderLong, public Singleton<ActTakeFromStorage>
{
public:
    ActTakeFromStorage() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 0)
        {
            slaveActuator.takeCubeFromStorage();
        }
        else
        {
            Server.printf_err("ActTakeFromStorage: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        Serializer::writeInt(slaveActuator.getStatus(), output);
    }
};


class ActPutOnPileUsingSensor : public OrderLong, public Singleton<ActPutOnPileUsingSensor>
{
public:
    ActPutOnPileUsingSensor() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 8)
        {
            size_t index = 0;
            float angle = Serializer::readFloat(input, index);
            int32_t floor = Serializer::readInt(input, index);
            slaveActuator.putCubeUsingSensor(angle, floor);
        }
        else
        {
            Server.printf_err("ActPutOnPileUsingSensor: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        Serializer::writeInt(slaveActuator.getStatus(), output);
    }
};


class ActPutOnPileFixed : public OrderLong, public Singleton<ActPutOnPileFixed>
{
public:
    ActPutOnPileFixed() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 8)
        {
            size_t index = 0;
            float angle = Serializer::readFloat(input, index);
            int32_t floor = Serializer::readInt(input, index);
            slaveActuator.putCubeFixed(angle, floor);
        }
        else
        {
            Server.printf_err("ActPutOnPileFixed: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        Serializer::writeInt(slaveActuator.getStatus(), output);
    }
};


class ActGoToPosition : public OrderLong, public Singleton<ActGoToPosition>
{
public:
    ActGoToPosition() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        if (input.size() == 16)
        {
            size_t index = 0;
            float angleH = Serializer::readFloat(input, index);
            float angleV = Serializer::readFloat(input, index);
            float angleHead = Serializer::readFloat(input, index);
            float posPlier = Serializer::readFloat(input, index);
            slaveActuator.goToPosition(angleH, angleV, angleHead, posPlier);
        }
        else
        {
            Server.printf_err("ActGoToPosition: wrong number of arguments\n");
        }
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output)
    {
        Serializer::writeInt(slaveActuator.getStatus(), output);
    }
};


class ActStop : public OrderLong, public Singleton<ActStop>
{
public:
    ActStop() {}
    void _launch(const std::vector<uint8_t> & input)
    {
        slaveActuator.stop();
    }
    void onExecute()
    {
        finished = !slaveActuator.isMoving();
    }
    void terminate(std::vector<uint8_t> & output) {}
};


#endif
