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
import senpai.capteurs.Mur;
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
	private CapteursProcess cp;
	
	public ScriptDomotique(Log log, CapteursProcess cp, boolean symetrie)
	{
		super(log);
		this.cp = cp;
		if(symetrie)
			positionEntree.setX(- positionEntree.getX());
	}

	@Override
	public XYO getPointEntree()
	{
		return new XYO(positionEntree, -Math.PI / 2);
	}

	@Override
	protected void run(Robot robot, Table table) throws InterruptedException, UnableToMoveException, ActionneurException
	{
		try {
			robot.avance(-100, 200);
		} catch(UnableToMoveException e)
		{
			// OK
		}
		robot.setDomotiqueDone();
		robot.avance(100, 800);

		CapteursCorrection.ARRIERE.murVu = Mur.MUR_HAUT;
		cp.doStaticCorrection(500);
		
		robot.avance(100, 800);
	}

}
