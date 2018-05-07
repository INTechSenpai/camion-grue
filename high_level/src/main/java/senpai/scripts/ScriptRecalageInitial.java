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
import pfg.log.Log;
import senpai.capteurs.CapteursCorrection;
import senpai.capteurs.CapteursProcess;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.ScriptException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Table;

/**
 * Script de recalage initial
 * @author pf
 *
 */

public class ScriptRecalageInitial extends Script
{
	private XYO correction;
	private CapteursCorrection[] capteurs;
	private long dureeRecalage;
	
	public ScriptRecalageInitial(Log log, Robot robot, Table table, CapteursProcess cp, long dureeRecalage)
	{
		super(log, robot, table, cp);
		this.dureeRecalage = dureeRecalage;
		capteurs = new CapteursCorrection[] {CapteursCorrection.DROITE, CapteursCorrection.GAUCHE, CapteursCorrection.ARRIERE};
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

	@Override
	public XYO getPointEntree()
	{
		return null;
	}

	@Override
	protected void run() throws InterruptedException, UnableToMoveException, ActionneurException, ScriptException
	{		
		correction = cp.doStaticCorrection(dureeRecalage, capteurs);
		if(correction == null)
			throw new ScriptException("Aucune correction réalisée !");
		Thread.sleep(100); // attendre la mise à jour de la correction
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
