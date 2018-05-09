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
import senpai.capteurs.CapteursProcess;
import senpai.comm.CommProtocol.Id;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.obstacles.ObstaclesDynamiques;
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
	public final boolean bourrine;
//	private ObstaclesDynamiques obsDyn;
//	private RectangularObstacle o;
	
	public ScriptPriseCube(Log log, Robot robot, Table table, CapteursProcess cp, ObstaclesDynamiques obsDyn, Cube cube, CubeFace face, boolean coteDroit, boolean bourrine)
	{
		super(log, robot, table, cp);
//		this.obsDyn = obsDyn;
		this.cube = cube;
		this.bourrine = bourrine;
		this.face = face;
		this.coteDroit = coteDroit;
	}
	
	public ScriptPriseCube(Log log, Robot robot, Table table, CapteursProcess cp, ObstaclesDynamiques obsDyn, Croix croix, CubeColor couleur, CubeFace face, boolean coteDroit, boolean bourrine)
	{
		this(log, robot, table, cp, obsDyn, Cube.getCube(croix, couleur), face, coteDroit, bourrine);
	}
	
	@Override
	public XYO getPointEntree()
	{
		if(coteDroit)
		{
			XY_RW position = new XY_RW(298, face.angleAttaque, true).plus(cube.position);
//			o = new RectangularObstacle(position, 0, 300, 100, 300, face.angleAttaque);
			double angle = face.angleAttaque - Math.PI / 2 - 15. * Math.PI / 180.;
			position.plus(new XY(50, angle, true));
			return new XYO(position, angle);
		}
		else
		{
			XY_RW position = new XY_RW(298, face.angleAttaque, true).plus(cube.position);
//			o = new RectangularObstacle(position, 0, 300, 300, 100, face.angleAttaque);
			double angle = face.angleAttaque + Math.PI / 2 + 15. * Math.PI / 180.;
			position.plus(new XY(50, angle, true));
			return new XYO(position, angle);
		}
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName()+" du cube "+cube+", face "+face+", entrée : "+getPointEntree();
	}

	@Override
	protected void run() throws InterruptedException, UnableToMoveException, ActionneurException
	{
//		Thread.sleep(1000);
//		if(obsDyn.collisionScript(o))
//			throw new UnableToMoveException("Obstacle détecté !");
		table.setDone(cube); // dans tous les cas, le cas n'est plus là (soit il ne l'a jamais été, soit on l'a pris)
		robot.execute(bourrine ? Id.ARM_TAKE_CUBE : Id.ARM_TAKE_CUBE_S, coteDroit ? - Math.PI / 180 * 75 : Math.PI / 180 * 75);
		robot.storeCube(cube);
	}
	
	@Override
	public boolean faisable()
	{
		// si on peut prendre ET deposer
		// sauf si on a déjà un cube et qu'on n'a pas le temps d'en prendre un autre
		return robot.canTakeCube() && !robot.isPileFull() && (!robot.isThereCubeInside() || robot.getTempsRestant() > 20000);
	}
	
	public int getDistance(XYO position)
	{
		XYO s1 = getPointEntree();
		return (int) Math.round(s1.position.distance(position.position) + 100*Math.abs(XYO.angleDifference(s1.orientation, position.orientation)));
	}

}
