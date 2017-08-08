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

package tests.lowlevel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import robot.RobotReal;
import robot.Speed;
import senpai.ConfigInfoSenpai;
import senpai.Subject;
import serie.BufferOutgoingOrder;
import tests.JUnit_Test;

/**
 * Tests unitaires des trajectoires et des actionneurs
 * 
 * @author pf
 *
 */

public class JUnit_Robot extends JUnit_Test
{

	private RobotReal robot;
//	private AStarCourbe astar;
//	private CheminPathfinding chemin;
//	private RealGameState state;
//	private PathCache pathcache;
	private BufferOutgoingOrder data;
//	private Cinematique c = null;
	private boolean simuleSerie;
	private Speed v;
	private double last;

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
			Method[] methodes = JUnit_Robot.class.getDeclaredMethods();
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
	@Override
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
//		state = container.getService(RealGameState.class);
		robot = container.getService(RobotReal.class);
//		chemin = container.getService(CheminPathfinding.class);
//		astar = container.getService(AStarCourbe.class);
//		pathcache = container.getService(PathCache.class);
		data = container.getService(BufferOutgoingOrder.class);
		simuleSerie = config.getBoolean(ConfigInfoSenpai.SIMULE_SERIE);
		data.startStream();
		v = Speed.TEST;
		log.write("Vitesse du robot : " + v.translationalSpeed * 1000, Subject.DUMMY);
	}

}
