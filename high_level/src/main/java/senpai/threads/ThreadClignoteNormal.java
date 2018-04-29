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

import pfg.log.Log;
import senpai.robot.Robot;
import senpai.utils.GPIO;
import senpai.utils.Subject;

/**
 * Thread qui clignote en mode normal
 * @author pf
 *
 */

public class ThreadClignoteNormal extends Thread
{
	private Log log;
	private Robot robot;
	
	public ThreadClignoteNormal(Log log, Robot robot)
	{
		this.log = log;
		this.robot = robot;
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
				GPIO.allumeDiode();
				Thread.sleep(100);
				GPIO.eteintDiode();
				Thread.sleep(4900);
				synchronized(robot)
				{
					// On vérifie régulièrement si le robot est entré en mode dégradé
					if(robot.isDegrade())
						break;
				}
			}
			log.write("Activation du mode dégradé : clignotement lent arrêté", Subject.STATUS);
			while(true)
				Thread.sleep(Integer.MAX_VALUE);
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