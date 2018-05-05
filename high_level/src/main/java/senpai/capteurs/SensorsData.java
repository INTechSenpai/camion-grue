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

import pfg.kraken.robot.Cinematique;

/**
 * Un groupe de mesures qui proviennent des capteurs
 * 
 * @author pf
 *
 */

public class SensorsData
{
/*	public enum TraitementEtat
	{
		HORS_TABLE,
		DANS_OBSTACLE_FIXE,
		TROP_LOIN,
		TROP_PROCHE,
		DESACTIVE,
		DESACTIVE_SCAN, // les capteurs qui ne participent pas au scan
		SCAN,
		OBSTACLE_CREE;
	}*/
	
	public Cinematique cinematique;
	/* Ce que voit chacun des capteurs */
	public final int[] mesures;
	public long dateCreation;
//	public TraitementEtat[] etats;
	public double angleTourelleGauche, angleTourelleDroite, angleGrue;

	public SensorsData()
	{
		mesures = new int[CapteursRobot.values().length];
	}
	
/*	public SensorsData(double angleRoueGauche, double angleRoueDroite, double angleGrue, int[] mesures, Cinematique cinematique)
	{
		dateCreation = System.currentTimeMillis();
		this.cinematique = cinematique;
		this.angleTourelleDroite = angleRoueDroite;
		this.angleTourelleGauche = angleRoueGauche;
		this.angleGrue = angleGrue;
		this.mesures = mesures;
//		etats = new TraitementEtat[mesures.length];
	}*/

	
	/**
	 * On vérifie que tous les capteurs ont bien reçu un traitement
	 * @return
	 */
/*	public boolean checkTraitementEtat()
	{
		if(etats == null)
			return true;
		for(int i = 0; i < etats.length; i++)
			if(etats[i] == null)
				return false;
		return true;
	}*/

}
