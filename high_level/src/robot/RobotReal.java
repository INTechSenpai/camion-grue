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

package robot;

import java.awt.Graphics;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import capteurs.SensorMode;
import obstacles.types.ObstacleRobot;
import pathfinding.astar.arcs.ArcCourbeDynamique;
import pathfinding.astar.arcs.ArcManager;
import pathfinding.astar.arcs.BezierComputer;
import pathfinding.chemin.CheminPathfinding;
import serie.BufferOutgoingOrder;
import serie.SerialProtocol;
import serie.SerialProtocol.InOrder;
import serie.SerialProtocol.State;
import serie.Ticket;
import config.Config;
import config.ConfigInfo;
import container.Service;
import container.dependances.CoreClass;
import exceptions.ActionneurException;
import exceptions.MemoryManagerException;
import exceptions.PathfindingException;
import exceptions.UnableToMoveException;
import utils.Log;
import utils.Vec2RO;
import utils.Vec2RW;
import utils.Log.Verbose;
import graphic.Fenetre;
import graphic.PrintBufferInterface;
import graphic.printable.Couleur;
import graphic.printable.Layer;
import graphic.printable.Printable;
import graphic.printable.Segment;
import graphic.printable.Vector;

/**
 * Effectue le lien entre le code et la réalité (permet de parler à la carte bas
 * niveau, d'interroger les capteurs, etc.)
 * 
 * @author pf
 *
 */

public class RobotReal extends Robot implements Service, Printable, CoreClass
{
	protected volatile boolean matchDemarre = false;
	protected volatile long dateDebutMatch;
	private boolean simuleSerie;
	private int demieLargeurNonDeploye, demieLongueurArriere, demieLongueurAvant, marge;
	private boolean print, printTrace;
	private PrintBufferInterface buffer;
	private BufferOutgoingOrder out;
	private ArcManager arcmanager;
	private CheminPathfinding chemin;
	private volatile boolean cinematiqueInitialised = false;
	private SensorMode lastMode = null;
	private BezierComputer bezier;
	private AnglesRoues angles = new AnglesRoues();
	private Vector vecteur = new Vector(new Vec2RW(), 0, Couleur.ToF_COURT);

