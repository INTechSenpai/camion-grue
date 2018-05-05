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

import pfg.config.Config;
import pfg.log.Log;
import senpai.robot.Robot;
import senpai.utils.ConfigInfoSenpai;
import senpai.utils.Subject;

/**
 * Thread qui dirige les tourelles
 * 
 * @author pf
 *
 */

public class ThreadTourelles extends Thread
{
	private Robot robot;
	private Log log;
	private boolean enableTourelle;

	public ThreadTourelles(Log log, Config config, Robot robot)
	{
		this.log = log;
		this.robot = robot;
		enableTourelle = config.getBoolean(ConfigInfoSenpai.ENABLE_TOURELLE);
		setDaemon(true);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);
		try
		{
			if(!enableTourelle)
			{
				log.write("Tourelles désactivées : " + Thread.currentThread().getName()+" va dormir.", Subject.STATUS);
				while(true)
					Thread.sleep(Integer.MAX_VALUE);
			}
			while(true)
			{
				robot.updateTourelles();
				Thread.sleep(100);
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
