/*
 Name:		teensy_com_socket.ino
 Created:	15/07/2017 17:30:33
 Author:	Sylvain
*/

#include "Command.h"
#include "CommunicationServer.h"

#define START_ON_SERIAL 1


void setup() 
{
#if START_ON_SERIAL
	while (!Serial);
#endif
}


void loop() 
{
	Server.communicate();

	if (Server.available())
	{
		Command command = Server.getLastCommand();
		if (command.isValid())
		{
			Server.print(command);
		}
		else
		{
			Server.printf_err("Invalid command\n");
		}
	}

}


/* Ce bout de code permet de compiler avec std::vector */
namespace std {
	void __throw_bad_alloc()
	{
		while (true)
		{
			Serial.println("Unable to allocate memory");
			delay(500);
		}
	}

	void __throw_length_error(char const*e)
	{
		while (true)
		{
			Serial.println(e);
			delay(500);
		}
	}

	void __throw_out_of_range(char const*e)
	{
		while (true)
		{
			Serial.println(e);
			delay(500);
		}
	}

	void __throw_out_of_range_fmt(char const*e, ...)
	{
		while (true)
		{
			Serial.println(e);
			delay(500);
		}
	}
}
