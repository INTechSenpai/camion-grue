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

package threads.serie;

import capteurs.CapteursRobot;
import capteurs.SensorsData;
import capteurs.SensorsDataBuffer;
import config.Config;
import config.ConfigInfo;
import container.Container;
import container.Container.ErrorCode;
import container.dependances.SerialClass;
import robot.Cinematique;
import robot.RobotColor;
import robot.RobotReal;
import serie.BufferIncomingOrder;
import serie.SerialProtocol.InOrder;
import serie.SerialProtocol.OutOrder;
import serie.trame.Frame.IncomingCode;
import serie.trame.Paquet;
import threads.ThreadService;
import utils.Log;
import utils.Log.Verbose;
import pathfinding.chemin.CheminPathfinding;

/**
 * Thread qui écoute la série et appelle qui il faut.
 * 
 * @author pf
 *
 */

public class ThreadSerialInputCoucheOrdre extends ThreadService implements SerialClass
{
	protected Log log;
	protected Config config;
	private BufferIncomingOrder serie;
	private SensorsDataBuffer buffer;
	private RobotReal robot;
	private CheminPathfinding chemin;
	private Container container;
	private Cinematique current = new Cinematique();

	public static boolean capteursOn = false;
	private int nbCapteurs;

	public ThreadSerialInputCoucheOrdre(Log log, Config config, BufferIncomingOrder serie, SensorsDataBuffer buffer, RobotReal robot, CheminPathfinding chemin, Container container)
	{
		this.container = container;
		this.log = log;
		this.config = config;
		this.serie = serie;
		this.buffer = buffer;
		this.robot = robot;
		this.chemin = chemin;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.debug("Démarrage de " + Thread.currentThread().getName());

		nbCapteurs = CapteursRobot.values().length;
		try
		{
			while(true)
			{
				long avant = System.currentTimeMillis();

				Paquet paquet;
				synchronized(serie)
				{
					if(serie.isEmpty())
						serie.wait();

					paquet = serie.poll();
				}

				log.debug("Durée avant obtention du paquet : " + (System.currentTimeMillis() - avant) + ". Traitement de " + paquet, Verbose.SERIE.masque);

				avant = System.currentTimeMillis();
				int[] data = paquet.message;

				/**
				 * Couleur du robot
				 */
				if(paquet.origine == OutOrder.ASK_COLOR)
				{
					if(data[0] == InOrder.COULEUR_BLEU.codeInt)
					{
						paquet.ticket.set(InOrder.COULEUR_BLEU);
						config.set(ConfigInfo.COULEUR, RobotColor.getCouleur(true));
					}
					else if(data[0] == InOrder.COULEUR_JAUNE.codeInt)
					{
						paquet.ticket.set(InOrder.COULEUR_JAUNE);
						config.set(ConfigInfo.COULEUR, RobotColor.getCouleur(false));
					}
					else
					{
						paquet.ticket.set(InOrder.COULEUR_ROBOT_INCONNU);
						if(data[0] != InOrder.COULEUR_ROBOT_INCONNU.codeInt)
							log.critical("Code couleur inconnu : " + data[0]);
					}
				}

				/**
				 * Capteurs
				 */
				else if(paquet.origine == OutOrder.START_STREAM_ALL && paquet.code == IncomingCode.STATUS_UPDATE)
				{
					/**
					 * Récupération de la position et de l'orientation
					 */
					int xRobot = data[0] << 4;
					xRobot += data[1] >> 4;
					xRobot -= 1500;
					int yRobot = (data[1] & 0x0F) << 8;
					yRobot = yRobot + data[2] - 1000;

					// On ne récupère pas toutes les infos mécaniques (la
					// courbure manque, marche avant, …)
					// Du coup, on récupère les infos théoriques (à partir du
					// chemin) qu'on complète
					double orientationRobot = ((data[3] << 8) + data[4]) / 1000.;
					int indexTrajectory = data[5];
					// log.debug("Index trajectory : "+indexTrajectory);

					Cinematique theorique = chemin.setCurrentIndex(indexTrajectory);

					if(theorique == null)
					{
//						log.debug("Cinématique théorique inconnue !", Verbose.PF.masque);
						current = new Cinematique(xRobot, yRobot, orientationRobot, true, 0);
					}
					else
					{
						theorique.copy(current);
						current.updateReel(xRobot, yRobot, orientationRobot, current.enMarcheAvant, current.courbureReelle);
					}

					robot.setCinematique(current);

					log.debug("Le robot est en " + current.getPosition() + ", orientation : " + orientationRobot + ", index : " + indexTrajectory, Verbose.ASSER.masque);

					boolean envoi = false;

					if(data.length > 6) // la présence de ces infos n'est pas
										// systématique
					{
						// changement de repère (cf la doc)
						double angleRoueGauche = -(data[6] - 150.) * Math.PI / 180.;
						double angleRoueDroite = -(data[7] - 150.) * Math.PI / 180.;

						robot.setAngleRoues(angleRoueGauche, angleRoueDroite);
//						log.debug("Angle roues : à gauche " + data[6] + ", à droite " + data[7], Verbose.ASSER.masque);

						/**
						 * Acquiert ce que voit les capteurs
						 */
						int[] mesures = new int[nbCapteurs];
						for(int i = 0; i < nbCapteurs; i++)
						{
							mesures[i] = data[8 + i] * CapteursRobot.values[i].type.conversion;
							log.debug("Capteur " + CapteursRobot.values[i].name() + " : " + mesures[i], Verbose.CAPTEURS.masque);
						}

						if(capteursOn)
						{
							buffer.add(new SensorsData(angleRoueGauche, angleRoueDroite, mesures, current));
							envoi = true;
						}
					}
					// il faut toujours envoyer la position
					if(!envoi)
						buffer.add(new SensorsData(current));
				}

				/**
				 * Démarrage du match
				 */
				else if(paquet.origine == OutOrder.WAIT_FOR_JUMPER)
				{
					capteursOn = true;
					synchronized(config)
					{
						config.set(ConfigInfo.DATE_DEBUT_MATCH, System.currentTimeMillis());
						config.set(ConfigInfo.MATCH_DEMARRE, true);
						paquet.ticket.set(InOrder.LONG_ORDER_ACK);
					}
				}

				else if(paquet.origine == OutOrder.SEND_ARC)
				{
					paquet.ticket.set(InOrder.ORDER_ACK);
				}

				/**
				 * Fin du match, on coupe la série et on arrête ce thread
				 */
				else if(paquet.origine == OutOrder.START_MATCH_CHRONO)
				{
					log.debug("Fin du Match !");

					if(data[0] == InOrder.ARRET_URGENCE.codeInt)
					{
						log.critical("Arrêt d'urgence provenant du bas niveau !");
						paquet.ticket.set(InOrder.ARRET_URGENCE);
						// On arrête le thread principal
						container.interruptWithCodeError(ErrorCode.EMERGENCY_STOP);
					}
					else
					{
						paquet.ticket.set(InOrder.MATCH_FINI);
						// On arrête le thread principal
						container.interruptWithCodeError(ErrorCode.END_OF_MATCH);
					}

					// On attend d'être arrêté
					while(true)
						Thread.sleep(1000);
				}

				/**
				 * Le robot est arrivé après un arrêt demandé par le haut niveau
				 */
				else if(paquet.origine == OutOrder.FOLLOW_TRAJECTORY && paquet.code == IncomingCode.EXECUTION_END)
				{
					chemin.setCurrentIndex(data[1]); // on a l'index courant

					if(data[0] == InOrder.ROBOT_ARRIVE.codeInt)
						paquet.ticket.set(InOrder.ROBOT_ARRIVE);
					else if(data[0] == InOrder.ROBOT_BLOCAGE_INTERIEUR.codeInt)
						paquet.ticket.set(InOrder.ROBOT_BLOCAGE_INTERIEUR);
					else if(data[0] == InOrder.ROBOT_BLOCAGE_EXTERIEUR.codeInt)
						paquet.ticket.set(InOrder.ROBOT_BLOCAGE_EXTERIEUR);
					else if(data[0] == InOrder.PLUS_DE_POINTS.codeInt)
						paquet.ticket.set(InOrder.PLUS_DE_POINTS);
					else if(data[0] == InOrder.STOP_REQUIRED.codeInt)
						paquet.ticket.set(InOrder.STOP_REQUIRED);
					else if(data[0] == InOrder.TROP_LOIN.codeInt)
						paquet.ticket.set(InOrder.TROP_LOIN);
				}

				/*
				 * ACTIONNEURS
				 */

				/**
				 * Actionneurs avec code de retour (forcément EXECUTION_END)
				 */
				else if(paquet.origine == OutOrder.PULL_DOWN_NET || paquet.origine == OutOrder.PULL_UP_NET || paquet.origine == OutOrder.PUT_NET_HALFWAY || paquet.origine == OutOrder.CROSS_FLIP_FLOP || paquet.origine == OutOrder.EJECT_LEFT_SIDE || paquet.origine == OutOrder.EJECT_RIGHT_SIDE || paquet.origine == OutOrder.REARM_LEFT_SIDE || paquet.origine == OutOrder.REARM_RIGHT_SIDE)
				{
					if(data[0] == InOrder.ACT_SUCCESS.codeInt)
						paquet.ticket.set(InOrder.ACT_SUCCESS);
					else
						paquet.ticket.set(InOrder.ACT_FAILURE);
				}

				else if(paquet.code == IncomingCode.EXECUTION_END)
					paquet.ticket.set(InOrder.LONG_ORDER_ACK);

				/**
				 * Les paquets dont l'état n'importe pas et sans donnée (par
				 * exemple PING ou STOP) n'ont pas besoin d'être traités
				 */
				else if(data.length != 0)
					log.critical("On a ignoré un paquet d'origine " + paquet.origine + " (taille : " + data.length + ")");

				log.debug("Durée de traitement de " + paquet.origine + " : " + (System.currentTimeMillis() - avant), Verbose.SERIE.masque);
			}
		}
		catch(InterruptedException e)
		{
			log.debug("Arrêt de " + Thread.currentThread().getName());
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			log.debug("Arrêt inattendu de " + Thread.currentThread().getName() + " : " + e);
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
			Thread.currentThread().interrupt();
		}
	}

}
