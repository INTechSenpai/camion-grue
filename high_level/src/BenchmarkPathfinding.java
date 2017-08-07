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

import container.Container;
import exceptions.ContainerException;
import exceptions.MemoryManagerException;
import exceptions.PathfindingException;
import pathfinding.RealGameState;
import pathfinding.astar.AStarCourbe;
import pathfinding.astar.arcs.CercleArrivee;
import pathfinding.chemin.CheminPathfinding;
import robot.Cinematique;
import robot.RobotReal;
import table.GameElementNames;
import utils.Log;

/**
 * Un benchmark du pathfinding courbe
 * 
 * @author pf
 *
 */

public class BenchmarkPathfinding
{

	public static void main(String[] args)
	{
		try
		{
			Container container = new Container();
			Log log = container.getService(Log.class);
			RobotReal robot = container.getService(RobotReal.class);
			AStarCourbe astar = container.getService(AStarCourbe.class);
			RealGameState state = container.getService(RealGameState.class);
			CheminPathfinding chemin = container.getService(CheminPathfinding.class);
			CercleArrivee cercle = container.getService(CercleArrivee.class);

			long avant = System.nanoTime();
			Cinematique depart = new Cinematique(-800, 350, Math.PI / 2, true, 0);
			robot.setCinematique(depart);
			cercle.set(GameElementNames.MINERAI_CRATERE_HAUT_GAUCHE, 230, 30, -30, 10, -10);
			int nbtest = 1000;
			log.debug("Début du test…");
			for(int i = 0; i < nbtest; i++)
			{
				astar.initializeNewSearchToCircle(true, state);
				astar.process(chemin, false);
				chemin.clear();
			}
			log.debug("Temps : " + (System.nanoTime() - avant) / (nbtest * 1000000.));
			container.destructor();
		}
		catch(PathfindingException | InterruptedException | ContainerException | MemoryManagerException e)
		{
			e.printStackTrace();
		}
	}

}
