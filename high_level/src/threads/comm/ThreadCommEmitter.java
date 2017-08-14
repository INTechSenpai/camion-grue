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

package threads.comm;

import buffer.OutgoingOrderBuffer;
import comm.Communication;
import comm.Order;
import pfg.config.Config;
import pfg.log.Log;
import senpai.Subject;

/**
 * Thread qui vérifie s'il faut envoyer des ordres
 * 
 * @author pf
 *
 */

public class ThreadCommEmitter extends Thread
{
	protected Log log;
	private Communication serie;
	private OutgoingOrderBuffer data;

	public ThreadCommEmitter(Log log, Communication serie, OutgoingOrderBuffer data, Config config)
	{
		this.log = log;
		this.serie = serie;
		this.data = data;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.DUMMY);
		Order message;

		// On envoie d'abord le ping long initial
		try
		{
			serie.waitForInitialization();
			
			while(true)
			{
				synchronized(data)
				{
					/**
					 * Pour désactiver le ping automatique, remplacer
					 * "data.wait(500)" par "data.wait()"
					 */

					message = null;
					if(data.isEmpty()) // pas de message ? On attend
						data.wait();

					message = data.poll();
				}
				serie.communiquer(message);
			}
		}
		catch(InterruptedException e)
		{
			log.write("Arrêt de " + Thread.currentThread().getName(), Subject.DUMMY);
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
