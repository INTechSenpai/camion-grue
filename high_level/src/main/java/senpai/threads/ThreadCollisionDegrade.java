/*
 * Copyright (C) 2013-2018 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package senpai.threads;

import pfg.log.Log;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.obstacles.ObstaclesDynamiques;
import senpai.robot.Robot;
import senpai.utils.Severity;
import senpai.utils.Subject;
import java.util.ArrayList;
import java.util.List;

import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.utils.XY;

/**
 * Thread qui s'occupe de la détection de collisions
 * 
 * @author pf
 *
 */

public final class ThreadCollisionDegrade extends Thread
{
	protected Log log;
	private ObstaclesDynamiques dynObs;
	private Robot robot;
	private OutgoingOrderBuffer out;
	private List<RectangularObstacle> initialObstacles = new ArrayList<RectangularObstacle>();
	private RectangularObstacle[] memory;


	public ThreadCollisionDegrade(Log log, ObstaclesDynamiques dynObs, Robot robot, OutgoingOrderBuffer out, RectangularObstacle vehicleTemplate)
	{
		this.dynObs = dynObs;
		this.log = log;
		this.robot = robot;
		this.out = out;
		memory = new RectangularObstacle[2000];
		for(int i = 0; i < memory.length; i++)
			memory[i] = vehicleTemplate.clone();
		setDaemon(true);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		try
		{
/*			synchronized(robot)
			{
				while(!robot.isDegrade())
					robot.wait();
			}
			log.write("Activation du mode dégradé : gestion des collisions démarré.", Subject.STATUS);
	*/		
			List<ItineraryPoint> currentPath;
			while(true)
			{
				synchronized(robot)
				{
					while(!robot.needCollisionCheck())
						robot.wait();
					
//					log.write("Démarrage check collision.", Subject.STATUS);

					currentPath = robot.getPath();
					
					initialObstacles.clear();
					int i = 0;
					for(ItineraryPoint ip : currentPath)
					{
						memory[i].update(ip.x, ip.y, ip.orientation);
						initialObstacles.add(memory[i]);
						i++;
					}
				}				
				
				/*
				 * On attend d'avoir des obstacles à vérifier
				 */
				synchronized(dynObs)
				{
//					log.write("Attente obstacles.", Subject.STATUS);

					while(!dynObs.needCollisionCheck())
						dynObs.wait();
					
//					log.write("Obstacle !", Subject.STATUS);

				}

				synchronized(robot)
				{
//					log.write("Vérification collision.", Subject.STATUS);

					if(robot.needCollisionCheck() && dynObs.isThereCollision(initialObstacles) != currentPath.size())
					{
						robot.setStopping();
						log.write("Collision détectée : arrêt nécessaire.", Severity.CRITICAL, Subject.STATUS);
						out.immobilise();
					}
					
//					log.write("Vérification finie.", Subject.STATUS);

				}
			}
		}
		catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}

}
