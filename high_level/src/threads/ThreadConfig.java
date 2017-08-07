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

import pfg.config.Config;
import pfg.log.Log;
import senpai.LogCategorySenpai;
import senpai.Senpai;

/**
 * S'occupe de la mise a jour de la config. Surveille config
 * 
 * @author pf
 *
 */

public class ThreadConfig extends Thread
{

	protected Log log;
	protected Config config;
	private Senpai container;

	public ThreadConfig(Log log, Config config, Senpai container)
	{
		this.log = log;
		this.container = container;
		this.config = config;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), LogCategorySenpai.DUMMY);
		try
		{
			while(true)
			{
				synchronized(config)
				{
					config.wait();
				}

				container.updateConfigForAll();
			}
		}
		catch(InterruptedException e)
		{
			log.write("Arrêt de " + Thread.currentThread().getName(), LogCategorySenpai.DUMMY);
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			log.write("Arrêt inattendu de " + Thread.currentThread().getName() + " : " + e, LogCategorySenpai.DUMMY);
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
			Thread.currentThread().interrupt();
		}
	}

}
