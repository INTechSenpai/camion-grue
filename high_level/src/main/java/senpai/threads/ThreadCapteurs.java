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

package senpai.threads;

import pfg.log.Log;
import senpai.Subject;
import senpai.buffer.SensorsDataBuffer;
import senpai.capteurs.CapteursProcess;

/**
 * Thread qui gère les entrées des capteurs
 * 
 * @author pf
 *
 */

public class ThreadCapteurs extends Thread
{
	private SensorsDataBuffer buffer;
	private CapteursProcess capteurs;

	protected Log log;

	public ThreadCapteurs(Log log, SensorsDataBuffer buffer, CapteursProcess capteurs)
	{
		this.log = log;
		this.buffer = buffer;
		this.capteurs = capteurs;
		setDaemon(true);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);
		try
		{
			while(true)
				capteurs.updateObstaclesMobiles(buffer.take());
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
			Thread.currentThread().interrupt();
		}
	}

}