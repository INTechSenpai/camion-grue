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

package senpai;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import pfg.kraken.utils.XY;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.capteurs.CapteursProcess;
import senpai.comm.CommProtocol.Id;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.robot.Speed;
import senpai.scripts.ScriptManager;
import senpai.scripts.ScriptPriseCube;
import senpai.table.Croix;
import senpai.table.Cube;
import senpai.table.CubeColor;
import senpai.table.CubeFace;
import senpai.table.Table;
import senpai.utils.ConfigInfoSenpai;
import senpai.utils.Subject;

/**
 * Tests unitaires des trajectoires et des actionneurs
 * 
 * @author pf
 *
 */

public class Test_Robot extends JUnit_Test
{

	private Robot robot;
	private Table table;
//	private AStarCourbe astar;
//	private CheminPathfinding chemin;
//	private RealGameState state;
//	private PathCache pathcache;
	private OutgoingOrderBuffer data;
	private ScriptManager scripts;
	private CapteursProcess cp;
//	private Cinematique c = null;
	private boolean simuleSerie;
	private Speed v;
	private double last;

	@Test
	public void test_cube() throws Exception
	{
		for(Croix croix : Croix.values())
			for(CubeColor couleur : CubeColor.values())
				if(couleur != CubeColor.GOLDEN)
					for(CubeFace face : CubeFace.values())
					{
						new ScriptPriseCube(log, robot, table, cp, null, croix, couleur, face, true);
						new ScriptPriseCube(log, robot, table, cp, null, croix, couleur, face, false);
					}
	}
	
	@Test
	public void test_cube2() throws Exception
	{
		scripts.setCouleur(RobotColor.ORANGE);
		table.updateCote(RobotColor.ORANGE.symmetry);
		scripts.getAllPossible(false);
		table.setDone(Cube.CROIX_CENTRE_DROITE_CUBE_DROITE);
		scripts.getAllPossible(false);
	}
	
	/**
	 * Génère un fichier qui présente les tests
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		BufferedWriter writer = null;
		try
		{
			writer = new BufferedWriter(new FileWriter("liste-tests.txt"));
			Method[] methodes = Test_Robot.class.getDeclaredMethods();
			for(Method m : methodes)
				if(m.isAnnotationPresent(Test.class))
					writer.write("./run_junit.sh tests.lowlevel.JUnit_Robot#" + m.getName() + "\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(writer != null)
				try
				{
					writer.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			System.out.println("Génération de la liste des tests terminée.");
		}
	}

	/**
	 * Pas un test
	 */
	@Before
	public void setUp() throws Exception
	{
		setUp("default");
		
		// il est nécessaire que les communications ne soient pas simulées
//		assert !config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);

//		state = container.getService(RealGameState.class);
		robot = container.getService(Robot.class);
//		chemin = container.getService(CheminPathfinding.class);
//		astar = container.getService(AStarCourbe.class);
//		pathcache = container.getService(PathCache.class);
		data = container.getService(OutgoingOrderBuffer.class);
		scripts = container.getService(ScriptManager.class);
		cp = container.getService(CapteursProcess.class);
		table = container.getService(Table.class);
		simuleSerie = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);
		data.startStream(Id.ODO_AND_SENSORS);
		v = Speed.TEST;
		log.write("Vitesse du robot : " + v.translationalSpeed * 1000, Subject.STATUS);
	}

}
