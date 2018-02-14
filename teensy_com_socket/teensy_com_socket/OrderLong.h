#ifndef _ORDERLONG_h
#define _ORDERLONG_h

#include <vector>
#include "Singleton.h"
#include "FloatBinaryEncoder.h"

class OrderLong
{
public:
	OrderLong() :
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
	bool finished;
};


// ### Définition des ordres longs ###

class RienL : public OrderLong, public Singleton<RienL>
{
public:
	RienL() {}
	void _launch(const std::vector<uint8_t> & input)
	{}
	void onExecute(std::vector<uint8_t> & output)
	{}
	void terminate(std::vector<uint8_t> & output)
	{}
};


#endif


