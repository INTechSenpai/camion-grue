package senpai;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.kraken.Kraken;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
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
 * Construit une trajectoire et l'enregistre.
 * @author pf
 *
 */

public class ConstructTrajectory
{

	public static void main(String[] args)
	{
		if(args.length != 6)
		{
			System.out.println("Usage : ./run.sh "+ConstructTrajectory.class.getSimpleName()+" x_depart y_depart o_depart x_arrivee y_arrivee output.path");
			return;
		}
		
		double x = Double.parseDouble(args[0]);
		double y = Double.parseDouble(args[1]);
		double o = Double.parseDouble(args[2]);
		XYO depart = new XYO(x,y,o);
		
		x = Double.parseDouble(args[3]);
		y = Double.parseDouble(args[4]);
//		o = Double.parseDouble(args[5]);
		XY arrivee = new XY(x,y);
		
		String output = args[5];
		
		List<Obstacle> obsList = new ArrayList<Obstacle>();

		for(ObstaclesFixes obs : ObstaclesFixes.values())
			obsList.add(obs.obstacle);
		
		Config config = new Config(ConfigInfoSenpai.values(), false, "senpai.conf", "default");
		int demieLargeurNonDeploye = config.getInt(ConfigInfoSenpai.LARGEUR_NON_DEPLOYE) / 2;
		int demieLongueurArriere = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		int demieLongueurAvant = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);

		RectangularObstacle robotTemplate = new RectangularObstacle(demieLargeurNonDeploye, demieLargeurNonDeploye, demieLongueurArriere, demieLongueurAvant);
		Kraken kraken = new Kraken(robotTemplate, obsList, new XY(-1500,0), new XY(1500, 2000), "default", "noLL");
		
		GraphicDisplay display = kraken.getGraphicDisplay();

		for(Obstacle obs : obsList)
			display.addPrintable(obs, Color.BLACK, Layer.MIDDLE.layer);
		display.refresh();
		try
		{
			kraken.initializeNewSearch(depart, arrivee);
			List<ItineraryPoint> path = kraken.search();
			for(ItineraryPoint p : path)
				display.addPrintable(p, Color.BLACK, Layer.FOREGROUND.layer);
			display.refresh();
			KnownPathManager.savePath(output, path);
		}
		catch(PathfindingException e)
		{
			e.printStackTrace();
		}
	}
	
}
