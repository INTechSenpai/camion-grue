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

import java.util.HashMap;

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
 * Script de recalage
 * @author pf
 *
 */

public class ScriptRecalage extends Script
{
	private CapteursProcess cp;
	private XYO correction;
	private XY_RW positionEntree = new XY_RW(1300,1700);
	private HashMap<CapteursCorrection, Mur> capteurs = new HashMap<CapteursCorrection, Mur>();
	
	public ScriptRecalage(Log log, CapteursProcess cp, boolean symetrie)
	{
		super(log);
		this.cp = cp;
		if(symetrie)
		{
			capteurs.put(CapteursCorrection.DROITE, Mur.MUR_GAUCHE);
			capteurs.put(CapteursCorrection.ARRIERE, Mur.MUR_HAUT);
			positionEntree.setX(- positionEntree.getX());
		}
		else
		{
			capteurs.put(CapteursCorrection.GAUCHE, Mur.MUR_DROIT);
			capteurs.put(CapteursCorrection.ARRIERE, Mur.MUR_HAUT);
		}
			
	}

	@Override
	public XYO getPointEntree()
	{
		return new XYO(positionEntree, -Math.PI / 2);
	}

	@Override
	protected void run(Robot robot, Table table) throws InterruptedException, UnableToMoveException, ActionneurException
	{
		for(CapteursCorrection c : capteurs.keySet())
			c.murVu = capteurs.get(c);
		correction = cp.doStaticCorrection(500);
	}
	
	public XYO getCorrection()
	{
		return correction;
	}

}
