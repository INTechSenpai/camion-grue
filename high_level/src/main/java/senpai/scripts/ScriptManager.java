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

import java.util.ArrayList;
import java.util.List;
import pfg.log.Log;
import senpai.table.Croix;
import senpai.table.Cube;
import senpai.table.CubeColor;
import senpai.table.CubeFace;
import senpai.table.Table;

/**
 * Script manager
 * @author pf
 *
 */

public class ScriptManager
{
	protected Log log;
	private Table table;
	
	public ScriptManager(Log log, Table table)
	{
		this.log = log;
		this.table = table;
	}
	
	public List<CubeAndFace> getAllPossible(CubeColor couleur, boolean bourrine)
	{
		List<CubeAndFace> out = new ArrayList<CubeAndFace>();
		for(Croix croix : Croix.values())
			for(CubeFace f : CubeFace.values())
			{
				Cube c = Cube.getCube(croix, couleur);
				if(isFacePossible(c, f, bourrine))
					out.add(new CubeAndFace(c,f));
			}
		return out;
	}
	
	public List<CubeAndFace> getAllPossible(boolean bourrine)
	{
		List<CubeAndFace> out = new ArrayList<CubeAndFace>();
		for(Cube c : Cube.values())
			for(CubeFace f : CubeFace.values())
				if(isFacePossible(c, f, bourrine))
					out.add(new CubeAndFace(c,f));
		return out;
	}
	
	private boolean isFacePossibleEnBourrinant(Cube c, CubeFace f)
	{
		/*
		 * possible si aucun cube entre le robot et le cube voulu
		 * les cubes à gauche et à droite du cube pris vont être bougés
		 */
		
		/*
		 * "O" est le cube qu'on veut prendre, depuis le bas
		 * Cubes vérifiés :
		 *   O
		 * A B C
		 *   D
		 */
		
		Cube p1 = f.getVoisin(c); // B
		boolean voisin1 = p1 == null || table.isDone(p1);
		
		if(p1 != null) // on vérifie ses voisins
		{
			Cube p2 = f.getVoisin(p1); // D
			boolean voisin2 = p2 == null || table.isDone(p2);
			
			Cube p3 = f.getOrthogonal(true).getVoisin(p1); // A
			boolean voisin3 = p3 == null || table.isDone(p3);
			
			Cube p4 = f.getOrthogonal(false).getVoisin(p1); // C
			boolean voisin4 = p4 == null || table.isDone(p4);
			
			return voisin1 && voisin2 && voisin3 && voisin4;
		}
		else
		{
			boolean voisin2 = true, voisin3 = true;
			
			Cube p2 = f.getOrthogonal(true).getVoisin(c);
			if(p2 != null)
			{
				Cube p3 = f.getOrthogonal(false).getVoisin(p2);
				voisin2 = p3 == null || table.isDone(p3); // A
			}
			
			p2 = f.getOrthogonal(false).getVoisin(c);
			if(p2 != null)
			{
				Cube p3 = f.getOrthogonal(true).getVoisin(p2);
				voisin3 = p3 == null || table.isDone(p3); // C
			}

			return voisin1 && voisin2 && voisin3;
		}
	}
	
	private boolean isFacePossible(Cube c, CubeFace f, boolean bourrine)
	{
		// c'est nécessaire
		if(!isFacePossibleEnBourrinant(c,f))
			return false;
		if(bourrine)
			return true;
		
		/*
		 * "O" est le cube qu'on veut prendre, depuis le bas
		 * Cubes vérifiés :
		 * X O X
		 * X X X
		 *   X
		 */
		
		Cube p1 = f.getOrthogonal(true).getVoisin(c);
		boolean voisin1 = p1 == null || table.isDone(p1);
		
		Cube p2 = f.getOrthogonal(false).getVoisin(c);
		boolean voisin2 = p2 == null || table.isDone(p2);

		return voisin1 && voisin2;
	}
}
