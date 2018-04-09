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
import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.graphic.printable.Segment;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.RobotState;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.Subject;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.CommProtocol;
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
	/*
	 * DÉPLACEMENT HAUT NIVEAU
	 */

	protected volatile boolean symetrie;
	protected Log log;
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

	private boolean simuleSerie;
	private boolean printTrace;
	private OutgoingOrderBuffer out;
	private GraphicDisplay buffer;
	private RobotPrintable printable = null;
	private volatile boolean cinematiqueInitialised = false;

	// Constructeur
	public Robot(Log log, OutgoingOrderBuffer out, Config config, GraphicDisplay buffer)
	{
		this.log = log;
		this.out = out;
		this.buffer = buffer;
		
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

	/**
	 * Méthode bloquante qui suit une trajectoire précédemment envoyée
	 * 
	 * @throws InterruptedException
	 * @throws UnableToMoveException
	 */
/*	public void followTrajectory(Speed vitesse) throws InterruptedException, UnableToMoveException
	{
		if(simuleSerie)
		{
//			setCinematique(chemin.getLastCinematique());
			return;
		}
		*/
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
//	}

	public void avance(double distance, Speed speed)
			throws UnableToMoveException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	public void execute(CommProtocol ordre, Object... param) throws InterruptedException, ActionneurException
	{
		bloque(ordre.toString(), param);
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

}
