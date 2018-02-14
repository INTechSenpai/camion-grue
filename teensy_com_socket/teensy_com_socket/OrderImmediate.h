#ifndef _ORDERIMMEDIATE_h
#define _ORDERIMMEDIATE_h

#include <vector>
#include "Singleton.h"
#include "FloatBinaryEncoder.h"


class OrderImmediate
{
public:
	OrderImmediate()
	{}

	/*
	M�thode ex�cutant l'ordre imm�diat.
	L'argument correspond � la fois � l'input et � l'output de l'odre, il sera modifi� par la m�thode.
	*/
	virtual void execute(std::vector<uint8_t> &) = 0;

protected:

};


// ### D�finition des ordres � r�ponse imm�diate ###

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


#endif
