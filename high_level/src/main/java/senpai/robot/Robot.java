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

package senpai.robot;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
//import java.util.PriorityQueue;

import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.graphic.printable.Segment;
import pfg.kraken.Kraken;
import pfg.kraken.SearchParameters;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.robot.RobotState;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.capteurs.CapteursRobot;
import senpai.comm.CommProtocol;
import senpai.comm.DataTicket;
import senpai.comm.Ticket;
import senpai.comm.CommProtocol.Id;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.table.Cube;
import senpai.utils.ConfigInfoSenpai;
import senpai.utils.Severity;
import senpai.utils.Subject;

/**
 * Classe abstraite du robot, dont héritent RobotVrai et RobotChrono
 * 
 * @author pf
 */

public class Robot extends RobotState
{
	public enum State
	{
		STANDBY, // le robot est à l'arrêt
		READY_TO_GO, // une trajectoire a été envoyée
		STOPPING, // une commande de stop a été envoyée
		MOVING; // le robot se déplace
//		SCRIPT; // le robot est en plein script
	}
	
	private boolean isInScript = false;
	
	private Cube cubeTop = Cube.GOLDEN_CUBE_1;
	private Cube cubeInside = Cube.GOLDEN_CUBE_2;
	@SuppressWarnings("unchecked")
	private List<Cube>[] piles = (List<Cube>[]) new List[2];
	protected volatile boolean symetrie;
	protected Log log;
	protected Kraken kraken;
//	private volatile boolean modeDegrade = false;
	private List<ItineraryPoint> pathDegrade;
	private RectangularObstacle obstacle;
	private double angleMin, angleMax;
	private List<ItineraryPoint> path = null;
	private double angleTourelleGaucheOld = Double.MAX_VALUE, angleTourelleDroiteOld = Double.MAX_VALUE;
	private long dateDebutMatch;
	
	private boolean jumperOK = false;
	private volatile State etat = State.STANDBY;
	private final boolean simuleLL;
	private final boolean printTrace;
	private OutgoingOrderBuffer out;
	private GraphicDisplay buffer;
//	private KnownPathManager known;
	private RobotPrintable printable = null;
	private volatile boolean cinematiqueInitialised = false;
//	private boolean enableLoadPath;
	private int currentIndexTrajectory = 0, anticipationTourelle;
//	private boolean domotiqueDone = false;
	private int score;
	
	public Robot(Log log, OutgoingOrderBuffer out, Config config, GraphicDisplay buffer, Kraken kraken, /*DynamicPath dpath,*/ KnownPathManager known, RectangularObstacle obstacle)
	{
		this.log = log;
		this.out = out;
		this.buffer = buffer;
		this.kraken = kraken;
//		this.known = known;
		this.obstacle = obstacle;
		piles[0] = new ArrayList<Cube>();
		piles[1] = new ArrayList<Cube>();

		jumperOK = config.getBoolean(ConfigInfoSenpai.DISABLE_JUMPER);
		angleMin = config.getInt(ConfigInfoSenpai.ANGLE_MIN_TOURELLE) * Math.PI / 180;
		angleMax = config.getInt(ConfigInfoSenpai.ANGLE_MAX_TOURELLE) * Math.PI / 180;
		anticipationTourelle = (int) Math.round(config.getInt(ConfigInfoSenpai.ANTICIPATION_TOURELLE) / 20.);
		// On ajoute une fois pour toute l'image du robot
		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ROBOT_AND_SENSORS))
		{
			printable = new RobotPrintable(config);
			buffer.addPrintable(printable, Color.BLACK, Layer.MIDDLE.layer);
		}
		
//		enableLoadPath = config.getBoolean(ConfigInfoSenpai.ENABLE_KNOWN_PATHS);
		printTrace = config.getBoolean(ConfigInfoSenpai.GRAPHIC_TRACE_ROBOT);
		cinematique = new Cinematique(new XYO(
				config.getDouble(ConfigInfoSenpai.INITIAL_X),
				config.getDouble(ConfigInfoSenpai.INITIAL_Y),
				config.getDouble(ConfigInfoSenpai.INITIAL_O)));
		cinematique.enMarcheAvant = true;

		simuleLL = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);		
		score = 5;
		out.setScore(score);
