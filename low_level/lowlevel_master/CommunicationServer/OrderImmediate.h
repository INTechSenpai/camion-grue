#ifndef _ORDERIMMEDIATE_h
#define _ORDERIMMEDIATE_h

#include <vector>
#include "../Locomotion/MotionControlSystem.h"
#include "../CommunicationServer/CommunicationServer.h"
#include "../Tools/Singleton.h"
#include "../Tools/FloatBinaryEncoder.h"


class OrderImmediate
{
public:
    OrderImmediate() : 
        motionControlSystem(MotionControlSystem::Instance())
    {}

    /*
    Méthode exécutant l'ordre immédiat.
    L'argument correspond à la fois à l'input et à l'output de l'ordre, il sera modifié par la méthode.
    */
    virtual void execute(std::vector<uint8_t> &) = 0;

protected:
    MotionControlSystem & motionControlSystem;
};


// ### Définition des ordres à réponse immédiate ###

class Rien : public OrderImmediate, public Singleton<Rien>
{
public:
    Rien() {}
    virtual void execute(std::vector<uint8_t> & io) {}
};


/*
    Ne fait rien, mais indique que le HL est vivant !
*/
class Ping : public OrderImmediate, public Singleton<Ping>
{
public:
    Ping() {}

    virtual void execute(std::vector<uint8_t> & io)
    {
        io.clear();
    // le ping doit répondre, donc on met une donnée qui ne sert à rien
    io.push_back(0);
    }
};


class GetColor : public OrderImmediate, public Singleton<GetColor>
{
public:
    GetColor() {}
    virtual void execute(std::vector<uint8_t> & io)
    {
        io.clear();

        io.push_back(0x02); // todo
    }
};


class SetPWM : public OrderImmediate, public Singleton<SetPWM>
{
public:
    SetPWM() {}
    virtual void execute(std::vector<uint8_t> & io)
    {
        if (io.size() == 8)
        {
            int16_t frontLeft = (int16_t)io.at(0) + ((int16_t)io.at(1) << 8);
            int16_t frontRight = (int16_t)io.at(2) + ((int16_t)io.at(3) << 8);
            int16_t backLeft = (int16_t)io.at(4) + ((int16_t)io.at(5) << 8);
            int16_t backRight = (int16_t)io.at(6) + ((int16_t)io.at(7) << 8);
            Server.printf("fl%d fr%d bl%d br%d\n", frontLeft, frontRight, backLeft, backRight);
            motionControlSystem.setPWM(frontLeft, frontRight, backLeft, backRight);
            motionControlSystem.startManualMove();
            io.clear();
        }
        else
        {
            Server.printf("invalid arg size:%lu\n", io.size());
        }
    }
};


//class Rien : public OrderImmediate, public Singleton<Rien>
//{
//public:
//    Rien() {}
//    virtual void execute(std::vector<uint8_t> & io) {}
//};
//
//
//class Rien : public OrderImmediate, public Singleton<Rien>
//{
//public:
//    Rien() {}
//    virtual void execute(std::vector<uint8_t> & io) {}
//};

#endif

