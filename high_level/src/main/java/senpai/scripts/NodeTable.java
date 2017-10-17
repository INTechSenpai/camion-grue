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

package senpai.scripts;

import senpai.robot.Robot;
import senpai.table.EtatElement;
import senpai.table.GameElementNames;
import senpai.table.Table;

/**
 * Un nœud du graphe
 * @author pf
 *
 */

public class NodeTable extends ScriptDAGNode
{
	private static final long serialVersionUID = 1L;

	private GameElementNames elementDone;
	
	public NodeTable(Table table, GameElementNames elementDone, ScriptDAGNode succes)
	{
		super(succes, null); // échec impossible
		this.elementDone = elementDone;
	}

	@Override
	public void execute(Table table, Robot robot)
	{
		table.setDone(elementDone, EtatElement.PRIS_PAR_NOUS);
	}

}
