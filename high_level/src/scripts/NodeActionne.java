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

import comm.CommProtocol;
import exceptions.ActionneurException;
import robot.RobotReal;
import table.RealTable;

/**
 * Un nœud du graphe
 * @author pf
 *
 */

public class NodeActionne extends ScriptDAGNode
{
	private static final long serialVersionUID = 1L;
	private Object[] param;
	private CommProtocol ordre;

	public NodeActionne(ScriptDAGNode succes, ScriptDAGNode echec, CommProtocol ordre, Object... param)
	{
		super(succes, echec);
		this.ordre = ordre;
		this.param = param;
	}

	@Override
	public void execute(RealTable table, RobotReal robot) throws InterruptedException, ActionneurException
	{
		robot.execute(ordre, param);
	}

}
