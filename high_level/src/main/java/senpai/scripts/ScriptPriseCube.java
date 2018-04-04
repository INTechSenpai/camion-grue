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
import senpai.table.ElementColor;
import senpai.table.Table;

/**
 * Le script qui dépose le minerai dans le panier
 * 
 * @author pf
 *
 */

public class ScriptPriseCube extends Script
{
	
	public enum CubePlace
	{
		CENTRE(0,0),
		GAUCHE(-58,0),
		DROITE(58,0),
		BAS(0,-58),
		HAUT(0,58);
		
		public final int deltaX, deltaY;
		
		private CubePlace(int deltaX, int deltaY)
		{
			this.deltaX = deltaX;
			this.deltaY = deltaY;
		}
	}
	
	public enum Face
	{
		GAUCHE(Math.PI),
		DROITE(0),
		BAS(- Math.PI / 2),
		HAUT(Math.PI / 2);
		
		public final double angleAttaque;
		
		private Face(double angleAttaque)
		{
			this.angleAttaque = angleAttaque;
		}
	}
	
	private final Face face;
	private CubePlace place;
	private XY cubePosition;
	boolean coteDroit;
	
	public ScriptPriseCube(int nbCroix, ElementColor color, Face face, boolean coteDroit)
	{
		this.face = face;
		cubePosition = new XY_RW(650, 1460);
		this.place = color.getPlace(cubePosition.getX() > 0);
		cubePosition = cubePosition.plusNewVector(new XY(place.deltaX, place.deltaY));
		this.coteDroit = coteDroit;
	}
	
	@Override
	public XYO getPointEntree()
	{
		if(coteDroit)
			return null;
		else
		{
			XY_RW position = new XY_RW(298, face.angleAttaque, true).plus(cubePosition);
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
