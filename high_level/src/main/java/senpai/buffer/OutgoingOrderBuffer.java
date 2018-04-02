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

package senpai.buffer;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import pfg.log.Log;
import senpai.Subject;
import senpai.comm.Order;
import senpai.comm.Ticket;
import senpai.comm.CommProtocol.Channel;
import senpai.comm.CommProtocol.Id;
import senpai.ConfigInfoSenpai;
import senpai.Severity;
import pfg.config.Config;
import pfg.graphic.Chart;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Plottable;
import pfg.kraken.robot.ItineraryPoint;
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

public class OutgoingOrderBuffer implements Plottable
{
	private static final long serialVersionUID = 1L;
	protected Log log;

	public OutgoingOrderBuffer(Log log, Config config, GraphicDisplay print)
	{
		this.log = log;
		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_COMM_CHART))
			print.addPlottable(this);
	}
	
	private BlockingQueue<Order> buffer = new PriorityBlockingQueue<Order>(100, comparing(Order::getPriority, naturalOrder()));

	public Order take() throws InterruptedException
	{
		boolean restart;
		Order o = null;
		do {
			if(o == null)
				o = buffer.take();
			
			// si on attend encore la réponse, on ignore cet ordre
			restart = !o.ordre.isSendPossible();
			if(restart)
			{
				// on récupère le prochain TOUT DE SUITE. Pas de prochain ? Tant pis.
				Order nextO = buffer.poll();
				addToBuffer(o); // on remet l'ordre fautif plus loin
				o = nextO;
			}
		} while(restart);
		o.ordre.orderSent();
		return o;
	}

	private void addToBuffer(Order o)
	{
		buffer.add(o);
	}
	
	public Ticket run()
	{
		// TODO ordre ascii
		return null;
	}

	/**
	 * Ordre long de suivi de trajectoire
	 * 
	 * @param vitesseInitiale
	 * @param marcheAvant
	 * @return
	 */
	public Ticket followTrajectory()
	{
		log.write("On commence à suivre la trajectoire.", Subject.COMM);
		addToBuffer(new Order(Id.FOLLOW_TRAJECTORY));
		return Id.FOLLOW_TRAJECTORY.ticket;
	}

	/**
	 * Ajout d'une demande d'ordre de s'arrêter
	 */
	public Ticket immobilise()
	{
		log.write("Stop !", Severity.WARNING, Subject.COMM);
		addToBuffer(new Order(Id.STOP));
		return Id.STOP.ticket;
	}

	XY_RW tmp = new XY_RW();

	/**
	 * Corrige la position du bas niveau
	 */
	public void setPosition(XY pos, double orientation)
	{
		log.write("Set position en "+pos+", "+orientation, Subject.COMM);
		ByteBuffer data = ByteBuffer.allocate(12);
		data.order(ByteOrder.LITTLE_ENDIAN);
		data.putInt((int)pos.getX());
		data.putInt((int)pos.getY());
		data.putFloat((float) orientation);
		addToBuffer(new Order(data, Id.SET_POSITION));
	}

	/**
	 * Corrige la position du bas niveau
	 */
	public void correctPosition(XY deltaPos, double deltaOrientation)
	{
		ByteBuffer data = ByteBuffer.allocate(12);
		data.order(ByteOrder.LITTLE_ENDIAN);
		data.putInt((int)deltaPos.getX());
		data.putInt((int)deltaPos.getY());
		data.putFloat((float) deltaOrientation);
		addToBuffer(new Order(data, Id.EDIT_POSITION));
	}

	/**
	 * Demande à être notifié du début du match
	 */
	public Ticket waitForJumper()
	{
		addToBuffer(new Order(Id.WAIT_FOR_JUMPER));
		return Id.WAIT_FOR_JUMPER.ticket;
	}

	/**
	 * Demande à être notifié de la fin du match
	 */
	public Ticket startMatchChrono()
	{
		addToBuffer(new Order(Id.START_MATCH_CHRONO));
		return Id.START_MATCH_CHRONO.ticket;
	}

	/**
	 * Demande la couleur au bas niveau
	 */
	public Ticket demandeCouleur()
	{
		addToBuffer(new Order(Id.ASK_COLOR));
		return Id.ASK_COLOR.ticket;
	}

	/**
	 * Demande le niveau de batterie
	 */
	public Ticket niveauBatterie()
	{
		addToBuffer(new Order(Id.GET_BATTERY));
		return Id.GET_BATTERY.ticket;
	}
	
	/**
	 * Ping
	 */
	public Ticket ping()
	{
		addToBuffer(new Order(Id.PING));
		return Id.PING.ticket;
	}
	
	/**
	 * Démarre un stream
	 */
	public void startStream(Id stream)
	{
		changeStream(stream, Channel.INSCRIPTION);
	}
	
	/**
	 * Arrête un stream
	 */
	public void stopStream(Id stream)
	{
		changeStream(stream, Channel.DESINSCRIPTION);
	}
	
	private void changeStream(Id stream, Channel ch)
	{
		// on vérifie qu'on ne cherche pas à s'abonner alors qu'on est déjà abonné (idem avec désabonné)
		ByteBuffer data = ByteBuffer.allocate(1);
		data.order(ByteOrder.LITTLE_ENDIAN);
		data.put(ch.code);
		addToBuffer(new Order(data, stream));
		stream.changeStreamState(ch);
	}
	
	/**
	 * Modifie des points déjà envoyés
	 * @param points
	 * @param indexTrajectory
	 * @throws InterruptedException
	 */
	public void editePointsTrajectoire(List<ItineraryPoint> points, int indexTrajectory) throws InterruptedException
	{
		log.write("Édition de " + points.size() + " points à partir de l'index " + indexTrajectory, Subject.COMM);
		envoiePointsTrajectoire(points, false, indexTrajectory);
	}

	/**
	 * Ajoute des points à la fin de la trajectoire
	 * @0 arc
	 * @throws InterruptedException 
	 */
	public void ajoutePointsTrajectoire(List<ItineraryPoint> points) throws InterruptedException
	{
		log.write("Ajout de " + points.size() + " points.", Subject.COMM);
		envoiePointsTrajectoire(points, true, 0);
	}
	
	private void envoiePointsTrajectoire(List<ItineraryPoint> points, boolean add, int indexTrajectory) throws InterruptedException
	{
		// on peut envoyer 11 arcs par trame au maximum
		int index = indexTrajectory;
		int nbEnvoi = (points.size() - 1) / 11 + 1;
		int modulo = points.size() % 11; // pour le dernier envoi
		Id id = add ? Id.ADD_POINTS : Id.EDIT_POINTS;
		for(int i = 0; i < nbEnvoi; i++)
		{
			int nbArc = 11;
			if(i == nbEnvoi - 1) // dernier envoi
				nbArc = modulo;
			ByteBuffer data;
			if(add)
			{
				data = ByteBuffer.allocate(22 * nbArc);
				data.order(ByteOrder.LITTLE_ENDIAN);
			}
			else
			{
				data = ByteBuffer.allocate(4 + 22 * nbArc);
				data.order(ByteOrder.LITTLE_ENDIAN);
				data.putInt(index);
			}

			int k = 11 * i;
			for(int j = 0; j < nbArc; j++)
			{
				ItineraryPoint c = points.get(k);
				log.write("Point " + k + " : " + c, Subject.COMM);
				addPoint(data, c, k == points.size() - 1);
				k++;
			}
			addToBuffer(new Order(data, id));
			id.ticket.attendStatus();
			index += nbArc;
		}
	}
	
	private void addPoint(ByteBuffer data, ItineraryPoint point, boolean last)
	{
		data.putInt((int)point.x);
		data.putInt((int)point.y);
		data.putFloat((float) point.orientation);
		data.putFloat((float)point.curvature);
		if(point.goingForward)
			data.putFloat((float) (1000*point.possibleSpeed));
		else
			data.putFloat((float) (-1000*point.possibleSpeed));
		data.put((byte) (point.stop ? 1 : 0));
		data.put((byte) (last ? 1 : 0));
	}

	/**
	 * Renvoie une estimation de la latence de la comm, en μs
	 * @throws InterruptedException 
	 */
	public void checkLatence() throws InterruptedException
	{
		int nbEssais = 1000;
		long avant = System.currentTimeMillis();
		for(int i = 0; i < nbEssais; i++)
			ping().attendStatus();
		double latency = 1000. * (System.currentTimeMillis() - avant) / (2*nbEssais);
		// on divise par 2 car il s'agit d'un aller-retour
		log.write("Latence estimée : "+latency+" μs", latency >= 500 ? Severity.CRITICAL : (latency >= 300 ? Severity.WARNING : Severity.INFO), Subject.COMM);
	}

	@Override
	public void plot(Chart a)
	{
		a.addData("Buffer d'ordre sortants", (double) buffer.size());
	}
	
}
