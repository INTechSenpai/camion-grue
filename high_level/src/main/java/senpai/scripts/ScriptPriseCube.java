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

import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.comm.CommProtocol.Id;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Croix;
import senpai.table.Cube;
import senpai.table.CubeColor;
import senpai.table.CubeFace;
import senpai.table.Table;

/**
 * Le script qui dépose le minerai dans le panier
 * 
 * @author pf
 *
 */

public class ScriptPriseCube extends Script
{
	public final CubeFace face;
	public final Cube cube;
	public final boolean coteDroit;
	
	public ScriptPriseCube(Log log, Cube cube, CubeFace face, boolean coteDroit)
	{
		super(log);
		this.cube = cube;
		this.face = face;
		this.coteDroit = coteDroit;
	}
	
	public ScriptPriseCube(Log log, Croix croix, CubeColor couleur, CubeFace face, boolean coteDroit)
	{
		super(log);
		this.face = face;
		cube = Cube.getCube(croix, couleur);
		assert cube.couleur == couleur : cube.couleur+" "+couleur;
		assert cube.croix == croix : cube.croix+" "+croix;
		this.coteDroit = coteDroit;
	}
	
	@Override
	public XYO getPointEntree()
	{
		if(coteDroit)
		{
			XY_RW position = new XY_RW(298, face.angleAttaque, true).plus(cube.position);
			double angle = face.angleAttaque - Math.PI / 2 - 15. * Math.PI / 180.;
			position.plus(new XY(50, angle, true));
			return new XYO(position, angle);
		}
		else
		{
			XY_RW position = new XY_RW(298, face.angleAttaque, true).plus(cube.position);
			double angle = face.angleAttaque + Math.PI / 2 + 15. * Math.PI / 180.;
			position.plus(new XY(50, angle, true));
			return new XYO(position, angle);
		}
	}

	@Override
	protected void run(Robot robot, Table table) throws InterruptedException, UnableToMoveException, ActionneurException
	{
		// exemple classique :
		// take cube, store cube inside
		// take cube, store cube top
		// put cube, table cube inside, put cube, go to home 
		robot.execute(Id.ARM_TAKE_CUBE, coteDroit ? 75 : -75);
		robot.storeCube(cube);
		table.setDone(cube);
	}
}