//		setDegrade();
	}
	
	public int getNbPile(boolean usePattern)
	{
		if(usePattern && piles[0].size() >= 3)
			return 1;
		return 0;
	}
	
	public int getHauteurPile(int nbPile)
	{
		return piles[nbPile].size();
	}
	
	public void setEnMarcheAvance(boolean enMarcheAvant)
	{
		cinematique.enMarcheAvant = enMarcheAvant;
	}

	public boolean isCinematiqueInitialised()
	{
		return cinematiqueInitialised;
	}

	@Override
	public String toString()
	{
		return cinematique.toString();
	}

	private XY_RW oldPosition = new XY_RW();
	
	public void setCinematique(Cinematique cinematique)
	{
		this.cinematique.getPosition().copy(oldPosition);
		cinematique.copy(this.cinematique);
		obstacle.update(cinematique.getPosition(), cinematique.orientationReelle);

		/*
		 * On vient juste de récupérer la position initiale
		 */
		if(printTrace)
			synchronized(buffer)
			{
				// affichage
				if(oldPosition.distanceFast(cinematique.getPosition()) < 100)
					buffer.addPrintable(new Segment(oldPosition, cinematique.getPosition().clone()), Color.RED, Layer.MIDDLE.layer);
			}
	}


	/*
	 * DÉPLACEMENTS
	 */

	
	/*
	 * ACTIONNEURS
	 */

	/**
	 * Rend bloquant l'appel d'une méthode
	 * 
	 * @param m
	 * @throws InterruptedException
	 * @throws ActionneurException
	 */
	protected void bloque(String nom, Object... param) throws InterruptedException, ActionneurException
	{
		if(param == null || param.length == 0)
			log.write("Appel à " + nom, Subject.SCRIPT);
		else
		{
			String s = "";
			for(Object o : param)
			{
				if(!s.isEmpty())
					s += ", ";
				s += o;
			}
			log.write("Appel à " + nom + " (param = " + s + ")", Subject.SCRIPT);
		}

		if(simuleLL)
			return;

		Ticket t = null;
		Class<?>[] paramClasses = null;
		if(param.length > 0)
		{
			paramClasses = new Class[param.length];
			for(int i = 0; i < param.length; i++)
				paramClasses[i] = param[i].getClass();
		}
		long avant = System.currentTimeMillis();
		try
		{
			t = (Ticket) OutgoingOrderBuffer.class.getMethod(nom, paramClasses).invoke(out, param.length == 0 ? null : param);
		}
		catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
		{
			e.printStackTrace();
			throw new ActionneurException("Méthode inconnue : " + nom);
		}
		DataTicket dt = t.attendStatus();
		if(dt.status == CommProtocol.State.KO)
			throw new ActionneurException("Problème pour l'actionneur " + nom+" : "+dt.data);

		log.write("Temps d'exécution de " + nom + " : " + (System.currentTimeMillis() - avant), Subject.SCRIPT);
	}

	public void avance(double distance, double vitesseMax) throws InterruptedException, UnableToMoveException
	{
		if(distance >= 0)
			log.write("On avance de "+distance+" mm", Subject.TRAJECTORY);
		else
			log.write("On recule de "+(-distance)+" mm", Subject.TRAJECTORY);
		
		LinkedList<ItineraryPoint> ch = new LinkedList<ItineraryPoint>();
		double cos = Math.cos(cinematique.orientationReelle);
		double sin = Math.sin(cinematique.orientationReelle);
		int nbPoint = (int) Math.round(Math.abs(distance) / 20);
		double xFinal = cinematique.getPosition().getX() + distance * cos;
		double yFinal = cinematique.getPosition().getY() + distance * sin;
		boolean marcheAvant = distance > 0;
		if(nbPoint == 0)
		{
			// Le point est vraiment tout proche
			ch.add(new ItineraryPoint(xFinal, yFinal, cinematique.orientationReelle, 0, marcheAvant, vitesseMax, vitesseMax, true));
		}
		else
		{
			double deltaX = 20 * cos;
			double deltaY = 20 * sin;
			if(distance < 0)
			{
				deltaX = -deltaX;
				deltaY = -deltaY;
			}

			for(int i = 0; i < nbPoint; i++)
				ch.addFirst(new ItineraryPoint(xFinal - i * deltaX, yFinal - i * deltaY, cinematique.orientationReelle, 0, marcheAvant, vitesseMax, vitesseMax, i == 0));
			System.out.println("Trajectoire : "+ch);
		}

		out.destroyPointsTrajectoires(0);
		out.ajoutePointsTrajectoire(ch, true);

		pathDegrade = ch;
		setReady();

		DataTicket dt = followTrajectory();
		if(dt.data != null)
			throw new UnableToMoveException(dt.data.toString());
	}
	
	public void execute(CommProtocol.Id ordre, Object... param) throws InterruptedException, ActionneurException
	{
		bloque(ordre.getMethodName(), param);
	}

	public void poseCubes(double angle, int etage, int nbPile) throws InterruptedException, ActionneurException
	{
		if(cubeTop != null)
		{
			execute(Id.ARM_PUT_ON_PILE_S, angle, etage);
			piles[nbPile].add(cubeTop);
			cubeTop = null;
		}
		if(cubeInside != null)
		{
			execute(Id.ARM_TAKE_FROM_STORAGE);
			execute(Id.ARM_PUT_ON_PILE_S, angle, etage);
			piles[nbPile].add(cubeInside);
			cubeInside = null;
		}
	}
	
	public boolean canTakeCube()
	{
		return cubeTop == null;
	}
	
	public boolean isThereCubeInside()
	{
		return cubeInside != null;
	}

	public boolean isThereCubeTop()
	{
		return cubeTop != null;
	}

	private void setCubeInside(Cube c)
	{
		assert cubeInside == null;
		cubeInside = c;
	}

	private void setCubeTop(Cube c)
	{
		assert cubeTop == null;
		cubeTop = c;
	}
	
	public void storeCube(Cube c) throws InterruptedException, ActionneurException
	{
		if(cubeInside == null)
		{
			execute(Id.ARM_STORE_CUBE_INSIDE);
			setCubeInside(c);
		}
		else
		{
			execute(Id.ARM_STORE_CUBE_TOP);
			setCubeTop(c);
		}
	}

	public void setScore(int score)
	{
		out.setScore(score);
	}

	public void updateColorAndSendPosition(RobotColor c) throws InterruptedException
	{
		assert cinematique != null;
		symetrie = c.symmetry;

		// on applique la symétrie à la position initiale
		if(symetrie)
			setCinematique(new Cinematique(-cinematique.getPosition().getX(),
					cinematique.getPosition().getY(),
					Math.PI - cinematique.orientationReelle,
					cinematique.enMarcheAvant,
					cinematique.courbureReelle,
					false));

		// on envoie la position au LL
		out.setPosition(cinematique.getPosition(), cinematique.orientationReelle);
		Thread.sleep(100);
		cinematiqueInitialised = true;
	}

	/*
	 * On a besoin d'initialiser à part car elle est utilisée pour centre l'affichage graphique
	 */
	public void initPositionObject(Cinematique c)
	{
		cinematique.copy(c);
		cinematique = c;
		
		// on active le printable
		if(printable != null)
			printable.initPositionObject(cinematique);
	}

	public synchronized void setReady()
	{
		assert etat == State.STANDBY;
		etat = State.READY_TO_GO;
		notifyAll();
	}
	
	public DataTicket goTo(XYO destination) throws PathfindingException, InterruptedException, UnableToMoveException
	{
		return goTo(new SearchParameters(cinematique.getXYO(), destination));
	}
	
	public DataTicket goTo(SearchParameters sp) throws PathfindingException, InterruptedException, UnableToMoveException
	{
//		PriorityQueue<SavedPath> allSaved = null;
/*		if(enableLoadPath)
		{
			allSaved = known.loadCompatiblePath(sp);			
			if(allSaved != null && allSaved.isEmpty())
				log.write("Aucun chemin connu pour : "+sp, Subject.TRAJECTORY);
		}*/
		
//		if(modeDegrade)
		long avant = System.currentTimeMillis();
		kraken.initializeNewSearch(sp);
		log.write("Durée d'initialisation de Kraken : "+(System.currentTimeMillis() - avant), Subject.TRAJECTORY);

/*		if(allSaved != null)
			while(!allSaved.isEmpty())
			{
				SavedPath saved = allSaved.poll();
//				try
//				{
//					if(modeDegrade)
//					{
						if(kraken.checkPath(saved.path))
						{
							log.write("On réutilise un chemin en mode dégradé : "+saved.name, Subject.TRAJECTORY);
							path = saved.path;
							break;
						}
						else
							log.write("Chemin inadapté", Subject.TRAJECTORY);
/*					}
					else
					{
						log.write("On démarre la recherche continue avec un chemin initial : "+saved.name, Subject.TRAJECTORY);
						kraken.startContinuousSearchWithInitialPath(sp, saved.path);
						path = saved.path;
						break;
					}
				}
				catch(PathfindingException e)
				{
					log.write("Chemin inadapté : "+e.getMessage(), Subject.TRAJECTORY);
				}*/
//			}

//		if(modeDegrade)
//		{

//			System.out.println(path);
			// On cherche et on envoie
//			if(path == null)
//			{
				log.write("On cherche un chemin en mode dégradé", Subject.TRAJECTORY);
				avant = System.currentTimeMillis();
				path = kraken.search();
				log.write("Durée de la recherche : "+(System.currentTimeMillis() - avant), Subject.TRAJECTORY);
//			}
//			else
//				log.write("On réutilise un chemin déjà connu !", Subject.TRAJECTORY);
			if(!simuleLL)
			{
				log.write("On envoie la trajectoire initiale en mode dégradé", Subject.TRAJECTORY);
				out.destroyPointsTrajectoires(0);
				out.ajoutePointsTrajectoire(path, true);
			}
			setReady();
			pathDegrade = path;
/*		}
		else
		{
			if(path == null)
			{
				log.write("On cherche un chemin en mode continu", Subject.TRAJECTORY);
				kraken.startContinuousSearch(sp);
			}
//			else
//				log.write("On réutilise un chemin déjà connu !", Subject.TRAJECTORY);
		}*/

		DataTicket out = followTrajectory();
/*		if(!modeDegrade)
		{
			log.write("Fin de la recherche en mode continu", Subject.TRAJECTORY);
			kraken.endContinuousSearch();
		}*/
		if(!simuleLL && out.data != null)
			throw new UnableToMoveException(out.data.toString());
		return out;
	}
	
	private DataTicket followTrajectory() throws InterruptedException
	{
		// non, marche pas avec avancer et reculer
//		assert modeDegrade == (pathDegrade != null) : modeDegrade+" "+pathDegrade;
		assert etat == State.READY_TO_GO || etat == State.STANDBY;

		log.write("Attente de la trajectoire…", Subject.TRAJECTORY);

		assert etat == State.READY_TO_GO; // parce que mode dégradé
		synchronized(this)
		{
			while(etat == State.STANDBY)
				wait();
		}

		if(!jumperOK)
		{
			log.write("La trajectoire est prête : attente du jumper !", Subject.TRAJECTORY);
			out.waitForJumper().attendStatus();
			out.startMatchChrono();
			jumperOK = true;
		}
		
		log.write("On commence à suivre la trajectoire de "+pathDegrade.size()+" points", Subject.TRAJECTORY);

		assert etat == State.READY_TO_GO;
		setMoving();
		
		DataTicket dt;
		
		if(!simuleLL)
		{
			Ticket t = out.followTrajectory();
		
			dt = t.attendStatus();
//			assert etat != State.MOVING : etat;
//			while(etat == State.MOVING)
//				wait();
			if(dt.data == null)
				log.write("Le robot a fini correctement la trajectoire. Position finale : "+cinematique.getXYO(), Subject.TRAJECTORY);
			else
				log.write("Le robot s'est arrêté suite à un problème : "+dt.data, Severity.CRITICAL, Subject.TRAJECTORY);
		}
		else
		{
			dt = new DataTicket(/*modeDegrade ?*/ pathDegrade /*: dpath.getPath()*/, CommProtocol.State.OK);
		}
		

		pathDegrade = null;
		path = null;
		etat = State.STANDBY;
		return dt;
	}

	public synchronized void setStopping()
	{
		etat = State.STOPPING;
		notifyAll();
	}

	
	private synchronized void setMoving()
	{
		etat = State.MOVING;
		notifyAll();
	}

	public synchronized boolean isStandby()
	{
		return etat == State.STANDBY;
	}

