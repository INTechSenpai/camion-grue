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

import java.util.Comparator;
import java.util.PriorityQueue;

import pfg.log.Log;
import senpai.capteurs.CapteursProcess;
import senpai.obstacles.ObstaclesDynamiques;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.table.Croix;
import senpai.table.Cube;
import senpai.table.CubeColor;
import senpai.table.CubeFace;
import senpai.table.Table;
import senpai.utils.ConfigInfoSenpai;
import senpai.utils.Subject;
import pfg.config.Config;
import pfg.kraken.utils.XYO;
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
	private ObstaclesDynamiques obsDyn;
	
	public void setCouleur(RobotColor couleur)
	{
		this.couleur = couleur;
		if(couleur.symmetry)
		{
			pilePosition[0].setX(-pilePosition[0].getX());
			pilePosition[1].setX(-pilePosition[1].getX());
			anglesDepose[0] = Math.PI - anglesDepose[0];
			anglesDepose[1] = Math.PI - anglesDepose[1];
			coteDroits[0] = !coteDroits[0];
			coteDroits[1] = !coteDroits[1];
		}
	}
	
	private boolean usePattern;
	private CubeColor[] pattern;
	private XY_RW[] pilePosition;
	private double anglesDepose[];
	private double[] longueurGrue = new double[]{300, 300, 290, 365, 365}; // longueur de la grue en fonction du nombre de cube déjà posés
	private boolean[] coteDroits;
	
	public ScriptManager(Log log, Config config, Table table, Robot robot, CapteursProcess cp, ObstaclesDynamiques obsDyn)
	{
		this.obsDyn = obsDyn;
		this.log = log;
		this.table = table;
		this.robot = robot;
		this.cp = cp;
		pilePosition = new XY_RW[] {
				new XY_RW(config.getDouble(ConfigInfoSenpai.PILE_1_X),config.getDouble(ConfigInfoSenpai.PILE_1_Y)),
				new XY_RW(config.getDouble(ConfigInfoSenpai.PILE_2_X),config.getDouble(ConfigInfoSenpai.PILE_2_Y))};
		anglesDepose = new double[] {config.getDouble(ConfigInfoSenpai.PILE_1_O),config.getDouble(ConfigInfoSenpai.PILE_2_O)};
		usePattern = false;
		coteDroits = new boolean[] {
				config.getBoolean(ConfigInfoSenpai.PILE_1_COTE_DROIT),
				config.getBoolean(ConfigInfoSenpai.PILE_2_COTE_DROIT)};
	}

	public void setPattern(CubeColor[] pattern)
	{
		this.pattern = pattern;
		usePattern = true;
		robot.setPattern(pattern);
	}
	
	public ScriptRecalage getScriptRecalage(long dureeRecalage)
	{
		return new ScriptRecalage(log, robot, table, cp, couleur.symmetry, dureeRecalage);		
	}
	
	public ScriptRecalage getScriptRecalage()
	{
		return getScriptRecalage(500);
	}
	
	public ScriptDomotiqueV2 getScriptDomotique()
	{
		return new ScriptDomotiqueV2(log, robot, table, cp, couleur.symmetry);
	}

	public ScriptDomotique getScriptDomotiqueVieux()
	{
		return new ScriptDomotique(log, robot, table, cp, couleur.symmetry);
	}

	public ScriptAbeille getScriptAbeille()
	{
		return new ScriptAbeille(log, robot, table, cp, couleur.symmetry);
	}

	public ScriptDeposeCube getDeposeScript()
	{
		int nbPile = robot.getNbPile(usePattern);
		return new ScriptDeposeCube(log, robot, table, cp, robot.getHauteurPile(nbPile), pilePosition[nbPile], anglesDepose[nbPile], coteDroits[nbPile], longueurGrue[robot.getHauteurPile(nbPile)], nbPile);
	}

	public class CubeComparator implements Comparator<Script>
	{
		private XYO position;
		
		public CubeComparator(XYO position)
		{
			this.position = position;
		}
		
		@Override
		public int compare(Script arg0, Script arg1) {
			XYO s1 = arg0.getPointEntree();
			XYO s2 = arg1.getPointEntree();
			return (int) (s1.position.squaredDistance(position.position) - s2.position.squaredDistance(position.position))
					+ (int) (XYO.angleDifference(s1.orientation, s2.orientation));
		}
		
	}
	
	public ScriptPriseCube getScriptPriseCube(CubeColor couleur, boolean bourrine)
	{
		return getAllPossible(couleur, bourrine).poll();
	}
	
	public PriorityQueue<ScriptPriseCube> getAllPossible(CubeColor couleur, boolean bourrine)
	{
		PriorityQueue<ScriptPriseCube> out = new PriorityQueue<ScriptPriseCube>(new CubeComparator(robot.getCinematique().getXYO()));
		
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
					log.write("Possible : "+c+" "+f, Subject.SCRIPT);
					out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,true));
					out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,false));
				}
			}
		}

		return out;
	}

	public ScriptPriseCube getScriptPriseCube(boolean bourrine)
	{
		return getAllPossible(bourrine).poll();
	}

	public PriorityQueue<ScriptPriseCube> getAllPossible(boolean bourrine)
	{
		PriorityQueue<ScriptPriseCube> out = new PriorityQueue<ScriptPriseCube>(new CubeComparator(robot.getCinematique().getXYO()));
		for(Cube c : Cube.values())
		{
			if(c == Cube.GOLDEN_CUBE_1 || c == Cube.GOLDEN_CUBE_2)
				continue;

			if(couleur.symmetry == c.position.getX() > 0)
				continue;
			for(CubeFace f : CubeFace.values())
				if(isFacePossible(c, f, bourrine))
				{
					log.write("Possible : "+c+" "+f, Subject.SCRIPT);
					out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,true));
					out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,false));
				}
//				else
//					log.write("Impossible : "+c+" "+f, Subject.SCRIPT);
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
		
//		log.write("v1 : "+voisin1, Subject.SCRIPT);
		
		if(p1 != null) // on vérifie ses voisins
		{
			Cube p2 = f.getVoisin(p1); // D
			boolean voisin2 = p2 == null || table.isDone(p2);
			
			Cube p3 = f.getOrthogonal(true).getVoisin(p1); // A
			boolean voisin3 = p3 == null || table.isDone(p3);
			
			Cube p4 = f.getOrthogonal(false).getVoisin(p1); // C
			boolean voisin4 = p4 == null || table.isDone(p4);
			
//			log.write("v1-4 : "+voisin1+" "+voisin2+" "+voisin3+" "+voisin4, Subject.SCRIPT);
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
