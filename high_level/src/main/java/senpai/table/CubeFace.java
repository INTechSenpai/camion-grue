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


package senpai.table;

/**
 * La face d'un cube
 * @author pf
 *
 */

public enum CubeFace
{
	// L'ordre est important
	GAUCHE(Math.PI),
	BAS(- Math.PI / 2),
	DROITE(0),
	HAUT(Math.PI / 2);
	
	public final double angleAttaque;
	
	private CubeFace(double angleAttaque)
	{
		this.angleAttaque = angleAttaque;
	}
	
	public CubeFace getOrthogonal(boolean cote)
	{
		if(cote)
			return values()[(ordinal() + 1) % 4];
		else
			return values()[(ordinal() + 3) % 4];
	}
	
	public Cube getVoisin(Cube c)
	{
		CubePlace p = getFaceVoisin(c.place);
		if(p == null)
			return null;
		return Cube.getCube(c.croix, p);
	}
	
	private CubePlace getFaceVoisin(CubePlace c)
	{
		if(c == CubePlace.CENTRE)
			return CubePlace.values()[(ordinal() + 2) % 4];
		else if(c.ordinal() == ordinal())
			return CubePlace.CENTRE;
		return null;
	}
}