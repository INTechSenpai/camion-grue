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

import java.awt.Color;
import senpai.scripts.ScriptPriseCube;
import senpai.scripts.ScriptPriseCube.CubePlace;

/**
 * Les couleurs des éléments de jeux
 * @author Pierre-François Gimenez
 *
 */

public enum ElementColor
{
	VERT(Color.GREEN, CubePlace.DROITE, CubePlace.GAUCHE),
	NOIR(Color.BLACK, CubePlace.HAUT, CubePlace.HAUT),
	JAUNE(Color.YELLOW, CubePlace.CENTRE, CubePlace.CENTRE),
	ORANGE(Color.ORANGE, CubePlace.GAUCHE, CubePlace.DROITE),
	BLEU(Color.BLUE, CubePlace.BAS, CubePlace.BAS);
	
	public final Color color;
	private CubePlace placeDroite, placeGauche;
	
	private ElementColor(Color color, CubePlace placeDroite, CubePlace placeGauche)
	{
		this.color = color;
		this.placeDroite = placeDroite;
		this.placeGauche = placeGauche;
	}
	
	public CubePlace getPlace(boolean coteDroit)
	{
		if(coteDroit)
			return placeDroite;
		return placeGauche;
	}
}
