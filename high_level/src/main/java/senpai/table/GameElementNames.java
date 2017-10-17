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

package senpai.table;

import pfg.kraken.obstacles.CircularObstacle;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.utils.XY;
import senpai.capteurs.CapteursRobot;

/**
 * Enumérations contenant tous les éléments de jeux
 * 
 * @author pf
 *
 */

public enum GameElementNames
{
	DUMMY_1(new CircularObstacle(new XY(100, 200), 500)),
	DUMMY_2(new CircularObstacle(new XY(-1000, 700), 400));

	public final Obstacle obstacle;

	private GameElementNames(Obstacle obs)
	{
		obstacle = obs;
	}

	public boolean isVisible(CapteursRobot c, boolean sureleve)
	{
		// les capteurs bas les voient, les hauts non
		return !sureleve;
	}

}
