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

package senpai.scripts;

import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.capteurs.CapteursCorrection;
import senpai.capteurs.CapteursProcess;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Table;

/**
 * Script du panneau domotique
 * @author pf
 *
 */

public class ScriptDomotique extends Script
{
	private XY_RW positionEntree = new XY_RW(370,1780);
	
	public ScriptDomotique(Log log, Robot robot, Table table, CapteursProcess cp, boolean symetrie)
	{
		super(log, robot, table, cp);
		if(symetrie)
			positionEntree.setX(- positionEntree.getX());
	}

	@Override
	public XYO getPointEntree()
	{
		return new XYO(positionEntree, -Math.PI / 2);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

	@Override
	protected void run() throws InterruptedException, UnableToMoveException, ActionneurException
	{
		try {
			robot.avance(-300, 0.2);
		} catch(UnableToMoveException e)
		{
			// OK
		}
		robot.setDomotiqueDone();
		robot.avance(100);
		cp.doStaticCorrection(500, CapteursCorrection.ARRIERE);
	}

	@Override
	public boolean faisable()
	{
		return robot.isDomotiqueDone() && !robot.isThereCubeTop();
	}

}
