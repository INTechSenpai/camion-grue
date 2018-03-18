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

import java.util.ArrayList;
import java.util.List;

import pfg.kraken.astar.TentacularAStar;
import pfg.kraken.obstacles.Obstacle;
import pfg.log.Log;
import senpai.Severity;
import senpai.Subject;
import senpai.buffer.ObstaclesBuffer;
import senpai.buffer.OutgoingOrderBuffer;

/**
 * Thread qui recalcule l'itinéraire à emprunter. Surveille CheminPathfinding.
 * 
 * @author pf
 *
 */

public class ThreadUpdatePathfinding extends Thread
{
	protected Log log;
	private TentacularAStar pathfinding;
//	private CheminPathfinding chemin;
	private OutgoingOrderBuffer out;
	private ObstaclesBuffer obsbuffer;

	public ThreadUpdatePathfinding(Log log, TentacularAStar pathfinding, /*CheminPathfinding chemin,*/ OutgoingOrderBuffer out, ObstaclesBuffer obsbuffer)
	{
		this.obsbuffer = obsbuffer;
		this.log = log;
		this.pathfinding = pathfinding;
//		this.chemin = chemin;
		this.out = out;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);
		List<Obstacle> newObs = new ArrayList<Obstacle>();
		List<Obstacle> oldObs = new ArrayList<Obstacle>();
		
		try
		{
			while(true)
			{
				synchronized(obsbuffer)
				{
					if(obsbuffer.needToWait())
						obsbuffer.wait();
					
					while(!obsbuffer.isNewObstaclesEmpty())
						newObs.add(obsbuffer.pollNewObstacle());

					while(!obsbuffer.isOldObstaclesEmpty())
						oldObs.add(obsbuffer.pollOldObstacle());
				}
				
//				pathfinding.updateObstacles(newObs, oldObs);
				
/*				synchronized(chemin)
				{
					if(chemin.isUptodate())
						chemin.wait();
				}

				// on a été prévenu que le chemin n'est plus à jour
				// : ralentissement et replanification
				try
				{
					if(chemin.needStop())
						throw new PathfindingException("Trajectoire vide");
					Cinematique lastValid = chemin.getLastValidCinematique();
					out.setMaxSpeed(Speed.REPLANIF, chemin.getCurrentMarcheAvant());
					log.debug("Mise à jour du chemin", Verbose.REPLANIF.masque);
					pathfinding.updatePath(lastValid);
					out.setMaxSpeed(Speed.STANDARD, chemin.getCurrentMarcheAvant());
				}
				catch(PathfindingException e)
				{
					log.critical(e);
//					if(!chemin.isEmpty())
					out.immobilise();
//					else
//						log.debug("Robot déjà à l'arrêt", Verbose.REPLANIF.masque);
					chemin.clear();
				}*/
			}
		}
		catch(InterruptedException e)
		{
			log.write("Arrêt de " + Thread.currentThread().getName(), Subject.STATUS);
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			log.write("Exception inattendue dans " + Thread.currentThread().getName() + " : " + e, Severity.CRITICAL, Subject.STATUS);
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
/*			try
			{
				while(true)
				{
					synchronized(chemin)
					{
						if(chemin.isUptodate())
							chemin.wait();
					}
					chemin.clear();
					out.immobilise();
				}
			}
			catch(InterruptedException e1)*/
			{
				log.write("Arrêt de " + Thread.currentThread().getName() + " après une exception inattendue récupérée", Subject.DUMMY);
				Thread.currentThread().interrupt();
			}
		}
	}

}