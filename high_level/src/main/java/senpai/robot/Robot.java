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
import pfg.kraken.astar.thread.DynamicPath;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.robot.RobotState;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.KnownPathManager;
import senpai.SavedPath;
import senpai.Subject;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.CommProtocol;
import senpai.comm.DataTicket;
import senpai.comm.Ticket;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;

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
	
	/*
	 * DÉPLACEMENT HAUT NIVEAU
	 */

	protected volatile boolean symetrie;
	protected Log log;
	protected Kraken kraken;
	private DynamicPath dpath;
	
/*	public Robot(Log log)
	{
		this.log = log;
		cinematique = new Cinematique();
	}*/
/*
	public int codeForPFCache()
	{
		return cinematique.codeForPFCache();
	}*/

	@Override
	public String toString()
	{
		return cinematique.toString();
	}

	private volatile State etat = State.STANDBY;
	private boolean simuleSerie;
	private boolean printTrace;
	private OutgoingOrderBuffer out;
	private GraphicDisplay buffer;
	private KnownPathManager known;
	private RobotPrintable printable = null;
	private volatile boolean cinematiqueInitialised = false;

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
		
		printTrace = config.getBoolean(ConfigInfoSenpai.GRAPHIC_TRACE_ROBOT);
		cinematique = new Cinematique(new XYO(
				config.getDouble(ConfigInfoSenpai.INITIAL_X),
				config.getDouble(ConfigInfoSenpai.INITIAL_Y),
				config.getDouble(ConfigInfoSenpai.INITIAL_O)));
		simuleSerie = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);
	}
	
	public void setEnMarcheAvance(boolean enMarcheAvant)
	{
		cinematique.enMarcheAvant = enMarcheAvant;
	}

/*	public long getTempsDepuisDebutMatch()
	{
		if(!matchDemarre)
			return 0;
		return System.currentTimeMillis() - dateDebutMatch;
	}*/

	public boolean isCinematiqueInitialised()
	{
		return cinematiqueInitialised;
	}

	private XY_RW oldPosition = new XY_RW();
	
	public synchronized void setCinematique(Cinematique cinematique)
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
		synchronized(buffer)
		{
			// affichage
			if(printTrace && oldPosition.distanceFast(cinematique.getPosition()) < 100)
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
				if(s != "")
					s += ", ";
				s += o;
			}
			log.write("Appel à " + nom + " (param = " + s + ")", Subject.SCRIPT);
		}

		if(simuleSerie)
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
	
	// TODO remove
	private void exempleAct()
	{
	}

	public void initActionneurs()
	{
		// TODO Auto-generated method stub		
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
	
	public synchronized DataTicket goTo(XYO destination) throws PathfindingException, InterruptedException
	{
		return goTo(new SearchParameters(cinematique.getXYO(), destination));
	}
	
	public synchronized DataTicket goTo(SearchParameters sp) throws PathfindingException, InterruptedException
	{
		PriorityQueue<SavedPath> allSaved = known.loadCompatiblePath(sp);
		boolean initialized = false;
		if(allSaved.isEmpty())
			log.write("Aucun chemin connu pour : "+sp, Subject.TRAJECTORY);
		
		while(!allSaved.isEmpty())
		{
			SavedPath saved = allSaved.poll();
			try
			{
				kraken.startContinuousSearchWithInitialPath(sp, saved.path);
				initialized = true;
				break;
			}
			catch(PathfindingException e)
			{
				log.write("Chemin inadapté : "+e.getMessage(), Subject.TRAJECTORY);
			}
		}
		if(!initialized)
			kraken.startContinuousSearch(sp);
		else
			log.write("On réutilise un chemin déjà connu !", Subject.TRAJECTORY);
		DataTicket out = followTrajectory();
		kraken.endContinuousSearch();
		return out;
	}
	
	private synchronized DataTicket followTrajectory() throws InterruptedException
	{
		assert etat == State.READY_TO_GO || etat == State.STANDBY;

		log.write("Attente de la trajectoire…", Subject.TRAJECTORY);

		while(etat == State.STANDBY)
			wait();

		log.write("On commence à suivre la trajectoire", Subject.TRAJECTORY);
		
		assert etat == State.READY_TO_GO;
		etat = State.MOVING;
		
		DataTicket dt;
		
		if(!simuleSerie)
		{
			Ticket t = out.followTrajectory();
		
			dt = t.attendStatus();
			assert etat != State.MOVING : etat;
//			while(etat == State.MOVING)
//				wait();
		}
		else
		{
			dt = new DataTicket(dpath.getPath(), CommProtocol.State.OK);
		}
		
		log.write("Le robot ne bouge plus : "+etat, Subject.TRAJECTORY);
		
		etat = State.STANDBY;
		return dt;
	}

	public synchronized boolean isStandby()
	{
		return etat == State.STANDBY;
	}
}
