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
import buffer.OutgoingOrderBuffer;
import capteurs.SensorMode;
import comm.CommProtocol;
import comm.Ticket;
import exceptions.ActionneurException;
import exceptions.UnableToMoveException;
import pfg.config.Config;
import pfg.graphic.GraphicPanel;
import pfg.graphic.printable.Printable;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.RobotState;
import pfg.kraken.utils.XY;
import pfg.graphic.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.Subject;

/**
 * Classe abstraite du robot, dont héritent RobotVrai et RobotChrono
 * 
 * @author pf
 */

public class Robot extends RobotState implements Printable
{
	/*
	 * DÉPLACEMENT HAUT NIVEAU
	 */

	private static final long serialVersionUID = 1L;

	protected volatile boolean symetrie;
	protected Log log;
	protected volatile boolean filetBaisse = false;
	protected volatile boolean filetPlein = false;

	public Robot(Log log)
	{
		this.log = log;
		cinematique = new Cinematique();
	}
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

	protected volatile boolean matchDemarre = false;
	protected volatile long dateDebutMatch;
	private boolean simuleSerie;
	private boolean print, printTrace;
	private OutgoingOrderBuffer out;
	private volatile boolean cinematiqueInitialised = false;
	private SensorMode lastMode = null;

	// Constructeur
	public Robot(Log log, OutgoingOrderBuffer out, Config config)
	{
		this.log = log;
		this.out = out;
		
		// c'est le LL qui fournira la position
		cinematique = new Cinematique(0, 300, 0, true, 3);
		print = config.getBoolean(ConfigInfoSenpai.GRAPHIC_ROBOT_AND_SENSORS);
		printTrace = config.getBoolean(ConfigInfoSenpai.GRAPHIC_TRACE_ROBOT);

		simuleSerie = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);
	}
	
	public void setEnMarcheAvance(boolean enMarcheAvant)
	{
		cinematique.enMarcheAvant = enMarcheAvant;
	}

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

	public synchronized void setCinematique(Cinematique cinematique)
	{
		XY old = this.cinematique.getPosition().clone();
		cinematique.copy(this.cinematique);
		/*
		 * On vient juste de récupérer la position initiale
		 */
		if(!cinematiqueInitialised)
		{
			cinematiqueInitialised = true;
			notifyAll();
		}
/*		synchronized(buffer)
		{
			// affichage
			if(printTrace && old.distanceFast(cinematique.getPosition()) < 100)
				buffer.addSupprimable(new Segment(old, cinematique.getPosition().clone(), Layer.FOREGROUND, Couleur.ROUGE.couleur));
			else if(print)
				buffer.notify();
		}*/
	}

	@Override
	public void print(Graphics g, GraphicPanel f)
	{
/*		if(print)
		{
			ObstacleRobot o = new ObstacleRobot(demieLargeurNonDeploye, demieLongueurArriere, demieLongueurAvant, marge);
			o.update(cinematique.getPosition(), cinematique.orientationReelle);
			o.print(g, f, robot);
		}*/
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
			log.write("Appel à " + nom, Subject.DUMMY);
		else
		{
			String s = "";
			for(Object o : param)
			{
				if(s != "")
					s += ", ";
				s += o;
			}
			log.write("Appel à " + nom + " (param = " + s + ")", Subject.DUMMY);
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
			e.printStackTrace(log.getPrintWriter());
		}
		etat = t.attendStatus().etat;
		if(etat == CommProtocol.State.KO)
			throw new ActionneurException("Problème pour l'actionneur " + nom);

		log.write("Temps d'exécution de " + nom + " : " + (System.currentTimeMillis() - avant), Subject.DUMMY);
	}

	/**
	 * Méthode bloquante qui suit une trajectoire précédemment envoyée
	 * 
	 * @throws InterruptedException
	 * @throws UnableToMoveException
	 */
	public void followTrajectory(Speed vitesse) throws InterruptedException, UnableToMoveException
	{
		if(simuleSerie)
		{
//			setCinematique(chemin.getLastCinematique());
			return;
		}
		
/*		boolean oneMoreTime = true;
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
		*/
	}

	public void setSensorMode(SensorMode mode)
	{
		if(lastMode != mode)
		{
			out.setSensorMode(mode);
			lastMode = mode;
		}
	}

	public void avance(double distance, Speed speed)
			throws UnableToMoveException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	public void execute(CommProtocol ordre, Object... param) throws InterruptedException, ActionneurException
	{
		bloque(ordre.toString(), param);
	}

}
