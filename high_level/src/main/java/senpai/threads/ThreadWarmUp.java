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

import pfg.config.Config;
import pfg.kraken.Kraken;
import pfg.kraken.SearchParameters;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.Severity;
import senpai.Subject;

/**
 * Thread d'échauffement de la JVM
 * 
 * @author pf
 *
 */

public class ThreadWarmUp extends Thread
{
	private Kraken k;
	private Log log;
	private int dureeWarmUp;

	public ThreadWarmUp(Log log, Kraken k, Config config)
	{
		this.log = log;
		this.k = k;
		dureeWarmUp = config.getInt(ConfigInfoSenpai.WARM_UP_DURATION);
		setDaemon(true);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);
		try
		{
			long before = System.currentTimeMillis();
			do {
				if(Thread.currentThread().isInterrupted())
				{
					log.write("Arrêt prématuré de " + Thread.currentThread().getName(), Subject.STATUS);
					Thread.currentThread().interrupt();
				}
				k.initializeNewSearch(new SearchParameters(new XYO(-500, 1000, Math.PI), new XY(1000, 1000)));
				k.search();
			} while(System.currentTimeMillis() - before < dureeWarmUp);
			log.write("Échauffement de la JVM terminé", Subject.STATUS);
		}
		catch(PathfindingException e)
		{
			log.write("Erreur lors de l'échauffement de la JVM : "+e, Severity.CRITICAL, Subject.STATUS);
		}

	}

}