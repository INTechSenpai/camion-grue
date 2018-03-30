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

package senpai.threads.comm;

import pfg.log.Log;
import senpai.Subject;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.Communication;

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

	public ThreadCommEmitter(Log log, Communication serie, OutgoingOrderBuffer data)
	{
		this.log = log;
		this.serie = serie;
		this.data = data;
		setDaemon(true);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);

		try
		{
			serie.waitForInitialization();
			
			while(true)
			{
				serie.communiquer(data.take());
			}
		}
		catch(InterruptedException e)
		{
			log.write("Arrêt de " + Thread.currentThread().getName(), Subject.STATUS);
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			log.write("Arrêt inattendu de " + Thread.currentThread().getName() + " : " + e, Subject.STATUS);
			e.printStackTrace();
//			e.printStackTrace(log.getPrintWriter());
			Thread.currentThread().interrupt();
		}
	}

}
