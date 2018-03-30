package senpai;

import pfg.config.Config;
import pfg.kraken.Kraken;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.log.Log;
import senpai.threads.ThreadWarmUp;

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

/**
 * Benchmark
 * @author pf
 *
 */

public class Benchmark
{
	public static void main(String[] args)
	{
		String configfile = "senpai-trajectory.conf";
		Log log = new Log(Severity.INFO, configfile, "default");
		Config config = new Config(ConfigInfoSenpai.values(), false, configfile, "default");
		
		if(args.length > 0)
		{
			int duree = Integer.parseInt(args[0]);
			config.override(ConfigInfoSenpai.WARM_UP_DURATION, duree);
		}
				
		log.write("Durée du warm-up : "+config.getInt(ConfigInfoSenpai.WARM_UP_DURATION), Subject.STATUS);
		int demieLargeurNonDeploye = config.getInt(ConfigInfoSenpai.LARGEUR_NON_DEPLOYE) / 2;
		int demieLongueurArriere = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		int demieLongueurAvant = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);

		RectangularObstacle robotTemplate = new RectangularObstacle(demieLargeurNonDeploye, demieLargeurNonDeploye, demieLongueurArriere, demieLongueurAvant);

		Kraken kraken = new Kraken(robotTemplate, null, new XY(-1500, 0), new XY(1500, 2000), configfile, "default", "nolog");
		ThreadWarmUp warmUp = new ThreadWarmUp(log, kraken, config);
		warmUp.run();
		
		try
		{
			long before = System.currentTimeMillis();
			for(int i = 0; i < 1000; i++)
			{
				kraken.initializeNewSearch(new XYO(-500, 700, 2./3.*Math.PI), new XY(1000, 1300));
				kraken.search();
			}
			long after = System.currentTimeMillis();
			System.out.println("Durée : "+(after - before) / 1000.);
		}
		catch(PathfindingException e)
		{
			log.write("Erreur lors du benchmark : "+e, Severity.CRITICAL, Subject.STATUS);
		}

	}
}
