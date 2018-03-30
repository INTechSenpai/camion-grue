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

import java.util.List;

import pfg.config.Config;
import pfg.kraken.dstarlite.DStarLite;
import pfg.kraken.obstacles.Obstacle;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.Subject;
import senpai.buffer.ObstaclesBuffer;
import senpai.obstacles.ObstaclesMemory;

/**
 * Thread qui gère la péremption des obstacles en dormant
 * le temps exact entre deux péremptions.
 * 
 * @author pf
 *
 */

public class ThreadPeremption extends Thread
{
	private ObstaclesMemory memory;
	protected Log log;
//	private PrintBufferInterface buffer;
	private DStarLite dstarlite;
	private ObstaclesBuffer buffer;

	private int dureePeremption;
	private boolean printProxObs;

	public ThreadPeremption(Log log, ObstaclesMemory memory, Config config, ObstaclesBuffer buffer)
	{
		this.log = log;
		this.buffer = buffer;
		this.memory = memory;
//		this.buffer = buffer;
		this.dstarlite = dstarlite;
//		printProxObs = config.getBoolean(ConfigInfo.GRAPHIC_PROXIMITY_OBSTACLES);
		dureePeremption = config.getInt(ConfigInfoSenpai.DUREE_PEREMPTION_OBSTACLES);
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
			{
				// TODO
				synchronized(buffer)
				{
					List<Obstacle> oldObs = memory.deleteOldObstacles();
					buffer.addAllOldObstacle(oldObs);
					
					buffer.notify();
				}
				
//				if(memory.deleteOldObstacles())
//					dstarlite.updateObstaclesEnnemi();

				// mise à jour des obstacles : on réaffiche
/*				if(printProxObs)
					synchronized(buffer)
					{
						buffer.notify();
					}*/

				long prochain = memory.getNextDeathDate();

				/**
				 * S'il n'y a pas d'obstacles, on dort de dureePeremption, qui
				 * est la durée minimale avant la prochaine péremption.
				 */
				if(prochain == Long.MAX_VALUE)
					Thread.sleep(dureePeremption);
				else
					// Il faut toujours s'assurer qu'on dorme un temps positif.
					// Il y a aussi une petite marge
					Thread.sleep(Math.min(dureePeremption, Math.max(prochain - System.currentTimeMillis() + 2, 5)));
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
			e.printStackTrace(log.getPrintWriter());
			Thread.currentThread().interrupt();
		}
	}

}
