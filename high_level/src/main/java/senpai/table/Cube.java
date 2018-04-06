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

public enum Cube
{
	CROIX_HAUT_GAUCHE_CUBE_CENTRE(Croix.CROIX_HAUT_GAUCHE, CubeColor.JAUNE),
	CROIX_HAUT_GAUCHE_CUBE_GAUCHE(Croix.CROIX_HAUT_GAUCHE, CubeColor.VERT),
	CROIX_HAUT_GAUCHE_CUBE_DROITE(Croix.CROIX_HAUT_GAUCHE, CubeColor.ORANGE),
	CROIX_HAUT_GAUCHE_CUBE_HAUT(Croix.CROIX_HAUT_GAUCHE, CubeColor.NOIR),
	CROIX_HAUT_GAUCHE_CUBE_BAS(Croix.CROIX_HAUT_GAUCHE, CubeColor.BLEU),

	CROIX_CENTRE_GAUCHE_CUBE_CENTRE(Croix.CROIX_CENTRE_GAUCHE, CubeColor.JAUNE),
	CROIX_CENTRE_GAUCHE_CUBE_GAUCHE(Croix.CROIX_CENTRE_GAUCHE, CubeColor.VERT),
	CROIX_CENTRE_GAUCHE_CUBE_DROITE(Croix.CROIX_CENTRE_GAUCHE, CubeColor.ORANGE),
	CROIX_CENTRE_GAUCHE_CUBE_HAUT(Croix.CROIX_CENTRE_GAUCHE, CubeColor.NOIR),
	CROIX_CENTRE_GAUCHE_CUBE_BAS(Croix.CROIX_CENTRE_GAUCHE, CubeColor.BLEU),

	CROIX_TOUT_GAUCHE_CUBE_CENTRE(Croix.CROIX_TOUT_GAUCHE, CubeColor.JAUNE),
	CROIX_TOUT_GAUCHE_CUBE_GAUCHE(Croix.CROIX_TOUT_GAUCHE, CubeColor.VERT),
	CROIX_TOUT_GAUCHE_CUBE_DROITE(Croix.CROIX_TOUT_GAUCHE, CubeColor.ORANGE),
	CROIX_TOUT_GAUCHE_CUBE_HAUT(Croix.CROIX_TOUT_GAUCHE, CubeColor.NOIR),
	CROIX_TOUT_GAUCHE_CUBE_BAS(Croix.CROIX_TOUT_GAUCHE, CubeColor.BLEU),
	
	
	CROIX_HAUT_DROITE_CUBE_CENTRE(Croix.CROIX_HAUT_DROITE, CubeColor.JAUNE),
	CROIX_HAUT_DROITE_CUBE_GAUCHE(Croix.CROIX_HAUT_DROITE, CubeColor.ORANGE),
	CROIX_HAUT_DROITE_CUBE_DROITE(Croix.CROIX_HAUT_DROITE, CubeColor.VERT),
	CROIX_HAUT_DROITE_CUBE_HAUT(Croix.CROIX_HAUT_DROITE, CubeColor.NOIR),
	CROIX_HAUT_DROITE_CUBE_BAS(Croix.CROIX_HAUT_DROITE, CubeColor.BLEU),

	CROIX_CENTRE_DROITE_CUBE_CENTRE(Croix.CROIX_CENTRE_DROITE, CubeColor.JAUNE),
	CROIX_CENTRE_DROITE_CUBE_GAUCHE(Croix.CROIX_CENTRE_DROITE, CubeColor.ORANGE),
	CROIX_CENTRE_DROITE_CUBE_DROITE(Croix.CROIX_CENTRE_DROITE, CubeColor.VERT),
	CROIX_CENTRE_DROITE_CUBE_HAUT(Croix.CROIX_CENTRE_DROITE, CubeColor.NOIR),
	CROIX_CENTRE_DROITE_CUBE_BAS(Croix.CROIX_CENTRE_DROITE, CubeColor.BLEU),

	CROIX_TOUT_DROITE_CUBE_CENTRE(Croix.CROIX_TOUT_DROITE, CubeColor.JAUNE),
	CROIX_TOUT_DROITE_CUBE_GAUCHE(Croix.CROIX_TOUT_DROITE, CubeColor.ORANGE),
	CROIX_TOUT_DROITE_CUBE_DROITE(Croix.CROIX_TOUT_DROITE, CubeColor.VERT),
	CROIX_TOUT_DROITE_CUBE_HAUT(Croix.CROIX_TOUT_DROITE, CubeColor.NOIR),
	CROIX_TOUT_DROITE_CUBE_BAS(Croix.CROIX_TOUT_DROITE, CubeColor.BLEU);

	public final Obstacle obstacle;
	public final Croix croix;
	public final CubePlace place;
	public final CubeColor couleur;
	public final XY position;
	
	private Cube(Croix croix, CubeColor couleur)
	{
		this.croix = croix;
		this.place = couleur.getPlace(croix.center.getX() > 0);
		this.couleur = couleur;
		position = new XY(croix.center.getX() + place.deltaX, croix.center.getY() + place.deltaY);
		obstacle = new RectangularObstacle(position, 58, 58);
	}

	public boolean isVisible(boolean sureleve)
	{
		// les capteurs bas les voient, les hauts non
		return !sureleve;
	}
	
	public static Cube getCube(Croix croix, CubeColor couleur)
	{
		return values()[5 * croix.ordinal() + couleur.ordinal()];
	}

}
