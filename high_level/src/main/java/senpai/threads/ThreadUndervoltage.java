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
import senpai.utils.GPIO;
import senpai.utils.Severity;
import senpai.utils.Subject;

/**
 * Thread qui signale les pertes de tension
 * @author pf
 *
 */

public class ThreadUndervoltage extends Thread
{
	private Log log;
	
	public ThreadUndervoltage(Log log)
	{
		this.log = log;
	}
	
	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);
		try
		{
			boolean last = false;
			while(true)
			{
				if(GPIO.waitUndervoltage())
				{
					if(!last)
						log.write("Raspberry pi en sous-tension !", Severity.CRITICAL, Subject.STATUS);
					last = true;
				}
				else
				{
					if(last)
						log.write("Raspberry pi à tension nominale.", Severity.WARNING, Subject.STATUS);
					last = false;
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