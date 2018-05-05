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
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import pfg.log.Log;
import senpai.capteurs.CapteursProcess;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.table.Croix;
import senpai.table.Cube;
import senpai.table.CubeColor;
import senpai.table.CubeFace;
import senpai.table.Table;
import senpai.utils.ConfigInfoSenpai;
import pfg.config.Config;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XY_RW;

/**
 * Script manager
 * @author pf
 *
 */

public class ScriptManager
{
	protected Log log;
	private Table table;
	private Robot robot;
	private CapteursProcess cp;
	private RobotColor couleur;
	
	public void setCouleur(RobotColor couleur)
	{
		this.couleur = couleur;
		if(couleur.symmetry)
		{
			pilePosition[0].setX(-pilePosition[0].getX());
			pilePosition[1].setX(-pilePosition[1].getX());
		}
	}
	
	private final boolean usePattern;
	private final String pattern;
	@SuppressWarnings("unchecked")
	private List<Cube>[] piles = (List<Cube>[]) new List[2];
	private XY_RW[] pilePosition = new XY_RW[] {new XY_RW(0,0), new XY_RW(0,0)};
	private CubeFace faceDepose;
	private boolean coteDroit;
	private double[] longueurGrue = new double[]{300, 300, 290, 365, 365}; // longueur de la grue en fonction du nombre de cube déjà posés
	
	public ScriptManager(Log log, Table table, Robot robot, Config config, CapteursProcess cp)
	{
		this.log = log;
		this.table = table;
		this.robot = robot;
		this.cp = cp;
		piles[0] = new ArrayList<Cube>();
		piles[1] = new ArrayList<Cube>();
		
		pattern = config.getString(ConfigInfoSenpai.COLOR_PATTERN);
		usePattern = pattern.isEmpty();
	}

	public ScriptRecalage getScriptRecalage(long dureeRecalage)
	{
		return new ScriptRecalage(log, robot, table, cp, couleur.symmetry, dureeRecalage);		
	}
	
	public ScriptRecalage getScriptRecalage()
	{
		return getScriptRecalage(500);
	}
	
	public ScriptDomotique getScriptDomotique()
	{
		return new ScriptDomotique(log, robot, table, cp, couleur.symmetry);
	}

	public ScriptDeposeCube getDeposeScript()
	{
		int nbPile = getNbPile();
		return new ScriptDeposeCube(log, robot, table, piles[nbPile].size(), pilePosition[nbPile], faceDepose, coteDroit, longueurGrue[piles[nbPile].size()]);
	}
	
	private int getNbPile()
	{
		if(usePattern && piles[0].size() >= 3)
			return 1;
		return 0;
	}

	public class CubeComparator implements Comparator<ScriptPriseCube>
	{
		private XY position;
		
		public CubeComparator(XY position)
		{
			this.position = position;
		}
		
		@Override
		public int compare(ScriptPriseCube arg0, ScriptPriseCube arg1) {
			return (int) (arg0.cube.position.squaredDistance(position) - arg1.cube.position.squaredDistance(position));
		}
		
	}
	
	public PriorityQueue<ScriptPriseCube> getAllPossible(CubeColor couleur, boolean bourrine)
	{
		PriorityQueue<ScriptPriseCube> out = new PriorityQueue<ScriptPriseCube>(new CubeComparator(robot.getCinematique().getPosition()));
		
		/*
		 * On n'a plus de place !
		 */
		if(!robot.canTakeCube())
			return out;
		
		for(Croix croix : Croix.values())
		{
			if(this.couleur.symmetry == croix.center.getX() > 0)
				continue;
			for(CubeFace f : CubeFace.values())
			{
				Cube c = Cube.getCube(croix, couleur);
				if(isFacePossible(c, f, bourrine))
				{
					out.add(new ScriptPriseCube(log,robot, table, c,f,true));
					out.add(new ScriptPriseCube(log,robot, table, c,f,false));
				}
			}
		}

		return out;
	}
	
	public PriorityQueue<ScriptPriseCube> getAllPossible(boolean bourrine)
	{
		PriorityQueue<ScriptPriseCube> out = new PriorityQueue<ScriptPriseCube>(new CubeComparator(robot.getCinematique().getPosition()));
		for(Cube c : Cube.values())
		{
			if(couleur.symmetry == c.position.getX() > 0)
				continue;
			for(CubeFace f : CubeFace.values())
				if(isFacePossible(c, f, bourrine))
				{
					out.add(new ScriptPriseCube(log,robot, table, c,f,true));
					out.add(new ScriptPriseCube(log,robot, table, c,f,false));
				}
		}
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
		if(table.isDone(c))
			return false;
		
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
