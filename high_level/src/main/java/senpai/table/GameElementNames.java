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

import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.utils.XY;

/**
 * Enumérations contenant tous les éléments de jeux
 * 
 * @author pf
 *
 */

public enum GameElementNames
{
	CROIX_1_CENTRE(new RectangularObstacle(new XY(-650, 1460), 58, 58), ElementColor.JAUNE),
	CROIX_1_GAUCHE(new RectangularObstacle(new XY(-650-58, 1460), 58, 58), ElementColor.VERT),
	CROIX_1_DROITE(new RectangularObstacle(new XY(-650+58, 1460), 58, 58), ElementColor.ORANGE),
	CROIX_1_HAUT(new RectangularObstacle(new XY(-650, 1460+58), 58, 58), ElementColor.NOIR),
	CROIX_1_BAS(new RectangularObstacle(new XY(-650, 1460-58), 58, 58), ElementColor.BLEU),

	CROIX_2_CENTRE(new RectangularObstacle(new XY(-400, 500), 58, 58), ElementColor.JAUNE),
	CROIX_2_GAUCHE(new RectangularObstacle(new XY(-400-58, 500), 58, 58), ElementColor.VERT),
	CROIX_2_DROITE(new RectangularObstacle(new XY(-400+58, 500), 58, 58), ElementColor.ORANGE),
	CROIX_2_HAUT(new RectangularObstacle(new XY(-400, 500+58), 58, 58), ElementColor.NOIR),
	CROIX_2_BAS(new RectangularObstacle(new XY(-400, 500-58), 58, 58), ElementColor.BLEU),

	CROIX_3_CENTRE(new RectangularObstacle(new XY(-1200, 810), 58, 58), ElementColor.JAUNE),
	CROIX_3_GAUCHE(new RectangularObstacle(new XY(-1200-58, 810), 58, 58), ElementColor.VERT),
	CROIX_3_DROITE(new RectangularObstacle(new XY(-1200+58, 810), 58, 58), ElementColor.ORANGE),
	CROIX_3_HAUT(new RectangularObstacle(new XY(-1200, 810+58), 58, 58), ElementColor.NOIR),
	CROIX_3_BAS(new RectangularObstacle(new XY(-1200, 810-58), 58, 58), ElementColor.BLEU),
	
	
	CROIX_4_CENTRE(new RectangularObstacle(new XY(650, 1460), 58, 58), ElementColor.JAUNE),
	CROIX_4_GAUCHE(new RectangularObstacle(new XY(650-58, 1460), 58, 58), ElementColor.ORANGE),
	CROIX_4_DROITE(new RectangularObstacle(new XY(650+58, 1460), 58, 58), ElementColor.VERT),
	CROIX_4_HAUT(new RectangularObstacle(new XY(650, 1460+58), 58, 58), ElementColor.NOIR),
	CROIX_4_BAS(new RectangularObstacle(new XY(650, 1460-58), 58, 58), ElementColor.BLEU),

	CROIX_5_CENTRE(new RectangularObstacle(new XY(400, 500), 58, 58), ElementColor.JAUNE),
	CROIX_5_GAUCHE(new RectangularObstacle(new XY(400-58, 500), 58, 58), ElementColor.ORANGE),
	CROIX_5_DROITE(new RectangularObstacle(new XY(400+58, 500), 58, 58), ElementColor.VERT),
	CROIX_5_HAUT(new RectangularObstacle(new XY(400, 500+58), 58, 58), ElementColor.NOIR),
	CROIX_5_BAS(new RectangularObstacle(new XY(400, 500-58), 58, 58), ElementColor.BLEU),

	CROIX_6_CENTRE(new RectangularObstacle(new XY(1200, 810), 58, 58), ElementColor.JAUNE),
	CROIX_6_GAUCHE(new RectangularObstacle(new XY(1200-58, 810), 58, 58), ElementColor.ORANGE),
	CROIX_6_DROITE(new RectangularObstacle(new XY(1200+58, 810), 58, 58), ElementColor.VERT),
	CROIX_6_HAUT(new RectangularObstacle(new XY(1200, 810+58), 58, 58), ElementColor.NOIR),
	CROIX_6_BAS(new RectangularObstacle(new XY(1200, 810-58), 58, 58), ElementColor.BLEU);

	public final Obstacle obstacle;
	public final ElementColor couleur;

	private GameElementNames(Obstacle obs, ElementColor couleur)
	{
		obstacle = obs;
		this.couleur = couleur;
	}

	public boolean isVisible(boolean sureleve)
	{
		// les capteurs bas les voient, les hauts non
		return !sureleve;
	}

}
