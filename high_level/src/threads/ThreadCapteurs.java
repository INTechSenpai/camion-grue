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

package threads;

import capteurs.CapteursProcess;
import capteurs.SensorsData;
import capteurs.SensorsDataBuffer;
import container.dependances.HighPFClass;
import container.dependances.LowPFClass;
import utils.Log;

/**
 * Thread qui gère les entrées des capteurs
 * 
 * @author pf
 *
 */

public class ThreadCapteurs extends ThreadService implements LowPFClass, HighPFClass
{
	private SensorsDataBuffer buffer;
	private CapteursProcess capteurs;

	protected Log log;

	public ThreadCapteurs(Log log, SensorsDataBuffer buffer, CapteursProcess capteurs)
	{
		this.log = log;
		this.buffer = buffer;
		this.capteurs = capteurs;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.debug("Démarrage de " + Thread.currentThread().getName());
		try
		{
			while(true)
			{
				SensorsData e;
				synchronized(buffer)
				{
					if(buffer.isEmpty())
						buffer.wait();
					e = buffer.poll();
				}
				capteurs.updateObstaclesMobiles(e);

			}
		}
		catch(InterruptedException e)
		{
			log.debug("Arrêt de " + Thread.currentThread().getName());
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