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
import senpai.exceptions.ScriptException;
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
	private XY_RW positionEntree = new XY_RW(1200,210);
	private CapteursCorrection capteurs;
	private double angle = 0;
	private boolean coteDroit;
	
	public ScriptAbeille(Log log, Robot robot, Table table, CapteursProcess cp, boolean symetrie)
	{
		super(log, robot, table, cp);
		this.coteDroit = !symetrie;
		if(symetrie)
		{
			capteurs = CapteursCorrection.GAUCHE;
			positionEntree.setX(- positionEntree.getX());
			angle = Math.PI - angle;
		}
		else
		{
			capteurs = CapteursCorrection.DROITE;
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
	protected void run() throws InterruptedException, UnableToMoveException, ActionneurException, ScriptException
	{
		boolean repli = false;
		try {
			robot.avance(200, 0.2);
			// on se cale contre le mur en face
		} catch(UnableToMoveException e)
		{
			// OK
		}
		try {
			// on prend la distance du capteur latéral avant
			Integer distanceBrute = cp.getLast()[capteurs.c1.ordinal()];
			if(distanceBrute == null)
				throw new ScriptException("Pas de mesure du capteur latéral !");
			if(distanceBrute < 200)
				distanceBrute = 200;
			else if(distanceBrute > 230)
				distanceBrute = 230;
			angle = ((distanceBrute-200)*0.81 + (230-distanceBrute)*0.70) / 30;
			cp.startStaticCorrection(capteurs);
			robot.execute(Id.ARM_PUSH_BEE, coteDroit ? -angle : angle);
			repli = true;
			robot.setAbeilleDone();
			cp.endStaticCorrection();
		} finally {
			robot.avance(-200, 0.2);
			robot.avance(100, 0.2);
			if(repli)
				robot.execute(Id.ARM_GO_TO, 0., 0.2, 2., 8.);
			if(coteDroit)
				robot.rangeBras(LLCote.PAR_LA_GAUCHE);
			else
				robot.rangeBras(LLCote.PAR_LA_DROITE);
		}
	}

	@Override
	public boolean faisable()
	{
		return !robot.isAbeilleDone() && !robot.isThereCubeTop();
	}
}
