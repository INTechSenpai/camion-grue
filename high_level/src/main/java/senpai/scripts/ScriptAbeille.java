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
import senpai.comm.CommProtocol.Id;
import senpai.comm.CommProtocol.LLCote;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Table;

/**
 * Script de l'abeille
 * @author pf
 *
 */

public class ScriptAbeille extends Script
{
	private XY_RW positionEntree = new XY_RW(1200,180);
	private CapteursCorrection[] capteurs = new CapteursCorrection[2];
	private double angle = 0;
	
	public ScriptAbeille(Log log, Robot robot, Table table, CapteursProcess cp, boolean symetrie)
	{
		super(log, robot, table, cp);
		if(symetrie)
		{
			capteurs[0] = CapteursCorrection.GAUCHE;
			capteurs[1] = CapteursCorrection.AVANT;
			positionEntree.setX(- positionEntree.getX());
			angle = Math.PI - angle;
		}
		else
		{
			capteurs[0] = CapteursCorrection.DROITE;
			capteurs[1] = CapteursCorrection.AVANT;
		}
	}

	@Override
	public XYO getPointEntree()
	{
		return new XYO(positionEntree, angle);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

	@Override
	protected void run() throws InterruptedException, UnableToMoveException, ActionneurException
	{
		robot.avance(200, 0.2);
		try {
			cp.startStaticCorrection(CapteursCorrection.AVANT, CapteursCorrection.GAUCHE);
			robot.execute(Id.ARM_PUSH_BEE, 0); // TODO angle
			robot.rangeBras(LLCote.AU_PLUS_VITE);
			cp.endStaticCorrection();
		} finally {
			robot.avance(-200, 0.2);
		}
	}

	@Override
	public boolean faisable()
	{
		return !robot.isAbeilleDone();
	}
}
