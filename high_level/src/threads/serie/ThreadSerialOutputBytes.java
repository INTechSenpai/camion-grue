/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package threads.serie;

import exceptions.serie.ClosedSerialException;
import pfg.log.Log;
import senpai.Subject;
import serie.BufferOutgoingBytes;

/**
 * Thread qui vérifie s'il faut envoyer des choses sur la série
 * 
 * @author pf
 *
 */

public class ThreadSerialOutputBytes extends Thread
{
	protected Log log;
	private BufferOutgoingBytes serie;

	public ThreadSerialOutputBytes(Log log, BufferOutgoingBytes serie)
	{
		this.log = log;
		this.serie = serie;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.DUMMY);

		try
		{
			while(true)
			{
				synchronized(serie)
				{
					if(serie.isEmpty()) // pas de message ? On attend
						serie.wait();
					serie.send();
				}
			}
		}
		catch(InterruptedException | ClosedSerialException e)
		{
			log.write("Arrêt de " + Thread.currentThread().getName()+" : "+e, Subject.DUMMY);
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			log.write("Arrêt inattendu de " + Thread.currentThread().getName() + " : " + e, Subject.DUMMY);
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
			Thread.currentThread().interrupt();
		}
	}

}
