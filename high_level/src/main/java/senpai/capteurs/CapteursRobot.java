package senpai.capteurs;

import pfg.kraken.utils.XY;

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


/**
 * Énum contenant les capteurs du robot
 * 
 * @author pf
 *
 */

public enum CapteursRobot
{
	// Doc : [1] ToF LP Avant ; [2] IR Avant Gauche ; [3] ToF LP Arrière ; [4]
	// IR Avant Droit ; [5] ToF Avant Gauche ; [6] ToF Flan Avant Gauche ; [7]
	// ToF Flan Arrière Gauche ; [8] ToF Arrière Gauche ; [9] ToF Arrière Droit
	// ; [10] ToF Flan Arrière Droit ; [11] ToF Flan Avant Droit ; [12]
	// ToF Avant Droit

	
	ToF_AVANT(CapteurImmobile.class, new XY(243, 0), 0, TypeCapteur.ToF_COURT),
	
	ToF_COIN_AVANT_GAUCHE(CapteurImmobile.class, new XY(243, 80), Math.PI / 4, TypeCapteur.ToF_COURT),

	ToF_COIN_AVANT_DROIT(CapteurImmobile.class, new XY(243, -80), -Math.PI / 4, TypeCapteur.ToF_COURT),

	ToF_LATERAL_AVANT_GAUCHE(CapteurImmobile.class, new XY(139, 88), Math.PI / 2, TypeCapteur.ToF_COURT),

	ToF_LATERAL_AVANT_DROIT(CapteurImmobile.class, new XY(139, -88), -Math.PI / 2, TypeCapteur.ToF_COURT),

	ToF_LATERAL_ARRIERE_GAUCHE(CapteurImmobile.class, new XY(-139, 88), Math.PI / 2, TypeCapteur.ToF_COURT),

	ToF_LATERAL_ARRIERE_DROIT(CapteurImmobile.class, new XY(-139, -88), -Math.PI / 2, TypeCapteur.ToF_COURT),

	ToF_ARRIERE_GAUCHE(CapteurImmobile.class, new XY(-164, 80), -Math.PI, TypeCapteur.ToF_COURT),

	ToF_ARRIERE_DROITE(CapteurImmobile.class, new XY(-164, -80), -Math.PI, TypeCapteur.ToF_COURT),
	
	TOURELLE_GAUCHE(CapteurMobile.class, new XY(40,52), 0, TypeCapteur.ToF_LONG),

	TOURELLE_DROITE(CapteurMobile.class, new XY(40,-52), 0, TypeCapteur.ToF_LONG);

	
	public final Class<? extends Capteur> classe;
	public final XY pos;
	public final double angle;
	public final TypeCapteur type;
	public final boolean sureleve;
	public final static CapteursRobot[] values = values();

	private <S extends Capteur> CapteursRobot(Class<S> classe, XY pos, double angle, TypeCapteur type)
	{
		sureleve = name().startsWith("TOURELLE_"); // les tourelles sont surélevées
		this.classe = classe;
		this.pos = pos;
		this.angle = angle;
		this.type = type;
	}

}
