package senpai;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.kraken.Kraken;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.utils.XY;
import pfg.log.Log;
import senpai.obstacles.ObstaclesFixes;

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
 * Affiche une trajectoire déjà calculée
 * @author pf
 *
 */

public class DisplayTrajectory
{

	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.out.println("Usage : ./run.sh "+DisplayTrajectory.class.getSimpleName()+" chemin");
			return;
		}
		
		String configfile = "senpai-trajectory.conf";
		String filename = args[0];
		List<ItineraryPoint> path = KnownPathManager.loadPath(filename);
		
		Config config = new Config(ConfigInfoSenpai.values(), false); // valeur par défaut seulement
		int demieLargeurNonDeploye = config.getInt(ConfigInfoSenpai.LARGEUR_NON_DEPLOYE) / 2;
		int demieLongueurArriere = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		int demieLongueurAvant = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);

		Log log = new Log(Severity.INFO, configfile, "log");
		
		List<Obstacle> obsList = new ArrayList<Obstacle>();

		for(ObstaclesFixes obs : ObstaclesFixes.values())
			obsList.add(obs.obstacle);
		
		RectangularObstacle robotTemplate = new RectangularObstacle(demieLongueurAvant, demieLongueurArriere, demieLargeurNonDeploye, demieLargeurNonDeploye);
		Kraken kraken = new Kraken(robotTemplate, obsList, new XY(-1500,0), new XY(1500, 2000), configfile, "default");
		GraphicDisplay display = kraken.getGraphicDisplay();
		
		for(ItineraryPoint p : path)
		{
			log.write(p, Subject.STATUS);
			display.addPrintable(p, Color.BLACK, Layer.FOREGROUND.layer);
		}
		display.refresh();

	}
	
}
