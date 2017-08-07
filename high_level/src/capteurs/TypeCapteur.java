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

package capteurs;

import java.awt.Color;

import senpai.CouleurSenpai;

/**
 * Les différents types de capteurs
 * 
 * @author pf
 *
 */

public enum TypeCapteur
{
	// IR : cône de 5°, horizon à 80mm, distance min 20mm
	// ToF : cône de 0.1°, horizon à 254mm, distance min 0mm
	// ToF longue portée : cône de 0.1°, horizon à 500mm

	ToF_COURT(0.01, 1, 254, CouleurSenpai.ToF_COURT, 1),
	ToF_LONG(0.01, 1, 500, CouleurSenpai.ToF_LONG, 2),
	IR(5. / 180 * Math.PI, 100, 630, CouleurSenpai.IR, 10);

	public final double angleCone; // ne sert qu'à l'affichage
	public final int distanceMin, portee;
	public final int conversion; // le coeff pour passer en mm
	public Color couleur, couleurTransparente;
	public CouleurSenpai couleurOrig;

	private TypeCapteur(double angleCone, int distanceMin, int portee, CouleurSenpai c, int conversion)
	{
		this.conversion = conversion;
		couleurOrig = c;
		couleur = c.couleur;
		couleurTransparente = new Color(couleur.getRed(), couleur.getGreen(), couleur.getBlue(), 100);
		this.angleCone = angleCone;
		this.distanceMin = distanceMin;
		this.portee = portee;
	}
}
