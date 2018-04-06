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

package senpai.robot;

/**
 * Les deux couleurs possibles pour le robot.
 * 
 * @author pf
 *
 */

public enum RobotColor
{

	/**
	 * Ces noms sont utilisés pour les tests uniquement. Sinon, on utilise le
	 * boolean symmetry
	 */
	ORANGE(false), // côté droite de la table
	VERT(true); // côté gauche de la table

	public final boolean symmetry;

	private RobotColor(boolean symmetry)
	{
		this.symmetry = symmetry;
	}

	/**
	 * Convertit une chaîne de caractère en enum
	 * 
	 * @param chaine
	 * @return
	 */
	public static RobotColor parse(String chaine)
	{
		if(chaine.compareToIgnoreCase(ORANGE.name()) == 0)
			return ORANGE;
		return VERT;
	}

	/**
	 * Récupère la couleur pour laquelle il n'y a pas de symétrie.
	 * Utilisé pour les tests sans avoir à hardcoder la couleur.
	 * 
	 * @return
	 */
	public static RobotColor getCouleurSansSymetrie()
	{
		for(RobotColor r : RobotColor.values())
			if(!r.symmetry)
				return r;
		return null;
	}

	public static String getCouleur(boolean symetrie)
	{
		for(RobotColor r : RobotColor.values())
			if(symetrie == r.symmetry)
				return r.toString();
		return null;
	}

}
