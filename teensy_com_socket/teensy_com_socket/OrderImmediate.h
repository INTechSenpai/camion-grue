#ifndef _ORDERIMMEDIATE_h
#define _ORDERIMMEDIATE_h

#include <vector>
#include "Singleton.h"


class OrderImmediate
{
public:
	OrderImmediate()
	{}

	/*
	Méthode exécutant l'ordre immédiat.
	L'argument correspond à la fois à l'input et à l'output de l'odre, il sera modifié par la méthode.
	*/
	virtual void execute(std::vector<uint8_t> &) = 0;

protected:

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
	}
};


#endif
