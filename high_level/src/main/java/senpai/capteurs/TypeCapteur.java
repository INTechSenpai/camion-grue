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

package senpai.capteurs;

import java.awt.Color;
import senpai.utils.CouleurSenpai;

/**
 * Les différents types de capteurs
 * 
 * @author pf
 *
 */

public enum TypeCapteur
{
	// ToF : cône de 0.1°, horizon à 254mm, distance min 0mm
	// ToF longue portée : cône de 0.1°, horizon à 500mm

	ToF_COURT(0.01, 150, CouleurSenpai.ToF_COURT),
	ToF_LONG(0.01, 500, CouleurSenpai.ToF_LONG);

	public final double angleCone; // ne sert qu'à l'affichage
	public final int portee; // ne sert qu'à l'affichage
	public final Color couleur, couleurTransparente;
	
	private TypeCapteur(double angleCone, int portee, CouleurSenpai c)
	{
		couleur = c.couleur;
		couleurTransparente = new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), 100);
		this.angleCone = angleCone;
		this.portee = portee;
	}
}
