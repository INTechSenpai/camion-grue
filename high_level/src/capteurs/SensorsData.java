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

import pfg.kraken.robot.Cinematique;

/**
 * Un groupe de mesures qui proviennent des capteurs
 * 
 * @author pf
 *
 */

public class SensorsData
{
	public enum TraitementEtat
	{
		HORS_TABLE,
		DANS_OBSTACLE_FIXE,
		TROP_LOIN,
		TROP_PROCHE,
		DESACTIVE,
		DESACTIVE_SCAN, // les capteurs qui ne participent pas au scan
		SCAN,
		OBSTACLE_CREE;
	}
	
	public Cinematique cinematique;
	/** Ce que voit chacun des capteurs */
	public int[] mesures;
	public TraitementEtat[] etats;
	public double angleRoueGauche, angleRoueDroite;

	public SensorsData(double angleRoueGauche, double angleRoueDroite, int[] mesures, Cinematique cinematique)
	{
		this.angleRoueDroite = angleRoueDroite;
		this.angleRoueGauche = angleRoueGauche;
		this.mesures = mesures;
		etats = new TraitementEtat[mesures.length];
		this.cinematique = cinematique;
	}

	public SensorsData(Cinematique cinematique)
	{
		angleRoueDroite = 0;
		angleRoueGauche = 0;
		mesures = null;
		etats = null;
		this.cinematique = cinematique;
	}
	
	/**
	 * On vérifie que tous les capteurs ont bien reçu un traitement
	 * @return
	 */
	public boolean checkTraitementEtat()
	{
		if(etats == null)
			return true;
		for(int i = 0; i < etats.length; i++)
			if(etats[i] == null)
				return false;
		return true;
	}

}
