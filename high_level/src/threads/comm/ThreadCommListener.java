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

import buffer.IncomingOrderBuffer;
import comm.Communication;
import pfg.graphic.log.Log;
import senpai.Subject;

/**
 * Thread qui s'occupe de la partie bas niveau du protocole série
 * 
 * @author pf
 *
 */

public class ThreadCommListener extends Thread
{

	protected Log log;
	private Communication serie;
	private IncomingOrderBuffer buffer;
	
	public ThreadCommListener(Log log, Communication serie, IncomingOrderBuffer buffer)
	{
		this.log = log;
		this.serie = serie;
		this.buffer = buffer;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.DUMMY);
		try
		{
			serie.waitForInitialization();
			while(true)
				buffer.add(serie.readPaquet());
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