/*	public synchronized void setDegrade()
	{
		if(!modeDegrade)
		{
			log.write("Le robot entre en mode dégradé !", Severity.WARNING, Subject.STATUS);
			kraken.endAutoReplanning();
			modeDegrade = true;
			notifyAll();
		}
	}
	
	public boolean isDegrade() {
		return modeDegrade;
	}*/

	public boolean needCollisionCheck()
	{
		return etat == State.MOVING;
	}

	public List<ItineraryPoint> getPath()
	{
		assert etat == State.READY_TO_GO || etat == State.MOVING;
		return pathDegrade;
	}

	public boolean isProcheRobot(XY positionVue, int distance)
	{
		return obstacle.isProcheObstacle(positionVue, distance);
	}
	
	XY_RW objTourelle = new XY_RW();
	XY tourelleGauche = CapteursRobot.TOURELLE_GAUCHE.pos;
	XY tourelleDroite = CapteursRobot.TOURELLE_DROITE.pos;

	public void updateTourelles()
	{
		double angleDefautGauche, angleDefautDroite;
		if(cinematique.enMarcheAvant)
		{
			angleDefautGauche = angleMin;
			angleDefautDroite = -angleMin;
		}
		else
		{
			angleDefautGauche = angleMax;
			angleDefautDroite = -angleMax;
		}
		
		if(isInScript)
			out.setTourellesAngles(Math.PI / 2, -Math.PI / 2);
			
		else if(path == null)
			envoieAnglesTourelles(angleDefautGauche, angleDefautDroite);

		else
		{
			int pointVise = Math.min(currentIndexTrajectory + anticipationTourelle, path.size() - 1);
			ItineraryPoint ip = path.get(pointVise);
			objTourelle.setX(ip.x);
			objTourelle.setY(ip.y);
			double angleGauche = objTourelle.minus(tourelleGauche).minus(cinematique.getPosition()).getArgument() - cinematique.orientationReelle;
			angleGauche = XYO.angleDifference(angleGauche, 0);
			if(angleGauche > angleMin && angleGauche < angleMax)
				envoieAnglesTourelles(angleGauche, angleDefautDroite);
			else
			{
				objTourelle.setX(ip.x);
				objTourelle.setY(ip.y);
				double angleDroite = objTourelle.minus(tourelleDroite).minus(cinematique.getPosition()).getArgument() - cinematique.orientationReelle;
				angleDroite = XYO.angleDifference(angleDroite, 0);
				if(angleDroite < -angleMin && angleDroite > -angleMax)
					envoieAnglesTourelles(angleDefautGauche, angleDroite);
				else
					envoieAnglesTourelles(angleDefautGauche, angleDefautDroite);
			}
		}
	}
	
	private void envoieAnglesTourelles(double angleTourelleGauche, double angleTourelleDroite)
	{
		if(Math.abs(angleTourelleGauche - angleTourelleGaucheOld) > 0.1 || Math.abs(angleTourelleDroite - angleTourelleDroiteOld) > 0.1)
		{
			angleTourelleGaucheOld = angleTourelleGauche;
			angleTourelleDroiteOld = angleTourelleDroite;
			out.setTourellesAngles(angleTourelleGauche, angleTourelleDroite);
		}
	}

	public void setCurrentTrajectoryIndex(Cinematique current, int indexTrajectory)
	{	
		currentIndexTrajectory = indexTrajectory;
//		chemin.setCurrentTrajectoryIndex(indexTrajectory);
		if(cinematiqueInitialised)
			setCinematique(current);
	}

	public void setDomotiqueDone()
	{
//		domotiqueDone = true;
		score += 25;
		out.setScore(score);
	}
	
	public void beginScript()
	{
		isInScript = true;
	}
	
	public void endScript()
	{
		isInScript = false;
	}

	public int getIndexTrajectory()
	{
		return currentIndexTrajectory;
	}

	public void printTemps()
	{
		log.write("Temps depuis le début du match : "+(System.currentTimeMillis() - dateDebutMatch), Subject.STATUS);
	}
	
	public void setDateDebutMatch()
	{
		dateDebutMatch = System.currentTimeMillis();
	}

	public void correctPosition(XY_RW position, double orientation)
	{
		cinematique.updateReel(cinematique.getPosition().getX() + position.getX(),
				cinematique.getPosition().getY() + position.getY(),
				cinematique.orientationReelle + orientation, cinematique.courbureReelle);
		out.correctPosition(position, orientation);
	}
}
