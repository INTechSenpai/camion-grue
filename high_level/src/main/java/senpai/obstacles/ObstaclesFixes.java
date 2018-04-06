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

package senpai.obstacles;

import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.utils.XY;

/**
 * Enumération des obstacles fixes.
 * Afin que les obstacles fixes soient facilement modifiables d'une coupe à
 * l'autre.
 * 
 * @author pf
 *
 */

public enum ObstaclesFixes
{
	// bords
	BORD_BAS(new RectangularObstacle(new XY(0, 0), 3000, 5), true, false),
	BORD_GAUCHE(new RectangularObstacle(new XY(-1500, 1000), 5, 2000), true, false),
	BORD_DROITE(new RectangularObstacle(new XY(1500, 1000), 5, 2000), true, false),
	BORD_HAUT(new RectangularObstacle(new XY(0, 2000), 3000, 5), true, false),

	BAC_EPURATION(new RectangularObstacle(new XY(0, 250 / 2), 1200, 250), true, false),

	DISTRIBUTEUR_MONOCOLOR_DROIT(new RectangularObstacle(new XY(1450, 2000-840), 100, 55), false, true),
	DISTRIBUTEUR_MONOCOLOR_GAUCHE(new RectangularObstacle(new XY(-1450, 2000-840), 100, 55), false, true),
	
	DISTRIBUTEUR_BICOLOR_GAUCHE(new RectangularObstacle(new XY(-890, 50), 55, 100), false, true),
	DISTRIBUTEUR_BICOLOR_DROIT(new RectangularObstacle(new XY(890, 50), 55, 100), false, true),

//	ZONE_DEPART_GAUCHE(new RectangularObstacle(new XY(1300, 1675), 400, 650), false, false),
//	ZONE_DEPART_DROITE(new RectangularObstacle(new XY(-1300, 1675), 400, 650), false, false),

	ZONE_CONSTRUCTION_GAUCHE(new RectangularObstacle(new XY(1100-560/2, 2000-180/2), 560, 180), false, false),
	ZONE_CONSTRUCTION_DROITE(new RectangularObstacle(new XY(-1100+560/2, 2000-180/2), 560, 180), false, false);
	
	public final Obstacle obstacle;
	private final boolean[] visible = new boolean[2];

	private ObstaclesFixes(Obstacle obstacle, boolean visibleBas, boolean visibleHaut)
	{
		this.obstacle = obstacle;
		visible[0] = visibleBas;
		visible[1] = visibleHaut;
	}
	
	/**
	 * Cet obstacle est-il visible pour un capteur surélevé ou non ?
	 * 
	 * @param sureleve
	 * @return
	 */
	public boolean isVisible(boolean sureleve)
	{
		return visible[sureleve ? 1 : 0];
	}

}