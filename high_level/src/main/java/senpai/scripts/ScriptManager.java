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
			pilePosition.setX(-pilePosition.getX());
			anglesDepose = Math.PI - anglesDepose;
			coteDroit = !coteDroit;
		}
	}
	
	private boolean usePattern;
	private CubeColor[] pattern;
	private XY_RW pilePosition;
	private double anglesDepose;
	private double[] longueurGrue = new double[]{300, 300, 290}; // longueur de la grue en fonction du nombre de cube déjà posés
	private boolean coteDroit;
	private int distanceToScript = 0;
	private boolean interrupteurRehausse;
	
	public ScriptManager(Log log, Config config, Table table, Robot robot, CapteursProcess cp, ObstaclesDynamiques obsDyn)
	{
		interrupteurRehausse = config.getBoolean(ConfigInfoSenpai.INTERRUPTEUR_REHAUSSE);
		this.obsDyn = obsDyn;
		this.log = log;
		this.table = table;
		this.robot = robot;
		this.cp = cp;
		pilePosition = new XY_RW(config.getDouble(ConfigInfoSenpai.PILE_1_X),config.getDouble(ConfigInfoSenpai.PILE_1_Y));
		anglesDepose = config.getDouble(ConfigInfoSenpai.PILE_1_O);
		usePattern = false;
		coteDroit = config.getBoolean(ConfigInfoSenpai.PILE_1_COTE_DROIT);
	}

	public void setPattern(CubeColor[] pattern)
	{
		this.pattern = pattern;
		usePattern = true;
		robot.setPattern(pattern);
	}
	
	public ScriptRecalageInitial getScriptRecalageInitial()
	{
		return new ScriptRecalageInitial(log, robot, table, cp, 1000);
	}
	
	public ScriptRecalage getScriptRecalage()
	{
		return new ScriptRecalage(log, robot, table, cp, couleur.symmetry, 500);		
	}
	
	public ScriptDomotiqueV2 getScriptDomotique()
	{
		return new ScriptDomotiqueV2(log, robot, table, interrupteurRehausse, cp, couleur.symmetry);
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
		return new ScriptDeposeCube(log, robot, table, cp, robot.getHauteurPile(), pilePosition, anglesDepose, coteDroit, longueurGrue[robot.getHauteurPile()], distanceToScript);
	}

	public class CubeComparator implements Comparator<ScriptPriseCube>
	{
		private XYO position;
		
		public CubeComparator(XYO position)
		{
			this.position = position;
		}
		
		@Override
		public int compare(ScriptPriseCube arg0, ScriptPriseCube arg1) {
			return arg0.getDistance(position) - arg1.getDistance(position);
		}
		
	}
	
	public PriorityQueue<ScriptPriseCube> getAllPossible(CubeColor couleur1, CubeColor couleur2, boolean bourrine)
	{
		log.write("Recherche de "+couleur1+(couleur2 == null ? "" : " et de "+couleur2)+(bourrine ? " en bourrinant." : "."), Subject.SCRIPT);

		PriorityQueue<ScriptPriseCube> out = new PriorityQueue<ScriptPriseCube>(new CubeComparator(robot.getCinematique().getXYO()));
		
		for(Croix croix : Croix.values())
		{
			for(CubeFace f : CubeFace.values())
			{
				Cube c = Cube.getCube(croix, couleur1);
				if(isFacePossible(c, f, false))
				{
					log.write("Possible : "+c+" "+f, Subject.SCRIPT);
					out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,true,false));
					out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,false,false));
				}
				else if(bourrine && isFacePossible(c, f, true))
				{
					log.write("Possible : "+c+" "+f, Subject.SCRIPT);
					out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,true,true));
					out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,false,true));					
				}
				if(couleur2 != null)
				{
					c = Cube.getCube(croix, couleur2);
					if(isFacePossible(c, f, false))
					{
						log.write("Possible : "+c+" "+f, Subject.SCRIPT);
						out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,true,false));
						out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,false,false));
					}					
					else if(bourrine && isFacePossible(c, f, true))
					{
						log.write("Possible : "+c+" "+f, Subject.SCRIPT);
						out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,true,true));
						out.add(new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,false,true));
					}
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
					ScriptPriseCube s1 = new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,true,bourrine);
//					System.out.println(robot.getCinematique().getXYO()+" "+s1.getPointEntree()+" "+s1.getDistance(robot.getCinematique().getXYO())+" "+s1);
					out.add(s1);
					ScriptPriseCube s2 = new ScriptPriseCube(log,robot, table, cp, obsDyn, c,f,false,bourrine);
//					System.out.println(robot.getCinematique().getXYO()+" "+s1.getPointEntree()+" "+s2.getDistance(robot.getCinematique().getXYO())+" "+s2);
					out.add(s2);
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

		
//		log.write("v1 : "+voisin1+" "+p1+" "+table.isDone(p1), Subject.SCRIPT);
		
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

	public PriorityQueue<ScriptPriseCube> getFirstPatternColor()
	{
		if(usePattern)
		{
			log.write("Recherche de scripts pour le premier cube avec pattern. Couleurs cherchées : "+pattern[0]+" et "+pattern[2], Subject.SCRIPT);
			PriorityQueue<ScriptPriseCube> out = getAllPossible(pattern[0], pattern[2], false); 
			if(!out.isEmpty())
				return out;
			log.write("Couleurs introuvables, pattern désactivé", Subject.SCRIPT);
			usePattern = false;
		}
		return getAllPossible(false);
	}

	public PriorityQueue<ScriptPriseCube> getSecondPatternColor()
	{
		if(usePattern)
		{
			// on vérifie qu'on a bien le premier cube, sinon on annule
			if(robot.isThereCubeInside())
			{
				log.write("Recherche de scripts pour le second cube avec pattern. Couleur cherchée : "+pattern[1], Subject.SCRIPT);
				PriorityQueue<ScriptPriseCube> out = getAllPossible(pattern[1], null, true);
				if(!out.isEmpty())
					return out;
				log.write("Couleur introuvable, pattern désactivé", Subject.SCRIPT);
				usePattern = false;
			}
			else
			{
				log.write("Le robot veut un second cube de pattern mais n'a pas le premier.", Subject.SCRIPT);
				return new PriorityQueue<ScriptPriseCube>();
			}
		}
		return getAllPossible(false);
	}
}
