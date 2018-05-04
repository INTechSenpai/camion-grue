/*
 * Copyright (C) 2013-2018 Pierre-François Gimenez
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

import java.awt.Color;
import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.kraken.astar.autoreplanning.DynamicPath;
import pfg.kraken.astar.autoreplanning.PathDiff;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.robot.ItineraryPoint;
import pfg.log.Log;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.robot.KnownPathManager;
import senpai.robot.Robot;
import senpai.utils.ConfigInfoSenpai;
import senpai.utils.Severity;
import senpai.utils.Subject;

/**
 * Thread qui gère l'envoi des trajectoires
 * 
 * @author pf
 *
 */

public class ThreadKraken extends Thread
{
	private DynamicPath dpath;
	private OutgoingOrderBuffer data;
	private GraphicDisplay display;
	private Robot robot;
	private KnownPathManager known;
	private boolean simuleLL, graphic;
	
	protected Log log;

	public ThreadKraken(Log log, Config config, DynamicPath dpath, OutgoingOrderBuffer data, GraphicDisplay display, Robot robot, KnownPathManager known)
	{
		this.log = log;
		this.data = data;
		this.dpath = dpath;
		this.display = display;
		this.robot = robot;
		this.known = known;
		simuleLL = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);
		graphic = config.getBoolean(ConfigInfoSenpai.GRAPHIC_PATH);
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
				try {
					PathDiff diff = dpath.waitDiff();					

					if(graphic)
						for(ItineraryPoint p : diff.diff)
						{
//							System.out.println(p);
							display.addPrintable(p, p.stop ? Color.BLUE : Color.BLACK, Layer.FOREGROUND.layer);
						}

/*					if(robot.isDegrade())
					{
						log.write("Mode dégradé activé, "+Thread.currentThread().getName()+" entre en sommeil.", Subject.STATUS);
						while(true)
							Thread.sleep(Integer.MAX_VALUE);
					}*/
					
					known.addPath(diff.diff);
					if(!simuleLL)
					{
						data.destroyPointsTrajectoires(diff.firstDifferentPoint);
						if(diff.firstDifferentPoint != diff.diff.size())
							data.ajoutePointsTrajectoire(diff.diff.subList(diff.firstDifferentPoint, diff.diff.size()), diff.isComplete);
					}
					
					if(robot.isStandby())
					{
						log.write("Robot en standby.", Subject.TRAJECTORY);
						// chemin initial, il est complet
						assert diff.isComplete;
						robot.setReady();
					}

				} catch(PathfindingException e)
				{
					log.write("On s'arrête à cause d'une exception "+e, Severity.CRITICAL, Subject.TRAJECTORY);
					data.immobilise();
				}
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
			Thread.currentThread().interrupt();
		}
	}

}