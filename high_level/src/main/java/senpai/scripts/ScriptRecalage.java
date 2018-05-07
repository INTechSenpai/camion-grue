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
import senpai.exceptions.ScriptException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Table;

/**
 * Script de recalage
 * @author pf
 *
 */

public class ScriptRecalage extends Script
{
	private XYO correction;
	private XY_RW positionEntree = new XY_RW(1300,1700);
	private CapteursCorrection[] capteurs = new CapteursCorrection[2];
	private long dureeRecalage;
	
	public ScriptRecalage(Log log, Robot robot, Table table, CapteursProcess cp, boolean symetrie, long dureeRecalage)
	{
		super(log, robot, table, cp);
		this.dureeRecalage = dureeRecalage;
		if(symetrie)
		{
			capteurs[0] = CapteursCorrection.DROITE;
			capteurs[1] = CapteursCorrection.ARRIERE;
			positionEntree.setX(- positionEntree.getX());
		}
		else
		{
			capteurs[0] = CapteursCorrection.GAUCHE;
			capteurs[1] = CapteursCorrection.ARRIERE;
		}
			
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

	@Override
	public XYO getPointEntree()
	{
		return new XYO(positionEntree, -Math.PI / 2);
	}

	@Override
	protected void run() throws InterruptedException, UnableToMoveException, ActionneurException, ScriptException
	{		
		correction = cp.doStaticCorrection(dureeRecalage, capteurs);
		if(correction == null)
			throw new ScriptException("Aucune correction réalisée !");
		Thread.sleep(200); // attendre la mise à jour de la correction
	}
	
	public XYO getCorrection()
	{
		return correction;
	}

	@Override
	public boolean faisable()
	{
		return true;
	}
}
