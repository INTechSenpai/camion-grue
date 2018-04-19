/*
 * Copyright (C) 2013-2018 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package senpai.threads;

import pfg.log.Log;
import senpai.Subject;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.obstacles.ObstaclesDynamiques;
import senpai.robot.Robot;

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
	private RectangularObstacle vehicleTemplate;

	public ThreadCollisionDegrade(Log log, ObstaclesDynamiques dynObs, Robot robot, OutgoingOrderBuffer out, RectangularObstacle vehicleTemplate)
	{
		this.dynObs = dynObs;
		this.log = log;
		this.robot = robot;
		this.out = out;
		this.vehicleTemplate = vehicleTemplate;
		setDaemon(true);
		setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		try
		{
			synchronized(robot)
			{
				while(!robot.isDegrade())
					robot.wait();
			}
			log.write("Activation du mode dégradé : gestion des collisions démarré.", Subject.STATUS);
			
			List<ItineraryPoint> currentPath;
			while(true)
			{
				synchronized(robot)
				{
					while(!robot.needCollisionCheck())
						robot.wait();
					currentPath = robot.getPath();
					
					initialObstacles.clear();
					for(ItineraryPoint ip : currentPath)
					{
						RectangularObstacle o = vehicleTemplate.clone();
						o.update(new XY(ip.x, ip.y), ip.orientation);
						initialObstacles.add(o);
					}
				}
				log.write("Activation du mode dégradé : gestion des collisions démarré.", Subject.STATUS);
				
				
				/*
				 * On attend que la vérification de collision soit nécessaire
				 */
				synchronized(robot)
				{
					while(!robot.needCollisionCheck())
						robot.wait();
				}
				
				/*
				 * On attend d'avoir des obstacles à vérifier
				 */
				synchronized(dynObs)
				{
					while(!dynObs.needCollisionCheck())
						dynObs.wait();
				}

				synchronized(robot)
				{
					if(robot.needCollisionCheck() && dynObs.isThereCollision(initialObstacles) != currentPath.size())
						out.immobilise();
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
