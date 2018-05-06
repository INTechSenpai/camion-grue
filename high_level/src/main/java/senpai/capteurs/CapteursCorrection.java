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

	GAUCHE(CapteursRobot.ToF_LATERAL_AVANT_GAUCHE,
			CapteursRobot.ToF_LATERAL_ARRIERE_GAUCHE),
	DROITE(CapteursRobot.ToF_LATERAL_AVANT_DROIT,
			CapteursRobot.ToF_LATERAL_ARRIERE_DROIT),
	ARRIERE(CapteursRobot.ToF_ARRIERE_DROITE,
			CapteursRobot.ToF_ARRIERE_GAUCHE),
	AVANT(CapteursRobot.ToF_AVANT,
			CapteursRobot.ToF_AVANT);
	
	public final CapteursRobot c1;
	public final CapteursRobot c2;
	public volatile Mur murVu = null;
	public List<Integer> valc1 = new ArrayList<Integer>();	
	public List<Integer> valc2 = new ArrayList<Integer>();
	
	private CapteursCorrection(CapteursRobot c1, CapteursRobot c2)
	{
		this.c1 = c1;
		this.c2 = c2;
	}
	
}
