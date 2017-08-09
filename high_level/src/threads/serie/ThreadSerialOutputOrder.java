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

import comm.Order;
import comm.buffer.BufferOutgoingBytes;
import comm.buffer.BufferOutgoingOrder;
import exceptions.ClosedSerialException;
import pfg.config.Config;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.Subject;

/**
 * Thread qui vérifie s'il faut envoyer des ordres
 * 
 * @author pf
 *
 */

public class ThreadSerialOutputOrder extends Thread
{
	protected Log log;
	private BufferOutgoingBytes serie;
	private BufferOutgoingOrder data;
	private boolean simuleSerie;

	public ThreadSerialOutputOrder(Log log, BufferOutgoingBytes serie, BufferOutgoingOrder data, Config config)
	{
		this.log = log;
		this.serie = serie;
		this.data = data;
		simuleSerie = config.getBoolean(ConfigInfoSenpai.SIMULE_SERIE);
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
			if(simuleSerie)
				while(true)
					Thread.sleep(10000);

/*			synchronized(input)
			{
				serie.sendOrder(new Order(OutOrder.PING));
				log.debug("Ping envoyé : attente de réception");
				input.wait(); // on est notifié dès qu'on reçoit quelque chose
								// sur la série
				log.debug("Pong reçu : la connexion série est OK");
			}
			
			input.setPingDone();*/
			
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
				if(message != null)
				{
					serie.add(message.trame, message.tailleTrame);
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
