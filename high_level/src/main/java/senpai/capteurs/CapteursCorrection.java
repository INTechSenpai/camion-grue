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

import java.util.ArrayList;
import java.util.List;

/**
 * Les différents capteurs utilisés pour la correction
 * @author pf
 *
 */

public enum CapteursCorrection {

	GAUCHE(CapteursRobot.ToF_LATERAL_AVANT_GAUCHE, CapteursRobot.ToF_LATERAL_AVANT_GAUCHE.pos.getX(),
			CapteursRobot.ToF_LATERAL_ARRIERE_GAUCHE, CapteursRobot.ToF_LATERAL_ARRIERE_GAUCHE.pos.getX(), CapteursRobot.ToF_LATERAL_ARRIERE_GAUCHE.pos.getY()),
	DROITE(CapteursRobot.ToF_LATERAL_AVANT_DROIT, CapteursRobot.ToF_LATERAL_AVANT_DROIT.pos.getX(),
			CapteursRobot.ToF_LATERAL_ARRIERE_DROIT, CapteursRobot.ToF_LATERAL_ARRIERE_DROIT.pos.getX(), CapteursRobot.ToF_LATERAL_ARRIERE_DROIT.pos.getY()),
	ARRIERE(CapteursRobot.ToF_ARRIERE_DROITE, CapteursRobot.ToF_ARRIERE_DROITE.pos.getY(),
			CapteursRobot.ToF_ARRIERE_GAUCHE, CapteursRobot.ToF_ARRIERE_GAUCHE.pos.getY(), CapteursRobot.ToF_ARRIERE_DROITE.pos.getX()),
	AVANT(CapteursRobot.ToF_AVANT, CapteursRobot.ToF_AVANT.pos.getY(),
			CapteursRobot.ToF_AVANT, CapteursRobot.ToF_AVANT.pos.getY(), CapteursRobot.ToF_AVANT.pos.getX());
	
	public final CapteursRobot c1, c2;
	public final double distanceToCenterc1, distanceToCenterc2, distanceBetween, distanceToRobot;
	public volatile Mur murVu = null;
	public List<Integer> valc1 = new ArrayList<Integer>();	
	public List<Integer> valc2 = new ArrayList<Integer>();
	
	private CapteursCorrection(CapteursRobot c1, double distanceToCenterc1, CapteursRobot c2, double distanceToCenterc2, double distanceToRobot)
	{
		this.c1 = c1;
		this.c2 = c2;
		this.distanceToCenterc1 = distanceToCenterc1;
		this.distanceToCenterc2 = distanceToCenterc2;
		this.distanceToRobot = distanceToRobot;
		distanceBetween = distanceToCenterc1 + distanceToCenterc2;
	}
	
}
