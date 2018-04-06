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

import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.CubePlace;
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
	private final CubeFace face;
	private Cube cube;
	private boolean coteDroit;
	
	public ScriptPriseCube(Croix croix, CubeColor couleur, CubeFace face, boolean coteDroit)
	{
		this.face = face;
		cube = Cube.getCube(croix, couleur);
		this.coteDroit = coteDroit;
	}
	
	@Override
	public XYO getPointEntree()
	{
		if(coteDroit)
			return null;
		else
		{
			XY_RW position = new XY_RW(298, face.angleAttaque, true).plus(cube.position);
			double angle = face.angleAttaque + Math.PI / 2 + 15. * Math.PI / 180.;
			position.plus(new XY(50, angle, true));
			return new XYO(position, angle);
		}
	}
	
	private boolean isFacePossible()
	{
		/*
		 * possible si aucun cube entre le robot et le cube voulu
		 * les cubes à gauche et à droite du cube pris vont être bougés
		 */
		return false;
	}

	@Override
	protected void run(Robot robot, Table table) throws InterruptedException, UnableToMoveException, ActionneurException
	{
	}
}
