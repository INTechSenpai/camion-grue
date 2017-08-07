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

package table;

import capteurs.CapteursRobot;
import pfg.kraken.obstacles.Obstacle;

/**
 * Enumérations contenant tous les éléments de jeux
 * 
 * @author pf
 *
 */

public enum GameElementNames
{
	;

	public final Obstacle obstacle; // il se trouve qu'ils sont tous
												// circulaires…
	public final boolean cylindre;
	public final double orientationArriveeDStarLite;
	public final Double[] anglesAttaque;

	private GameElementNames(Obstacle obs, double orientationArriveeDStarLite, Double[] anglesAttaque)
	{
		this.anglesAttaque = anglesAttaque;
		cylindre = toString().startsWith("CYLINDRE");
		obstacle = obs;
		this.orientationArriveeDStarLite = orientationArriveeDStarLite;
	}

	public boolean isVisible(CapteursRobot c, boolean sureleve)
	{
		// cas particulier
		if(c == CapteursRobot.ToF_LONG_AVANT && cylindre)
			return true;
		// les capteurs bas les voient, les hauts non
		return !sureleve;
	}

}
