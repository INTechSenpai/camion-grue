#ifndef _ORDERMGR_h
#define _ORDERMGR_h

#include "CommunicationServer.h"
#include "OrderImmediate.h"
#include "OrderLong.h"
#include "Command.h"

#define EXEC_STACK_SIZE				16
#define NB_ORDER					256
#define LONG_ORDER_START_ID			32
#define IMMEDIATE_ORDER_START_ID	128
#define NB_LONG_ORDER (IMMEDIATE_ORDER_START_ID - LONG_ORDER_START_ID)
#define NB_IMMEDIATE_ORDER (NB_ORDER - IMMEDIATE_ORDER_START_ID)


class OrderMgr
{
public:
	OrderMgr()
	{
		for (size_t i = 0; i < NB_LONG_ORDER; i++)
		{
			longOrderList[i] = (OrderLong*)NULL;
		}
		for (size_t i = 0; i < NB_IMMEDIATE_ORDER; i++)
		{
			immediateOrderList[i] = (OrderImmediate*)NULL;
		}

		/*	################################################## *
		*	# Ici est définie la correspondance ID <-> Ordre # *
		*	# (il faut ajouter le START_ID à l'index du		 # *
		*	# tableau pour avoir l'ID utilisé dans la trame) # *
		*	################################################## */

		// Ordres à réponse immédiate
		immediateOrderList[0x00] = &Ping::Instance();

		// Ordres longs
//		longOrderList[0x00] = &RienL::Instance();
	}

	void execute()
	{
		Server.communicate();
		while (Server.available())
		{
			Command command = Server.getLastCommand();
			handleNewCommand(command);
		}
		executeStackedOrders();
	}

private:
	class OrderMemory
	{
	public:
		OrderMemory()
		{
			running = false;
			longOrderIndex = 0;
			commandId = 0;
			commandSource = 0;
		}

		void saveOrder(uint8_t index, Command const & command)
		{
			longOrderIndex = index;
			commandId = command.getId();
			commandSource = command.getSource();
			running = true;
		}

		void deleteOrder()
		{
			running = false;
		}

		uint8_t getIndex() const
		{
			return longOrderIndex;
		}

		uint8_t getId() const
		{
			return commandId;
		}

		uint8_t getSource() const
		{
			return commandSource;
		}

		bool isRunning() const
		{
			return running;
		}

	private:
		uint8_t longOrderIndex;
		uint8_t commandId;
		uint8_t commandSource;
		bool running;
	};

	void handleNewCommand(Command const & command)
	{
		if (command.isValid())
		{
			uint8_t id = command.getId();
			std::vector<uint8_t> data = command.getData();
			if (id >= IMMEDIATE_ORDER_START_ID)
			{
				uint8_t index = id - IMMEDIATE_ORDER_START_ID;
				if (index < NB_IMMEDIATE_ORDER && immediateOrderList[index] != NULL)
				{
					immediateOrderList[index]->execute(data);
					if (data.size() > 0)
					{
						Command answer(command.getSource(), id, data);
						Server.sendAnswer(answer);
					}
				}
				else
				{
					Server.printf_err("Unknown immediate order: %u\n", index);
				}
			}
			else if (id >= LONG_ORDER_START_ID)
			{
				uint8_t index = id - LONG_ORDER_START_ID;
				if (index < NB_LONG_ORDER && longOrderList[index] != NULL)
				{
					if (addOrderToStack(index, command) == 0)
					{
						longOrderList[index]->launch(data);
					}
					else
					{
						Server.printf_err("Too many long orders already running\n");
					}
				}
				else
				{
					Server.printf_err("Unknown long order: %u\n", index);
				}
			}
			else
			{
				Server.printf_err("Invalid command id\n");
				Server.print_err(command);
			}
		}
		else
		{
			Server.printf_err("Invalid command\n");
			Server.print_err(command);
		}
	}

	void executeStackedOrders()
	{
		for (size_t i = 0; i < EXEC_STACK_SIZE; i++)
		{
			if (orderStack[i].isRunning())
			{
				uint8_t index = orderStack[i].getIndex();
				longOrderList[index]->onExecute();
				if (longOrderList[index]->isFinished())
				{
					std::vector<uint8_t> output_data;
					longOrderList[index]->terminate(output_data);
					Command answer(orderStack[i].getSource(), orderStack[i].getId(), output_data);
					Server.sendAnswer(answer);
					orderStack[i].deleteOrder();
				}
			}
		}
	}
	
	int addOrderToStack(uint8_t index, Command const & command)
	{
		for (size_t i = 0; i < EXEC_STACK_SIZE; i++)
		{
			if (!orderStack[i].isRunning())
			{
				orderStack[i].saveOrder(index, command);
				return 0;
			}
		}
    return -1;
	}

	OrderMemory orderStack[EXEC_STACK_SIZE];
	OrderLong* longOrderList[NB_LONG_ORDER];
	OrderImmediate* immediateOrderList[NB_IMMEDIATE_ORDER];
};


#endif

