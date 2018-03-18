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

package senpai.threads.comm;

import java.nio.ByteBuffer;
import pfg.config.Config;
import pfg.log.Log;
import pfg.kraken.robot.Cinematique;
import senpai.Senpai;
import senpai.Senpai.ErrorCode;
import senpai.buffer.IncomingOrderBuffer;
import senpai.buffer.SensorsDataBuffer;
import senpai.capteurs.CapteursRobot;
import senpai.capteurs.SensorsData;
import senpai.comm.Paquet;
import senpai.comm.CommProtocol;
import senpai.comm.CommProtocol.Id;
import senpai.comm.CommProtocol.InOrder;
import senpai.robot.Robot;
import senpai.Subject;

/**
 * Thread qui écoute la série et appelle qui il faut.
 * 
 * @author pf
 *
 */

public class ThreadCommProcess extends Thread
{
	protected Log log;
	protected Config config;
	private IncomingOrderBuffer serie;
	private SensorsDataBuffer buffer;
	private Robot robot;
	private Senpai container;
	private Cinematique current = new Cinematique();

	private boolean capteursOn = false;
	private int nbCapteurs;

	public ThreadCommProcess(Log log, Config config, IncomingOrderBuffer serie, SensorsDataBuffer buffer, Robot robot, Senpai container)
	{
		this.container = container;
		this.log = log;
		this.config = config;
		this.serie = serie;
		this.buffer = buffer;
		this.robot = robot;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);

		nbCapteurs = CapteursRobot.values().length;
		try
		{
			while(true)
			{
//				long avant = System.currentTimeMillis();

				Paquet paquet = serie.take();

//				log.write("Durée avant obtention du paquet : " + (System.currentTimeMillis() - avant) + ". Traitement de " + paquet, Subject.COMM);

//				avant = System.currentTimeMillis();
				ByteBuffer data = paquet.message;

				/**
				 * Couleur du robot
				 */
				if(paquet.origine == Id.ASK_COLOR)
				{
					byte code = data.get();
					if(code == InOrder.COULEUR_VERT.codeInt)
					{
						paquet.origine.ticket.set(InOrder.COULEUR_VERT);
//						config.set(ConfigInfo.COULEUR, RobotColor.getCouleur(true));
					}
					else if(code == InOrder.COULEUR_ORANGE.codeInt)
					{
						paquet.origine.ticket.set(InOrder.COULEUR_ORANGE);
//						config.set(ConfigInfo.COULEUR, RobotColor.getCouleur(false));
					}
					else
					{
						paquet.origine.ticket.set(InOrder.COULEUR_ROBOT_INCONNU);
						assert code == InOrder.COULEUR_ROBOT_INCONNU.codeInt : "Code couleur inconnu : " + code;
					}
				}

				/**
				 * Capteurs
				 */
				else if(paquet.origine == Id.ODO_AND_SENSORS)
				{
					/**
					 * Récupération de la position et de l'orientation
					 */
					int xRobot = data.getInt();
					int yRobot = data.getInt();
					double orientationRobot = data.getFloat();
					double courbure = data.getFloat();
					int indexTrajectory = data.getInt();
					int[] mesures = new int[nbCapteurs];
					for(CapteursRobot c : CapteursRobot.values())
					{
						mesures[c.ordinal()] = data.getInt();
						log.write("Capteur " + c.name() + " : " + mesures[c.ordinal()], Subject.CAPTEURS);
					}
					double angleTourelleGauche = data.getFloat();
					double angleTourelleDroite = data.getFloat();
						
					// TODO : marche avant ?
					current.updateReel(xRobot, yRobot, orientationRobot, current.enMarcheAvant, courbure);

					robot.setCinematique(current);

					log.write("Le robot est en " + current.getPosition() + ", orientation : " + orientationRobot + ", index : " + indexTrajectory, Subject.CAPTEURS);

					if(capteursOn)
						buffer.add(new SensorsData(angleTourelleGauche, angleTourelleDroite, mesures, current));
				}

				/**
				 * Démarrage du match
				 */
				else if(paquet.origine == Id.WAIT_FOR_JUMPER)
				{
					capteursOn = true;
					paquet.origine.ticket.set(InOrder.ACK_SUCCESS);
				}
				
				else if(paquet.origine == Id.STOP)
				{
					paquet.origine.ticket.set(InOrder.ACK_SUCCESS);
				}

				else if(paquet.origine == Id.SEND_ARC)
				{
					paquet.origine.ticket.set(InOrder.ACK_SUCCESS);
				}
				
				else if(paquet.origine == Id.PING)
				{
					assert data.capacity() == 1 && data.get() == 0x00 : paquet;
					paquet.origine.ticket.set(InOrder.ACK_SUCCESS);
				}

				/**
				 * Fin du match, on coupe la série et on arrête ce thread
				 */
				else if(paquet.origine == Id.START_MATCH_CHRONO)
				{
					log.write("Fin du Match !", Subject.DUMMY);
					container.interruptWithCodeError(ErrorCode.END_OF_MATCH);

					// On attend d'être arrêté
					while(true)
						Thread.sleep(1000);
				}

				/**
				 * Le robot est arrivé après un arrêt demandé par le haut niveau
				 */
				else if(paquet.origine == Id.FOLLOW_TRAJECTORY)
				{
					byte code = data.get();
					if(code == 0)
						paquet.origine.ticket.set(InOrder.ROBOT_OK);
					else
					{
						log.write(CommProtocol.TrajEndMask.describe(code), Subject.CAPTEURS);
						paquet.origine.ticket.set(InOrder.ROBOT_KO);
					}
				}

				/*
				 * ACTIONNEURS
				 */
				// TODO
				
				else
					assert false : "On a ignoré une réponse " + paquet.origine + " (taille : " + data.capacity() + ")";
				
//				log.write("Durée de traitement de " + paquet.origine + " : " + (System.currentTimeMillis() - avant), Subject.COMM);
			}
		}
		catch(InterruptedException e)
		{
			log.write("Arrêt de " + Thread.currentThread().getName(), Subject.STATUS);
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			log.write("Arrêt inattendu de " + Thread.currentThread().getName() + " : " + e, Subject.STATUS);
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
			Thread.currentThread().interrupt();
		}
	}

}
