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


package scripts;

/**
 * Scripts symétrisés
 * @author pf
 *
 */

public enum ScriptsSymetrises
{
	SCRIPT_HOMOLO_A_NOUS(ScriptNames.SCRIPT_HOMOLO_DROITE, ScriptNames.SCRIPT_HOMOLO_GAUCHE),

	SCRIPT_CRATERE_HAUT_A_NOUS(ScriptNames.SCRIPT_CRATERE_HAUT_DROITE, ScriptNames.SCRIPT_CRATERE_HAUT_GAUCHE),
	SCRIPT_CRATERE_HAUT_ENNEMI(ScriptNames.SCRIPT_CRATERE_HAUT_GAUCHE, ScriptNames.SCRIPT_CRATERE_HAUT_DROITE),
	SCRIPT_CRATERE_BAS_A_NOUS(ScriptNames.SCRIPT_CRATERE_BAS_DROITE, ScriptNames.SCRIPT_CRATERE_BAS_GAUCHE),
	SCRIPT_CRATERE_BAS_ENNEMI(ScriptNames.SCRIPT_CRATERE_BAS_GAUCHE, ScriptNames.SCRIPT_CRATERE_BAS_DROITE),

	SCRIPT_DEPOSE_MINERAI(ScriptNames.SCRIPT_DEPOSE_MINERAI_DROITE, ScriptNames.SCRIPT_DEPOSE_MINERAI_GAUCHE),
	SCRIPT_DEPOSE_MINERAI_FIN(ScriptNames.SCRIPT_DEPOSE_MINERAI_DROITE_FIN, ScriptNames.SCRIPT_DEPOSE_MINERAI_GAUCHE_FIN),
	SCRIPT_DEPOSE_MINERAI_FIN_APRES_BAS(ScriptNames.SCRIPT_DEPOSE_MINERAI_DROITE_FIN_APRES_BAS, ScriptNames.SCRIPT_DEPOSE_MINERAI_GAUCHE_FIN_APRES_BAS);

	private final ScriptNames sNoSym, sSym;
	
	private ScriptsSymetrises(ScriptNames sNoSym, ScriptNames sSym)
	{
		this.sNoSym = sNoSym;
		this.sSym = sSym;
	}
	
	public ScriptNames getScript(boolean symetrie)
	{
		if(symetrie)
			return sSym;
		return sNoSym;
	}
}