	// Constructeur
	public RobotReal(Log log, BezierComputer bezier, ArcManager arcmanager, BufferOutgoingOrder out, PrintBufferInterface buffer, CheminPathfinding chemin, Config config)
	{
		super(log);
		this.arcmanager = arcmanager;
		this.buffer = buffer;
		this.out = out;
		this.chemin = chemin;
		this.bezier = bezier;
		
		// c'est le LL qui fournira la position
		cinematique = new Cinematique(0, 300, 0, true, 3);
		print = config.getBoolean(ConfigInfo.GRAPHIC_ROBOT_AND_SENSORS);
		demieLargeurNonDeploye = config.getInt(ConfigInfo.LARGEUR_NON_DEPLOYE) / 2;
		demieLongueurArriere = config.getInt(ConfigInfo.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		demieLongueurAvant = config.getInt(ConfigInfo.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);
		marge = config.getInt(ConfigInfo.DILATATION_OBSTACLE_ROBOT);
		printTrace = config.getBoolean(ConfigInfo.GRAPHIC_TRACE_ROBOT);

		simuleSerie = config.getBoolean(ConfigInfo.SIMULE_SERIE);

		if(print || printTrace)
			buffer.add(this);
	}

	public double getAngleRoueGauche()
	{
		return angles.angleRoueGauche;
	}

	public double getAngleRoueDroite()
	{
		return angles.angleRoueDroite;
	}

	public void setAngleRoues(double angleRoueGauche, double angleRoueDroite)
	{
		angles.angleRoueDroite = angleRoueDroite;
		angles.angleRoueGauche = angleRoueGauche;
	}

	/*
	 * MÉTHODES PUBLIQUES
	 */

	@Override
	public synchronized void updateConfig(Config config)
	{
		super.updateConfig(config);
		Long date = config.getLong(ConfigInfo.DATE_DEBUT_MATCH);
		if(date != null)
			dateDebutMatch = date;
		Boolean m = config.getBoolean(ConfigInfo.MATCH_DEMARRE);
		if(m != null)
			matchDemarre = m;
	}

	public void setEnMarcheAvance(boolean enMarcheAvant)
	{
		cinematique.enMarcheAvant = enMarcheAvant;
	}

	@Override
	public long getTempsDepuisDebutMatch()
	{
		if(!matchDemarre)
			return 0;
		return System.currentTimeMillis() - dateDebutMatch;
	}

	public boolean isCinematiqueInitialised()
	{
		return cinematiqueInitialised;
	}

	@Override
	public synchronized void setCinematique(Cinematique cinematique)
	{
		Vec2RO old = this.cinematique.getPosition().clone();
		super.setCinematique(cinematique);
		/*
		 * On vient juste de récupérer la position initiale
		 */
		if(!cinematiqueInitialised)
		{
			cinematiqueInitialised = true;
			notifyAll();
		}
		synchronized(buffer)
		{
			// affichage
			if(printTrace && old.distanceFast(cinematique.getPosition()) < 100)
				buffer.addSupprimable(new Segment(old, cinematique.getPosition().clone(), Layer.FOREGROUND, Couleur.ROUGE.couleur));
			else if(print)
				buffer.notify();
		}
	}

	@Override
	public void print(Graphics g, Fenetre f, RobotReal robot)
	{
		if(print)
		{
			ObstacleRobot o = new ObstacleRobot(demieLargeurNonDeploye, demieLongueurArriere, demieLongueurAvant, marge);
			o.update(cinematique.getPosition(), cinematique.orientationReelle);
			o.print(g, f, robot);
		}
		if(printTrace)
		{
			vecteur.update(cinematique.getPosition(), cinematique.orientationReelle);
			vecteur.print(g, f, robot);
		}
	}

	@Override
	public Layer getLayer()
	{
		return Layer.FOREGROUND;
	}

	public int getDemieLargeurGauche()
	{
		return demieLargeurNonDeploye;
	}

	public int getDemieLargeurDroite()
	{
		return demieLargeurNonDeploye;
	}

	public int getDemieLongueurAvant()
	{
		return demieLongueurAvant;
	}

	public int getDemieLongueurArriere()
	{
		return demieLongueurArriere;
	}

	/*
	 * DÉPLACEMENTS
	 */

	@Override
	public void avanceToCircle(Speed speed) throws InterruptedException, UnableToMoveException, MemoryManagerException
	{
		ArcCourbeDynamique arc = bezier.trajectoireCirculaireVersCentre(cinematique);
		if(arc == null)
			throw new UnableToMoveException("Le robot est arrivé au mauvais endroit et aucune correction n'est possible !");
		LinkedList<CinematiqueObs> out = new LinkedList<CinematiqueObs>();
		for(CinematiqueObs o : arc.arcs)
			out.add(o);
		try
		{
			chemin.addToEnd(out);			
			if(!simuleSerie)
				chemin.waitTrajectoryTickets();
		}
		catch(PathfindingException e)
		{
			// Ceci ne devrait pas arriver, ou alors en demandant d'avancer de
			// 5m
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
		}
		followTrajectory(speed);
	}
	
	@Override
	public void avance(double distance, Speed speed) throws UnableToMoveException, InterruptedException, MemoryManagerException
	{
		try
		{
			chemin.addToEnd(bezier.avance(distance, cinematique));
			if(!simuleSerie)
				chemin.waitTrajectoryTickets();
		}
		catch(PathfindingException e)
		{
			// Ceci ne devrait pas arriver, ou alors en demandant d'avancer de
			// 5m
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
		}
		followTrajectory(speed);
	}

	public boolean isSerieSimule()
	{
		return simuleSerie;
	}
	
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
	@Override
	protected void bloque(String nom, Object... param) throws InterruptedException, ActionneurException
	{
		if(param == null || param.length == 0)
			log.debug("Appel à " + nom, Verbose.SCRIPTS.masque);
		else
		{
			String s = "";
			for(Object o : param)
			{
				if(s != "")
					s += ", ";
				s += o;
			}
			log.debug("Appel à " + nom + " (param = " + s + ")", Verbose.SCRIPTS.masque);
		}

		if(simuleSerie)
			return;

		SerialProtocol.State etat;
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
			t = (Ticket) BufferOutgoingOrder.class.getMethod(nom, paramClasses).invoke(out, param.length == 0 ? null : param);
		}
		catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
		{
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
		}
		etat = t.attendStatus().etat;
		if(etat == SerialProtocol.State.KO)
			throw new ActionneurException("Problème pour l'actionneur " + nom);

		log.debug("Temps d'exécution de " + nom + " : " + (System.currentTimeMillis() - avant), Verbose.SCRIPTS.masque);
	}

