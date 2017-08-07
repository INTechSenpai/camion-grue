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
import java.util.LinkedList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import capteurs.SensorMode;
import config.ConfigInfo;
import graphic.PrintBuffer;
import obstacles.types.ObstacleCircular;
import pathfinding.KeyPathCache;
import pathfinding.PathCache;
import pathfinding.RealGameState;
import pathfinding.astar.arcs.ArcCourbeDynamique;
import pathfinding.astar.arcs.BezierComputer;
import pathfinding.chemin.CheminPathfinding;
import robot.Cinematique;
import robot.CinematiqueObs;
import robot.RobotColor;
import robot.RobotReal;
import robot.Speed;
import scripts.ScriptNames;
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
	private RealGameState state;
	private PathCache pathcache;
	private BufferOutgoingOrder data;
	private Cinematique c = null;
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
		state = container.getService(RealGameState.class);
		robot = container.getService(RobotReal.class);
//		chemin = container.getService(CheminPathfinding.class);
//		astar = container.getService(AStarCourbe.class);
		pathcache = container.getService(PathCache.class);
		data = container.getService(BufferOutgoingOrder.class);
		simuleSerie = config.getBoolean(ConfigInfo.SIMULE_SERIE);
		data.startStream();
		v = Speed.TEST;
		log.debug("Vitesse du robot : " + v.translationalSpeed * 1000);
	}

	/**
	 * Pas un test
	 */
	@Override
	@After
	public void tearDown() throws Exception
	{
		data.stopStream();
		log.debug("Position robot : " + robot.getCinematique().getPosition());
		log.debug("Orientation robot : " + robot.getCinematique().orientationReelle);
		if(!simuleSerie && c != null)
		{
			log.debug("Position voulue : " + c.getPosition());
			log.debug("Erreur position : " + robot.getCinematique().getPosition().distance(c.getPosition()));
			log.debug("Orientation voulue : " + last);
			double deltaO = robot.getCinematique().orientationReelle - last;
			if(deltaO > Math.PI)
				deltaO -= 2 * Math.PI;
			else if(deltaO < -Math.PI)
				deltaO += 2 * Math.PI;
			log.debug("Erreur orientation : " + deltaO);
		}

		super.tearDown();
	}

	/**
	 * Test AX-12 filet
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_ax12() throws Exception
	{
		robot.baisseFilet();
		robot.leveFilet();
		robot.bougeFiletMiChemin();
	}
	
	@Test
	public void test_capteurs() throws Exception
	{
		robot.setSensorMode(SensorMode.BACK_AND_SIDES);
		Thread.sleep(10000);
	}
	
	/**
	 * Test de l'attrape-rêve
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_actionneurs() throws Exception
	{
		robot.ejecteBalles();
		robot.rearme();
		robot.ouvreFilet();
		robot.fermeFilet();
		robot.verrouilleFilet();
		robot.traverseBascule();
	}

	@Test
	public void test_correction() throws Exception
	{
		Cinematique depart = new Cinematique(-300, 1750, 0, true, 0);
		robot.setCinematique(depart);
		data.setPosition(depart.getPosition(), depart.orientationReelle);
		Thread.sleep(100); // on attend un peu que la position soit affectée bas niveau
		if(!simuleSerie)
			robot.avance(400, Speed.STANDARD);
		log.debug("Odométrie finale : "+robot.getCinematique());
	}

	/**
	 * Va au cratère droit
	 */
	@Test
	public void depart_jaune_cratere_droit_HL_prehension() throws Exception
	{
		try
		{
			Cinematique depart = new Cinematique(550, 1905, -Math.PI / 2, true, 0);
			robot.setCinematique(depart);
			data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																				// envoie
																				// la
																				// position
																				// haut
																				// niveau
			Thread.sleep(100); // on attend un peu que la position soit affectée
								// bas niveau
			pathcache.computeAndFollow(new KeyPathCache(state, ScriptNames.SCRIPT_CRATERE_HAUT_DROITE, false), Speed.STANDARD);
			ScriptNames.SCRIPT_CRATERE_HAUT_DROITE.s.execute(state);
			pathcache.computeAndFollow(new KeyPathCache(state, ScriptNames.SCRIPT_DEPOSE_MINERAI_DROITE, false), Speed.STANDARD);
			ScriptNames.SCRIPT_DEPOSE_MINERAI_DROITE.s.execute(state);
		}
		catch(Exception e)
		{
			e.printStackTrace(log.getPrintWriter());
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * Va au cratère gauche
	 */
	@Test
	public void depart_bleu_cratere_gauche_HL_prehension() throws Exception
	{
		try
		{
			config.set(ConfigInfo.COULEUR, RobotColor.BLEU);
			Cinematique depart = new Cinematique(-550, 1905, -Math.PI / 2, true, 0);
			robot.setCinematique(depart);
			data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																				// envoie
																				// la
																				// position
																				// haut
																				// niveau
			Thread.sleep(100); // on attend un peu que la position soit affectée
								// bas niveau
			pathcache.computeAndFollow(new KeyPathCache(state, ScriptNames.SCRIPT_CRATERE_HAUT_GAUCHE, false), Speed.STANDARD);
			ScriptNames.SCRIPT_CRATERE_HAUT_GAUCHE.s.execute(state);
			pathcache.computeAndFollow(new KeyPathCache(state, ScriptNames.SCRIPT_DEPOSE_MINERAI_GAUCHE, false), Speed.STANDARD);
			ScriptNames.SCRIPT_DEPOSE_MINERAI_GAUCHE.s.execute(state);
		}
		catch(Exception e)
		{
			e.printStackTrace(log.getPrintWriter());
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test
	public void depart_bleu_depose() throws Exception
	{
		try
		{
			config.set(ConfigInfo.COULEUR, RobotColor.BLEU);
			Thread.sleep(300);
			Cinematique depart = new Cinematique(-550, 1905, -Math.PI / 2, true, 0);
			robot.setCinematique(depart);
			data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																				// envoie
																				// la
																				// position
																				// haut
																				// niveau
			Thread.sleep(100); // on attend un peu que la position soit affectée
								// bas niveau
			pathcache.computeAndFollow(new KeyPathCache(state, ScriptNames.SCRIPT_DEPOSE_MINERAI_GAUCHE, false), Speed.STANDARD);
			ScriptNames.SCRIPT_DEPOSE_MINERAI_GAUCHE.s.execute(state);
		}
		catch(Exception e)
		{
			e.printStackTrace(log.getPrintWriter());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Va au cratère droit
	 */
	@Test
	public void depart_jaune_cratere_droit_HL_shoot() throws Exception
	{
		try
		{
			Cinematique depart = new Cinematique(550, 1905, -Math.PI / 2, true, 0);
			robot.setCinematique(depart);
			data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																				// envoie
																				// la
																				// position
																				// haut
																				// niveau
			Thread.sleep(100); // on attend un peu que la position soit affectée
								// bas niveau
			pathcache.computeAndFollow(new KeyPathCache(state, ScriptNames.SCRIPT_CRATERE_HAUT_DROITE, true), Speed.STANDARD);
		}
		catch(Exception e)
		{
			e.printStackTrace(log.getPrintWriter());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Va au cratère gauche
	 */
	@Test
	public void depart_jaune_cratere_gauche_HL() throws Exception
	{
		try
		{
			Cinematique depart = new Cinematique(550, 1905, -Math.PI / 2, true, 0);
			robot.setCinematique(depart);
			data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																				// envoie
																				// la
																				// position
																				// haut
																				// niveau
			Thread.sleep(100); // on attend un peu que la position soit affectée
								// bas niveau
			pathcache.computeAndFollow(new KeyPathCache(state, ScriptNames.SCRIPT_CRATERE_HAUT_GAUCHE, false), Speed.STANDARD);
		}
		catch(Exception e)
		{
			e.printStackTrace(log.getPrintWriter());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Trajectoire tout droit
	 */
	@Test
	public void depart_jaune_HL() throws Exception
	{
		try
		{
			Cinematique depart = new Cinematique(550, 1905, -Math.PI / 2, true, 0);
			robot.setCinematique(depart);
			data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																				// envoie
																				// la
																				// position
																				// haut
																				// niveau
			Thread.sleep(100); // on attend un peu que la position soit affectée
								// bas niveau
			c = new Cinematique(550, 1000, Math.PI, false, 0);
			pathcache.computeAndFollow(new KeyPathCache(state, c, true), Speed.STANDARD);
		}
		catch(Exception e)
		{
			e.printStackTrace(log.getPrintWriter());
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void test_arc_cercle() throws Exception
	{
		BezierComputer bezier = container.getService(BezierComputer.class);
		PrintBuffer buffer = container.getService(PrintBuffer.class);
	
		Cinematique c = new Cinematique(441.63,1589.78, 9.405, true, 0);
		data.setPosition(c.getPosition(), c.orientationReelle); // on envoie la
																// position haut
																// niveau
		Thread.sleep(1000);
		ScriptNames.SCRIPT_CRATERE_HAUT_DROITE.s.setUpCercleArrivee();
		log.debug("Initial : " + c);
		ArcCourbeDynamique a = bezier.trajectoireCirculaireVersCentre(c);

		LinkedList<CinematiqueObs> path = new LinkedList<CinematiqueObs>();
		CheminPathfinding chemin = container.getService(CheminPathfinding.class);
		for(CinematiqueObs co : a.arcs)
		{
			path.add(co);
			buffer.addSupprimable(new ObstacleCircular(co.getPosition(), 4));
		}

		chemin.addToEnd(path);
		if(!simuleSerie)
			robot.followTrajectory(v);
	}


	@Test
	public void recule() throws Exception
	{
		Cinematique depart = new Cinematique(0, 1600, Math.PI / 2, true, 0);
		robot.setCinematique(depart);
		data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																			// envoie
																			// la
																			// position
																			// haut
																			// niveau
		Thread.sleep(500);
		if(!simuleSerie)
			robot.avance(-200, v);
	}
	
	/**
	 * Le robot avance de 20cm
	 * 
	 * @throws Exception
	 */
	@Test
	public void avance() throws Exception
	{
		Cinematique depart = new Cinematique(0, 1800, -Math.PI / 2, true, 0);
		robot.setCinematique(depart);
		data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																			// envoie
																			// la
																			// position
																			// haut
																			// niveau
		Thread.sleep(500);
		if(!simuleSerie)
			robot.avance(200, v);
	}

	
	@Test
	public void avance_un_peu() throws Exception
	{
		Cinematique depart = new Cinematique(0, 1800, -Math.PI / 2, true, 0);
		robot.setCinematique(depart);
		data.setPosition(depart.getPosition(), depart.orientationReelle); // on
																			// envoie
																			// la
																			// position
																			// haut
																			// niveau
		Thread.sleep(500);
		if(!simuleSerie)
			robot.avance(20, v);
	}
	
}
