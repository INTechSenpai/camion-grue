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

package threads.serie;

import config.Config;
import config.ConfigInfo;
import container.dependances.SerialClass;
import exceptions.serie.ClosedSerialException;
import serie.SerieCoucheTrame;
import threads.ThreadService;
import utils.Log;

/**
 * Thread qui permet de faire gaffe au timeout de la série bas niveau
 * 
 * @author pf
 *
 */

public class ThreadSerialOutputTimeout extends ThreadService implements SerialClass
{
	protected Log log;
	private SerieCoucheTrame serie;
	private int sleep;

	public ThreadSerialOutputTimeout(Log log, SerieCoucheTrame serie, Config config)
	{
		this.log = log;
		this.serie = serie;
		sleep = config.getInt(ConfigInfo.SLEEP_ENTRE_TRAMES);
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
				int timeResend = serie.timeBeforeResend();
				int timeDeath = serie.timeBeforeDeath();

				if(timeDeath <= timeResend)
				{
					Thread.sleep(timeDeath + sleep);
					serie.kill();
				}
				else
				{
					Thread.sleep(timeResend + sleep);
					serie.resend();
				}
			}
		}
		catch(InterruptedException | ClosedSerialException e)
		{
			log.debug("Arrêt de " + Thread.currentThread().getName()+" : "+e);
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
