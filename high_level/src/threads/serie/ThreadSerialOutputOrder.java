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

import config.Config;
import config.ConfigInfo;
import container.dependances.SerialClass;
import exceptions.serie.ClosedSerialException;
import serie.BufferIncomingBytes;
import serie.BufferOutgoingOrder;
import serie.SerieCoucheTrame;
import serie.Ticket;
import serie.SerialProtocol.OutOrder;
import serie.trame.Order;
import threads.ThreadService;
import utils.Log;
import utils.Log.Verbose;

/**
 * Thread qui vérifie s'il faut envoyer des ordres
 * 
 * @author pf
 *
 */

public class ThreadSerialOutputOrder extends ThreadService implements SerialClass
{
	protected Log log;
	private SerieCoucheTrame serie;
	private BufferOutgoingOrder data;
	private int sleep;
	private BufferIncomingBytes input;
	private boolean simuleSerie;

	public ThreadSerialOutputOrder(Log log, SerieCoucheTrame serie, BufferIncomingBytes input, BufferOutgoingOrder data, Config config)
	{
		this.log = log;
		this.serie = serie;
		this.data = data;
		this.input = input;
		sleep = config.getInt(ConfigInfo.SLEEP_ENTRE_TRAMES);
		simuleSerie = config.getBoolean(ConfigInfo.SIMULE_SERIE);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.debug("Démarrage de " + Thread.currentThread().getName());
		Order message;

		// On envoie d'abord le ping long initial
		try
		{
			if(simuleSerie)
				while(true)
					Thread.sleep(10000);

			serie.init();
			Thread.sleep(50); // on attend que la série soit bien prête
			synchronized(input)
			{
				serie.sendOrder(new Order(OutOrder.PING));
				log.debug("Ping envoyé : attente de réception");
				input.wait(); // on est notifié dès qu'on reçoit quelque chose
								// sur la série
				log.debug("Pong reçu : la connexion série est OK");
			}
			
			input.setPingDone();
			Ticket t = new Ticket();
			
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
						data.wait(500);

					if(data.isEmpty()) // si c'est le timeout qui nous a
										// réveillé, on envoie un ping
					{
						synchronized(t)
						{
							if(!t.isEmpty())
							{
								t.attendStatus(); // pas besoin d'attendre
								message = new Order(OutOrder.PING, t);
								log.debug("Envoi d'un ping pour vérifier la connexion", Verbose.SERIE.masque);
							}
						}
					}
					else
						message = data.poll();
				}
				if(message != null)
				{
					serie.sendOrder(message);
					Thread.sleep(sleep); // laisse un peu de temps entre deux trames
											// si besoin est
				}
			}
		}
		catch(InterruptedException | ClosedSerialException e)
		{
			log.debug("Arrêt de " + Thread.currentThread().getName()+" : "+e);
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			log.debug("Arrêt inattendu de " + Thread.currentThread().getName() + " : " + e);
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
			Thread.currentThread().interrupt();
		}
	}

}