	/**
	 * Initialise les actionneurs pour le début du match
	 * 
	 * @throws InterruptedException
	 */
	public void initActionneurs() throws InterruptedException
	{
		try
		{
			leveFilet();
			verrouilleFilet();
			rearme();
			rearmeAutreCote();
		}
		catch(ActionneurException e)
		{
			log.critical(e);
		}
	}

	public Ticket traverseBascule() throws InterruptedException, ActionneurException
	{
		return out.traverseBascule();
	}

	/**
	 * Méthode bloquante qui suit une trajectoire précédemment envoyée
	 * 
	 * @throws InterruptedException
	 * @throws UnableToMoveException
	 */
	@Override
	public void followTrajectory(Speed vitesse) throws InterruptedException, UnableToMoveException
	{
		if(simuleSerie)
		{
			setCinematique(chemin.getLastCinematique());
			return;
		}
		
		boolean oneMoreTime = true;
		if(chemin.isEmpty())
			log.warning("Trajectoire vide !");
		else
			while(!chemin.isArrived())
			{
				boolean marcheAvant = chemin.getCurrentMarcheAvant();
				if(marcheAvant)
					setSensorMode(SensorMode.FRONT_AND_SIDES);
				else
					setSensorMode(SensorMode.BACK_AND_SIDES);
				try
				{
					// on attend toujours que la trajectoire soit bien envoyée
					// avant de lancer un FollowTrajectory
					chemin.waitTrajectoryTickets();
					Ticket t = out.followTrajectory(vitesse, marcheAvant);
					InOrder i = t.attendStatus();
					if(i.etat == State.KO)
					{
						chemin.clear();

						// on attend la fin du stop
						if(i == InOrder.STOP_REQUIRED)
							out.waitStop();
						else
							log.critical("Erreur : " + i);

						if(i == InOrder.ROBOT_BLOCAGE_EXTERIEUR && oneMoreTime)
						{
							oneMoreTime = false;
							log.debug("One more time !");
							continue;
						}

						throw new UnableToMoveException(i.name());
					}
					log.debug("Le trajet s'est bien terminé (" + i + ")");
				}
				finally
				{
					setSensorMode(SensorMode.ALL);
				}
			}
		chemin.clear(); // dans tous les cas, il faut nettoyer le chemin
	}

	public void setSensorMode(SensorMode mode)
	{
		if(lastMode != mode)
		{
			out.setSensorMode(mode);
			lastMode = mode;
		}
	}

	public AnglesRoues getAngles()
	{
		return angles;
	}
	
	public Vector getVector()
	{
		return vecteur.clone();
	}

	public void setVector(Vector vecteur)
	{
		this.vecteur = vecteur;
	}
	
	@Override
	public boolean isArrivedAsser()
	{
		return arcmanager.isArrivedAsser(cinematique);
	}

	public void fermeFiletNonBloquant()
	{
		out.fermeFilet();
	}

}
