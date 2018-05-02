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
import java.util.List;
import java.util.PriorityQueue;
import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.graphic.printable.Segment;
import pfg.kraken.Kraken;
import pfg.kraken.SearchParameters;
import pfg.kraken.astar.autoreplanning.DynamicPath;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.robot.RobotState;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.buffer.OutgoingOrderBuffer;
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
		MOVING; // le robot se déplace
	}
	
	private Cube cubeTop = null;
	private Cube cubeInside = null;
	protected volatile boolean symetrie;
	protected Log log;
	protected Kraken kraken;
	private DynamicPath dpath;
	private volatile boolean modeDegrade = false;
	private List<ItineraryPoint> pathDegrade;

	@Override
	public String toString()
	{
		return cinematique.toString();
	}

	private volatile State etat = State.STANDBY;
	private boolean simuleLL;
	private boolean printTrace;
	private OutgoingOrderBuffer out;
	private GraphicDisplay buffer;
	private KnownPathManager known;
	private RobotPrintable printable = null;
	private volatile boolean cinematiqueInitialised = false;
	private boolean enableLoadPath;
	
	// Constructeur
	public Robot(Log log, OutgoingOrderBuffer out, Config config, GraphicDisplay buffer, Kraken kraken, DynamicPath dpath, KnownPathManager known)
	{
		this.log = log;
		this.out = out;
		this.buffer = buffer;
		this.kraken = kraken;
		this.dpath = dpath;
		this.known = known;
		
		// On ajoute une fois pour toute l'image du robot
		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ROBOT_AND_SENSORS))
		{
			printable = new RobotPrintable(config);
			buffer.addPrintable(printable, Color.BLACK, Layer.MIDDLE.layer);
		}
		
		enableLoadPath = config.getBoolean(ConfigInfoSenpai.ENABLE_KNOWN_PATHS);
		printTrace = config.getBoolean(ConfigInfoSenpai.GRAPHIC_TRACE_ROBOT);
		cinematique = new Cinematique(new XYO(
				config.getDouble(ConfigInfoSenpai.INITIAL_X),
				config.getDouble(ConfigInfoSenpai.INITIAL_Y),
				config.getDouble(ConfigInfoSenpai.INITIAL_O)));
		simuleLL = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);
	}
	
	public void setEnMarcheAvance(boolean enMarcheAvant)
	{
		cinematique.enMarcheAvant = enMarcheAvant;
	}

	public boolean isCinematiqueInitialised()
	{
		return cinematiqueInitialised;
	}

	private XY_RW oldPosition = new XY_RW();
	
	public void setCinematique(Cinematique cinematique)
	{
		this.cinematique.getPosition().copy(oldPosition);
		cinematique.copy(this.cinematique);
		/*
		 * On vient juste de récupérer la position initiale
		 */
		if(!cinematiqueInitialised)
		{
			cinematiqueInitialised = true;
//			notifyAll();
		}
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

		CommProtocol.State etat;
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
		etat = t.attendStatus().status;
		if(etat == CommProtocol.State.KO)
			throw new ActionneurException("Problème pour l'actionneur " + nom);

		log.write("Temps d'exécution de " + nom + " : " + (System.currentTimeMillis() - avant), Subject.SCRIPT);
	}

	public void avance(double distance, Speed speed)
			throws UnableToMoveException, InterruptedException {
		// TODO Auto-generated method stub
		
	}
	
	public void execute(CommProtocol.Id ordre, Object... param) throws InterruptedException, ActionneurException
	{
		bloque(ordre.getMethodName(), param);
	}

	public void poseCube(double angle, int etage) throws InterruptedException, ActionneurException
	{
		if(cubeTop != null)
		{
			execute(Id.ARM_PUT_ON_PILE_S, angle, etage);
			cubeTop = null;
		}
		else if(cubeInside != null)
		{
			execute(Id.ARM_TAKE_FROM_STORAGE);
			execute(Id.ARM_PUT_ON_PILE_S, angle, etage);
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
		execute(Id.ARM_STORE_CUBE_TOP);
		setCubeTop(c);
	}

	public void setScore(int score)
	{
		out.setScore(score);
	}

	public void updateColorAndSendPosition(RobotColor c)
	{
		assert cinematique != null;
		
		symetrie = c.symmetry;

		// on applique la symétrie à la position initiale
		if(symetrie)
			setCinematique(new Cinematique(-cinematique.getPosition().getX(),
					cinematique.getPosition().getY(),
					Math.PI + cinematique.orientationReelle,
					cinematique.enMarcheAvant,
					cinematique.courbureReelle,
					false));

		// on envoie la position au LL
		out.setPosition(cinematique.getPosition(), cinematique.orientationReelle);
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
	
	public DataTicket goTo(XYO destination) throws PathfindingException, InterruptedException
	{
		return goTo(new SearchParameters(cinematique.getXYO(), destination));
	}
	
	public DataTicket goTo(SearchParameters sp) throws PathfindingException, InterruptedException
	{
		PriorityQueue<SavedPath> allSaved = null;
		if(enableLoadPath)
			allSaved = known.loadCompatiblePath(sp);
			
		if(allSaved != null && allSaved.isEmpty())
			log.write("Aucun chemin connu pour : "+sp, Subject.TRAJECTORY);
		
		if(modeDegrade)
			kraken.initializeNewSearch(sp);
		
		List<ItineraryPoint> path = null;

		if(allSaved != null)
			while(!allSaved.isEmpty())
			{
				SavedPath saved = allSaved.poll();
				try
				{
					if(modeDegrade)
					{
						if(kraken.checkPath(saved.path))
						{
							log.write("On réutilise un chemin en mode dégradé : "+saved.name, Subject.TRAJECTORY);
							path = saved.path;
							break;
						}
						else
							log.write("Chemin inadapté", Subject.TRAJECTORY);
					}
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
				}
			}

		if(modeDegrade)
		{

//			System.out.println(path);
			// On cherche et on envoie
			if(path == null)
			{
				log.write("On cherche un chemin en mode dégradé", Subject.TRAJECTORY);
				path = kraken.search();
			}
			else
				log.write("On réutilise un chemin déjà connu !", Subject.TRAJECTORY);
			if(!simuleLL)
			{
				out.destroyPointsTrajectoires(0);
				out.ajoutePointsTrajectoire(path, true);
			}
			setReady();
			pathDegrade = path;
		}
		else
		{
			if(path == null)
			{
				log.write("On cherche un chemin en mode continu", Subject.TRAJECTORY);
				kraken.startContinuousSearch(sp);
			}
//			else
//				log.write("On réutilise un chemin déjà connu !", Subject.TRAJECTORY);
		}

		DataTicket out = followTrajectory();
		if(!modeDegrade)
			kraken.endContinuousSearch();
		return out;
	}
	
	private DataTicket followTrajectory() throws InterruptedException
	{
		assert modeDegrade == (pathDegrade != null) : modeDegrade+" "+pathDegrade;
		assert etat == State.READY_TO_GO || etat == State.STANDBY;

		log.write("Attente de la trajectoire…", Subject.TRAJECTORY);

		synchronized(this)
		{
			while(etat == State.STANDBY)
				wait();
		}

		log.write("On commence à suivre la trajectoire", Subject.TRAJECTORY);
		
		assert etat == State.READY_TO_GO;
		etat = State.MOVING;
		
		DataTicket dt;
		
		if(!simuleLL)
		{
			Ticket t = out.followTrajectory();
		
			dt = t.attendStatus();
			assert etat != State.MOVING : etat;
//			while(etat == State.MOVING)
//				wait();
		}
		else
		{
			dt = new DataTicket(modeDegrade ? pathDegrade : dpath.getPath(), CommProtocol.State.OK);
		}
		
		if(dt.data == null)
			log.write("Le robot a fini correctement la trajectoire.", Subject.TRAJECTORY);
		else
			log.write("Le robot s'est arrêté suite à un problème : "+dt.data, Severity.CRITICAL, Subject.TRAJECTORY);

		pathDegrade = null;
		etat = State.STANDBY;
		return dt;
	}

	public synchronized boolean isStandby()
	{
		return etat == State.STANDBY;
	}

	public synchronized void setDegrade()
	{
		log.write("Le robot entre en mode dégradé !", Severity.CRITICAL, Subject.STATUS);
		kraken.endAutoReplanning();
		modeDegrade = true;
		notifyAll();
	}
	
	public boolean isDegrade() {
		return modeDegrade;
	}

	public boolean needCollisionCheck()
	{
		return etat == State.MOVING;
	}

	public List<ItineraryPoint> getPath()
	{
		assert etat == State.READY_TO_GO || etat == State.MOVING;
		return pathDegrade;
	}
}
