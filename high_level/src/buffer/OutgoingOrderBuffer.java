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

package buffer;

import java.awt.Graphics;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import capteurs.SensorMode;
import comm.Order;
import comm.Ticket;
import comm.CommProtocol.Channel;
import comm.CommProtocol.Id;
import pfg.log.Log;
import robot.Speed;
import senpai.Subject;
import senpai.ConfigInfoSenpai;
import senpai.Severity;
import pfg.config.Config;
import pfg.graphic.Chart;
import pfg.graphic.Fenetre;
import pfg.graphic.PrintBuffer;
import pfg.graphic.printable.Printable;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.CinematiqueObs;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XY_RW;

/**
 * Classe qui contient les ordres à envoyer à la série
 * Il y a trois priorité
 * - la plus haute, l'arrêt
 * - ensuite, la trajectoire courbe
 * - enfin, tout le reste
 * 
 * @author pf
 *
 */

public class OutgoingOrderBuffer implements Printable
{
	private static final long serialVersionUID = 1L;
	protected Log log;

	public OutgoingOrderBuffer(Log log, Config config, PrintBuffer print)
	{
		this.log = log;
		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_COMM_CHART))
			print.add(this);
	}

	private Queue<Order> bufferBassePriorite = new ConcurrentLinkedQueue<Order>();
	private Queue<Order> bufferTrajectoireCourbe = new ConcurrentLinkedQueue<Order>();
	private volatile boolean sendStop = false;
	private volatile Ticket stop = null;

	/**
	 * Le buffer est-il vide?
	 * 
	 * @return
	 */
	public synchronized boolean isEmpty()
	{
		return bufferBassePriorite.isEmpty() && bufferTrajectoireCourbe.isEmpty() && !sendStop;
	}

	/**
	 * Retire un élément du buffer
	 * 
	 * @return
	 */
	public synchronized Order poll()
	{
		if(bufferTrajectoireCourbe.size() + bufferBassePriorite.size() > 10)
			log.write("On n'arrive pas à envoyer les ordres assez vites (ordres TC en attente : " + bufferTrajectoireCourbe.size() + ", autres en attente : " + bufferBassePriorite.size() + ")", Severity.WARNING, Subject.COMM);

		if(sendStop)
		{
			bufferTrajectoireCourbe.clear(); // on annule tout mouvement
			Order out = new Order(Id.STOP);
			sendStop = false;
			return out;
		}
		else if(!bufferTrajectoireCourbe.isEmpty())
			return bufferTrajectoireCourbe.poll();
		else
			return bufferBassePriorite.poll();
	}

	/**
	 * Signale la vitesse max au bas niveau
	 * 
	 * @param vitesse signée
	 * @return
	 */
	public synchronized void setMaxSpeed(Speed vitesseInitiale, boolean marcheAvant)
	{
		log.write("Envoi d'un ordre de vitesse max : " + vitesseInitiale+". Marche avant : "+marcheAvant, Subject.COMM);

		short vitesseTr; // vitesse signée
		if(marcheAvant)
			vitesseTr = (short) (vitesseInitiale.translationalSpeed * 1000);
		else
			vitesseTr = (short) (-vitesseInitiale.translationalSpeed * 1000);
		setMaxSpeed(vitesseTr);
	}
	
	private synchronized void addBassePriorite(Order o)
	{
		o.ordre.orderSent();
		bufferBassePriorite.add(o);
		notify();
	}

	private synchronized void addHautePriorite(Order o)
	{
		o.ordre.orderSent();
		bufferTrajectoireCourbe.add(o);
		notify();
	}
	
	public synchronized Ticket run()
	{
		addBassePriorite(new Order(Id.RUN));
		return Id.RUN.ticket;
	}

	public synchronized void setCurvature(double courbure)
	{
		ByteBuffer data = ByteBuffer.allocate(2);
		short courbureShort = (short) (Math.round(courbure * 100));
		data.putShort(courbureShort);

		addBassePriorite(new Order(data, Id.SET_CURVATURE));
	}

	public synchronized void setMaxSpeed(short vitesseTr)
	{
		ByteBuffer data = ByteBuffer.allocate(2);
		data.putShort(vitesseTr);

		addBassePriorite(new Order(data, Id.SET_MAX_SPEED));
	}

	/**
	 * Ordre long de suivi de trajectoire
	 * 
	 * @param vitesseInitiale
	 * @param marcheAvant
	 * @return
	 */
	public synchronized Ticket followTrajectory(Speed vitesseInitiale, boolean marcheAvant)
	{
		short vitesseTr; // vitesse signée
		if(marcheAvant)
			vitesseTr = (short) (vitesseInitiale.translationalSpeed * 1000);
		else
			vitesseTr = (short) (-vitesseInitiale.translationalSpeed * 1000);
		log.write("On commence à suivre la trajectoire. Vitesse : "+vitesseTr, Subject.COMM);

		ByteBuffer data = ByteBuffer.allocate(2);
		data.putShort(vitesseTr);

		addBassePriorite(new Order(data, Id.FOLLOW_TRAJECTORY));
		return Id.FOLLOW_TRAJECTORY.ticket;
	}

	/**
	 * Ajout d'une demande d'ordre de s'arrêter
	 */
	public synchronized void immobilise()
	{
		log.write("Stop !", Severity.WARNING, Subject.COMM);
		sendStop = true;
		stop = new Ticket();
		notify();
	}

	XY_RW tmp = new XY_RW();

	/**
	 * Ajoute une position et un angle.
	 * Occupe 5 octets.
	 * 
	 * @param data
	 * @param pos
	 * @param angle
	 */
	private void addXYO(ByteBuffer data, XY pos, double angle, boolean checkInTable)
	{
		double x = pos.getX();
		double y = pos.getY();
		
		if(checkInTable)
		{
			x = (x < -1500 ? -1500 : x > 1500 ? 1500 : x);
			y = (y < 0 ? 0 : y > 2000 ? 2000 : y);
		}
		
		data.put((byte) (((int) (x) + 1500) >> 4));
		data.put((byte) ((((int) (x) + 1500) << 4) + (((int) (y) + 1000) >> 8)));
		data.put((byte) ((int) (y) + 1000));
		short theta = (short) Math.round((angle % (2 * Math.PI)) * 1000);
		if(theta < 0)
			theta += (short) Math.round(2000 * Math.PI);
		data.putShort(theta);
	}

	/**
	 * Corrige la position du bas niveau
	 */
	public synchronized void setPosition(XY pos, double orientation)
	{
		ByteBuffer data = ByteBuffer.allocate(5);
		addXYO(data, pos, orientation, true);
		addBassePriorite(new Order(data, Id.SET_POSITION));
	}

	/**
	 * Corrige la position du bas niveau
	 */
	public synchronized void correctPosition(XY deltaPos, double deltaOrientation)
	{
		ByteBuffer data = ByteBuffer.allocate(5);
		addXYO(data, deltaPos, deltaOrientation, false);
		addBassePriorite(new Order(data, Id.EDIT_POSITION));
	}

	/**
	 * Demande à être notifié du début du match
	 */
	public synchronized Ticket waitForJumper()
	{
		addBassePriorite(new Order(Id.WAIT_FOR_JUMPER));
		return Id.WAIT_FOR_JUMPER.ticket;
	}

	/**
	 * Demande à être notifié de la fin du match
	 */
	public synchronized Ticket startMatchChrono()
	{
		addBassePriorite(new Order(Id.START_MATCH_CHRONO));
		return Id.START_MATCH_CHRONO.ticket;
	}

	/**
	 * Demande la couleur au bas niveau
	 */
	public synchronized Ticket demandeCouleur()
	{
		addBassePriorite(new Order(Id.ASK_COLOR));
		return Id.ASK_COLOR.ticket;
	}

	/**
	 * Demande la couleur au bas niveau
	 */
	public synchronized Ticket ping()
	{
		addBassePriorite(new Order(Id.PING));
		return Id.PING.ticket;
	}
	
	/**
	 * Demande d'utiliser un certain SensorMode
	 * 
	 * @param mode
	 */
	public synchronized void setSensorMode(SensorMode mode)
	{
		ByteBuffer data = ByteBuffer.allocate(1);
		data.put(mode.code);
		addBassePriorite(new Order(data, Id.SET_SENSOR_MODE));
	}

	/**
	 * Démarre un stream
	 */
	public synchronized void startStream(Id stream)
	{
		changeStream(stream, Channel.INSCRIPTION);
	}
	
	/**
	 * Arrête un stream
	 */
	public synchronized void stopStream(Id stream)
	{
		changeStream(stream, Channel.DESINSCRIPTION);
	}
	
	private synchronized void changeStream(Id stream, Channel ch)
	{
		// on vérifie qu'on ne cherche pas à s'abonner alors qu'on est déjà abonné (idem avec désabonné)
		stream.changeStreamState(ch);
		ByteBuffer data = ByteBuffer.allocate(1);
		data.put(ch.code);
		addBassePriorite(new Order(data, stream));
	}
	
	/**
	 * Envoi un seul arc sans stop. Permet d'avoir une erreur NO_MORE_POINTS
	 * avec le point d'après
	 * 
	 * @param point
	 * @param indexTrajectory
	 * @return
	 */
	public synchronized void makeNextObsolete(Cinematique c, int indexTrajectory)
	{
		log.write("Envoi d'un arc d'obsolescence", Subject.COMM);

		ByteBuffer data = ByteBuffer.allocate(8);
		data.put((byte) indexTrajectory);
		addXYO(data, c.getPosition(), c.orientationReelle, true);
		short courbure = (short) ((Math.round(Math.abs(c.courbureReelle) * 100)) & 0x7FFF);

		// stop
		courbure |= 0x8000;
		if(c.courbureReelle < 0) // bit de signe
			courbure |= 0x4000;

		data.putShort(courbure);

		addHautePriorite(new Order(data, Id.SEND_ARC));
	}

	/**
	 * Envoi de tous les arcs élémentaires d'un arc courbe
	 * @0 arc
	 * @throws InterruptedException 
	 */
	public synchronized void envoieArcCourbe(List<CinematiqueObs> points, int indexTrajectory) throws InterruptedException
	{
		log.write("Envoi de " + points.size() + " points à partir de l'index " + indexTrajectory, Subject.COMM);

		int index = indexTrajectory;
		int nbEnvoi = (points.size() >> 5) + 1;
		int modulo = (points.size() & 31); // pour le dernier envoi

		for(int i = 0; i < nbEnvoi; i++)
		{
			int nbArc = 32;
			if(i == nbEnvoi - 1) // dernier envoi
				nbArc = modulo;
			ByteBuffer data = ByteBuffer.allocate(1 + 7 * nbArc);
			data.put((byte) index);
			for(int j = 0; j < nbArc; j++)
			{
				int k = (i << 5) + j;
				double prochaineCourbure; // pour gérer les arrêts qui font
											// arrêter le robot
				CinematiqueObs c = points.get(k);

				if(k + 1 < points.size())
					prochaineCourbure = points.get(k + 1).courbureReelle;
				else
					prochaineCourbure = c.courbureReelle; // c'est le dernier
															// point, de toute
															// façon ce sera un
															// STOP_POINT

				log.write("Point " + k + " : " + c, Subject.COMM);
				addXYO(data, c.getPosition(), c.orientationReelle, true);
				short courbure = (short) ((Math.round(Math.abs(c.courbureReelle) * 100)) & 0x7FFF);

				// on vérifie si on va dans le même sens que le prochain point
				// le dernier point est forcément un point d'arrêt
				// de plus, si le changement de courbure est trop grand, on
				// impose un arrêt
				if(k + 1 == points.size() || c.enMarcheAvant != points.get(k + 1).enMarcheAvant || Math.abs(c.courbureReelle - prochaineCourbure) > 0.5)
					courbure |= 0x8000; // en cas de rebroussement

				if(c.courbureReelle < 0) // bit de signe
					courbure |= 0x4000;

				data.putShort(courbure);
			}
			addHautePriorite(new Order(data, Id.SEND_ARC));
			Id.SEND_ARC.ticket.attendStatus();
			index += nbArc;
		}
	}

	public void waitStop() throws InterruptedException
	{
		log.write("Attente de la réception de la réponse au stop", Subject.COMM);
		if(stop != null)
		{
			stop.attendStatus(1500);
			if(stop.isEmpty())
				log.write("Timeout d'attente du stop dépassé !", Severity.WARNING, Subject.COMM);
			stop = null;
		}
	}

	/**
	 * Renvoie une estimation de la latence de la comm, en μs
	 * @throws InterruptedException 
	 */
	public void checkLatence() throws InterruptedException
	{
		int nbEssais = 10000;
		long avant = System.currentTimeMillis();
		for(int i = 0; i < nbEssais; i++)
			ping().attendStatus();
		double latency = 1000. * (System.currentTimeMillis() - avant) / (2*nbEssais);
		// on divise par 2 car il s'agit d'un aller-retour
		log.write("Latence estimée : "+latency+" μs", latency >= 200 ? Severity.CRITICAL : (latency >= 50 ? Severity.WARNING : Severity.INFO), Subject.COMM);
	}

	@Override
	public void print(Graphics g, Fenetre f, Chart a)
	{
		a.addData("Buffer d'ordre sortants", (double) (bufferBassePriorite.size() + bufferTrajectoireCourbe.size()));
	}

	@Override
	public int getLayer()
	{
		return 0;
	}
	
}
