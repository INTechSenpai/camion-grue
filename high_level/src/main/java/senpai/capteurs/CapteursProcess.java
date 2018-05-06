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

package senpai.capteurs;

import senpai.comm.CommProtocol;
import senpai.obstacles.ObstaclesDynamiques;
import senpai.obstacles.ObstaclesFixes;
import senpai.robot.Robot;
import senpai.table.Cube;
import senpai.table.Table;
import senpai.utils.ConfigInfoSenpai;
import senpai.utils.Severity;
import senpai.utils.Subject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;

/**
 * Cette classe contient les informations sur la situation
 * spatiale des capteurs sur le robot.
 * 
 * @author pf
 *
 */

public class CapteursProcess
{
	protected Log log;
	private Table table;
//	private OutgoingOrderBuffer serie;

	private int nbCapteurs;
	private int distanceApproximation;
	private RectangularObstacle obstacleRobot;
	private Capteur[] capteurs;
//	private final double imprecisionMaxPos;
//	private final double imprecisionMaxAngle;

//	private long dateLastMesureCorrection = -1;
//	private final long peremptionCorrection;
	private final int distanceAuMurMinimumTourelle = 250;
//	private final boolean enableDynamicCorrection;
	private volatile boolean ongoingStaticCorrection;
//	private boolean scan = false;
//	private final int tailleBufferRecalage;
//	private final List<XYO> bufferDynamicCorrection = new ArrayList<XYO>();
//	private ObstaclesMemory obstacles;
	private ObstaclesDynamiques dynObs;
	private Robot robot;
	private volatile boolean needLast = false;
	private volatile int[] last;
	private final int margeIgnoreTourelle;
	
//	private List<SensorsData> mesuresScan = new ArrayList<SensorsData>();

	public CapteursProcess(ObstaclesDynamiques dynObs, Robot robot, Log log, RectangularObstacle obstacleRobot, Table table, Config config, GraphicDisplay buffer)
	{
		this.table = table;
		this.log = log;
		this.dynObs = dynObs;
		this.robot = robot;

		distanceApproximation = config.getInt(ConfigInfoSenpai.DISTANCE_MAX_ENTRE_MESURE_ET_OBJET);
		nbCapteurs = CapteursRobot.values().length;
		margeIgnoreTourelle = config.getInt(ConfigInfoSenpai.MARGE_IGNORE_TOURELLE);
		last = new int[nbCapteurs];
		
		this.obstacleRobot = obstacleRobot;
		
		capteurs = new Capteur[nbCapteurs];

		for(int i = 0; i < nbCapteurs; i++)
		{
			try {
				CapteursRobot c = CapteursRobot.values()[i];
				@SuppressWarnings("unchecked")
				// On utilise le seul constructeur
				Constructor<? extends Capteur> constructor = (Constructor<? extends Capteur>) c.classe.getConstructors()[0];
				capteurs[i] = constructor.newInstance(robot, config, c.pos, c.angle, c.type, c.sureleve);
			}
			catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				assert false : e;
			}
		}

		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ROBOT_AND_SENSORS))
			for(Capteur c : capteurs)
				buffer.addPrintable(c, c.type.couleur, Layer.FOREGROUND.layer);
	}

	/**
	 * Met à jour les obstacles mobiles
	 */
	public synchronized void updateObstaclesMobiles(SensorsData data)
	{
		if(needLast)
			synchronized(this)
			{
				for(int i = 0; i < nbCapteurs; i++)
					last[i] = data.mesures[i];
				needLast = false;
				notifyAll();
			}
		
		long avant = System.currentTimeMillis();

		double orientationRobot = data.cinematique.orientationReelle;
		XY positionRobot = data.cinematique.getPosition();

		obstacleRobot.update(positionRobot, orientationRobot);

		/**
		 * On update la table avec notre position
		 */
		for(Cube g : Cube.values())
			if(!table.isDone(g) && g.obstacle.isColliding(obstacleRobot))
			{
				log.write("Élément shooté", Subject.CAPTEURS);
				table.setDone(g);
			}

		/**
		 * Suppression des mesures qui sont hors-table ou qui voient un obstacle
		 * de table
		 */
		for(int i = 0; i < nbCapteurs; i++)
		{
			CapteursRobot c = CapteursRobot.values[i];

			XY positionVue = getPositionVue(capteurs[i], data.mesures[i], data.cinematique, data.angleTourelleGauche, data.angleTourelleDroite, data.angleGrue);
			if(positionVue == null)
			{
				c.isThereObstacle = false;
				continue;
			}

			boolean stop = false;

			if(c.isTourelle && robot.isProcheRobot(positionVue, margeIgnoreTourelle))
			{
				log.write("Une tourelle voit quelque chose dans le robot", Subject.CAPTEURS);
				c.isThereObstacle = false;
				continue;
			}
			
			/**
			 * Si ce qu'on voit est un obstacle de table, on l'ignore
			 */
			for(ObstaclesFixes o : ObstaclesFixes.values())
				if(o.isVisible(capteurs[i].sureleve) && o.obstacle.squaredDistance(positionVue) < distanceApproximation * distanceApproximation)
				{
//					log.write("Obstacle de table vu : " + o, Subject.CAPTEURS);
//						data.etats[i] = TraitementEtat.DANS_OBSTACLE_FIXE;
					stop = true;
					break;
				}

			if(stop)
			{
				c.isThereObstacle = false;
				continue;
			}
			
			if(c.isTourelle)
				if(positionVue.getY() > (2000 - distanceAuMurMinimumTourelle)
					|| 	positionVue.getY() < distanceAuMurMinimumTourelle
					|| positionVue.getX() > (1500 - distanceAuMurMinimumTourelle)
					|| positionVue.getX() < (-1500 + distanceAuMurMinimumTourelle))
				{
					c.isThereObstacle = false;
					continue;					
				}

			for(Cube o : Cube.values())
				if(!table.isDone(o) && o.isVisible(capteurs[i].sureleve) && o.obstacle.squaredDistance(positionVue) < distanceApproximation * distanceApproximation)
				{
					log.write("Élément de jeu vu : " + o, Subject.CAPTEURS);
					stop = true;
					break;
				}

			if(stop)
			{
				c.isThereObstacle = false;
				continue;
			}	
			
			/**
			 * Sinon, on ajoute
			 */
			XY_RW positionEnnemi = new XY_RW(data.mesures[i] + CapteursRobot.profondeur / 2, capteurs[i].orientationRelativeRotate, true);
			positionEnnemi.plus(capteurs[i].positionRelativeRotate);
			positionEnnemi.rotate(orientationRobot);
			positionEnnemi.plus(positionRobot);
			c.updateObstacle(positionEnnemi, orientationRobot + capteurs[i].orientationRelativeRotate);

//				ObstacleProximity obs = new ObstacleProximity(positionEnnemi, c.profondeurEnnemi, c.largeurEnnemi, orientationRobot + capteurs[i].orientationRelativeRotate, System.currentTimeMillis() + dureeAvantPeremption, data, i);

/*				if(c.current.isHorsTable())
				{
					log.write(c+" voit quelque chose hors table.", Subject.CAPTEURS);
//					data.etats[i] = TraitementEtat.HORS_TABLE;
					c.isThereObstacle = false;
					continue; // hors table
				}*/
			
			c.isThereObstacle = true;
			log.write("Ajout d'un obstacle d'ennemi en " + positionEnnemi + " vu par " + c, Subject.CAPTEURS);

			dynObs.update(c);

//				data.etats[i] = TraitementEtat.OBSTACLE_CREE;
			
			// ObstacleProximity o =
//			gridspace.addObstacleAndRemoveNearbyObstacles(obs);

			/**
			 * Mise à jour de l'état de la table : un ennemi est passé
			 */
/*			for(GameElementNames g : GameElementNames.values())
				if(table.isDone(g) == EtatElement.INDEMNE && g.obstacle.isColliding(obs))
					table.setDone(g, EtatElement.PRIS_PAR_ENNEMI);
*/
		}
		
//		if(chemin.checkColliding(false))
//			obsbuffer.notify();

//		if(enableDynamicCorrection)
//			correctDynamicXYO(data);
		if(ongoingStaticCorrection)
			correctStaticXYO(data);
		double duree = System.currentTimeMillis() - avant;
		if(duree > 10)
			log.write("Durée traitement capteurs : "+duree, duree > 1000 ? Severity.CRITICAL : Severity.WARNING, Subject.CAPTEURS);

	}
	
	private void correctStaticXYO(SensorsData data)
	{
		for(CapteursCorrection c : CapteursCorrection.values())			
		{
			if(c.enable)
			{
				if(data.mesures[c.c1.ordinal()] >= CommProtocol.EtatCapteur.values().length
						&& data.mesures[c.c2.ordinal()] >= CommProtocol.EtatCapteur.values().length)
				{
					c.valc1.add(data.mesures[c.c1.ordinal()]);
					c.valc2.add(data.mesures[c.c2.ordinal()]);
				}
			}
		}
	}

	/**
	 * Corrige les données et envoie la correction au robot
	 * La correction n'est pas toujours possible
	 * 
	 * @param data
	 */
/*	private void correctDynamicXYO(SensorsData data)
	{
		for(CapteursCorrection c : CapteursCorrection.values())			
		{
			int index1 = c.c1.ordinal();
			int index2 = c.c2.ordinal();
			
			XY_RW pointVu1 = getPositionVue(capteurs[index1], data.mesures[index1], data.cinematique, data.angleTourelleGauche, data.angleTourelleDroite, data.angleGrue);
			if(pointVu1 == null)
				continue;

			XY_RW pointVu2 = getPositionVue(capteurs[index2], data.mesures[index2], data.cinematique, data.angleTourelleGauche, data.angleTourelleDroite, data.angleGrue);
			if(pointVu2 == null)
				continue;

			Mur mur1 = orientationMurProche(pointVu1);
			Mur mur2 = orientationMurProche(pointVu2);

			// ces capteurs ne voient pas un mur proche, ou pas le même
			if(mur1 == null || mur2 == null || mur1 != mur2)
				continue;

			XY delta = pointVu1.minusNewVector(pointVu2);
			double deltaOrientation = (mur1.orientation - delta.getArgument()) % Math.PI; // on
																				// veut
																				// une
																				// mesure
																				// précise,
																				// donc
																				// on
																				// évite
																				// getFastArgument

			// le delta d'orientation qu'on cherche est entre -PI/2 et PI/2
			if(deltaOrientation > Math.PI / 2)
				deltaOrientation -= Math.PI;
			else if(deltaOrientation < -Math.PI / 2)
				deltaOrientation += Math.PI;

			// log.debug("Delta orientation : "+deltaOrientation);

			pointVu1.rotate(deltaOrientation, data.cinematique.getPosition());
			pointVu2.rotate(deltaOrientation, data.cinematique.getPosition());

			double deltaX = 0;
			double deltaY = 0;
			if(mur1 == Mur.MUR_BAS)
				deltaY = -pointVu1.getY();
			else if(mur1 == Mur.MUR_HAUT)
				deltaY = -(pointVu1.getY() - 2000);
			else if(mur1 == Mur.MUR_GAUCHE)
				deltaX = -(pointVu1.getX() + 1500);
			else if(mur1 == Mur.MUR_DROIT)
				deltaX = -(pointVu1.getX() - 1500);

			XYO correction = new XYO(deltaX, deltaY, deltaOrientation);
			*/
			/*
			 * L'imprécision mesurée est trop grande. C'est probablement une
			 * erreur.
			 */
/*			if(Math.abs(deltaOrientation) > imprecisionMaxAngle)
			{
				log.write("Imprécision en angle trop grande !" + Math.abs(deltaOrientation), Subject.CORRECTION);
				continue;
			}
			*/
			/*
			 * L'imprécision mesurée est trop grande. C'est probablement une
			 * erreur.
			 */
/*			if(Math.abs(deltaX) > imprecisionMaxPos || Math.abs(deltaY) > imprecisionMaxPos)
			{
				log.write("Imprécision en position trop grande ! ("+deltaX+","+deltaY+")", Subject.CORRECTION);
				continue;
			}

			// log.debug("Correction : "+deltaX+" "+deltaY+"
			// "+deltaOrientation);

			if(System.currentTimeMillis() - dateLastMesureCorrection > peremptionCorrection) // trop
																								// de
																								// temps
																								// depuis
																								// le
																								// dernier
																								// calcul
			{
				log.write("Correction timeout", Subject.CORRECTION);
				bufferDynamicCorrection.clear();
			}

			bufferDynamicCorrection.add(correction);
			log.write("Intégration d'une donnée de correction", Subject.CORRECTION);
			if(bufferDynamicCorrection.size() == tailleBufferRecalage)
			{
				correctPosition(bufferDynamicCorrection);
				bufferDynamicCorrection.clear();
			}
		}
		dateLastMesureCorrection = System.currentTimeMillis();

	}

	private void correctPosition(List<XYO> bufferCorrection)
	{
		XY_RW posmoy = new XY_RW();
		double orientationmoy = 0;
		int nbPos = 0;
		for(int i = 0; i < bufferCorrection.size(); i++)
		{
			if(i >= bufferCorrection.size() / 2)
			{
				posmoy.plus(bufferCorrection.get(i).position);
				nbPos++;
			}
			orientationmoy += bufferCorrection.get(i).orientation;
		}
		posmoy.scalar(1. / nbPos);
		orientationmoy /= bufferCorrection.size();
		log.write("Envoi d'une correction XYO dynamique: " + posmoy + " " + orientationmoy, Subject.STATUS);
		serie.correctPosition(posmoy, orientationmoy);
	}*/

	/**
	 * Renvoie la position vue par ce capteurs
	 * 
	 * @param c
	 * @param mesure
	 * @param cinematique
	 * @return
	 */
	private XY_RW getPositionVue(Capteur c, int mesure, Cinematique cinematique, double angleRoueGauche, double angleRoueDroite, double angleGrue)
	{
		c.computePosOrientationRelative(cinematique, angleRoueGauche, angleRoueDroite, angleGrue);

		/**
		 * Si le capteur voit trop proche ou trop loin, on ne peut pas lui faire
		 * confiance
		 */
		
		assert mesure >= 0 : "Mesure de capteur négative ! "+c+" "+mesure;
		
		if(mesure == CommProtocol.EtatCapteur.TROP_PROCHE.ordinal())
			mesure = 15;
		
		else if(mesure < CommProtocol.EtatCapteur.values().length)
		{
//			log.write("Capteur "+c+" : "+CommProtocol.EtatCapteur.values()[mesure], Subject.CAPTEURS);
			return null;
		}
		
/*		if(mesure <= c.distanceMin || mesure >= c.portee)
		{
			if(mesure == 0)
				data.etats[nbCapteur] = TraitementEtat.DESACTIVE;
			else if(mesure <= c.distanceMin)
				data.etats[nbCapteur] = TraitementEtat.TROP_PROCHE;
			else
				data.etats[nbCapteur] = TraitementEtat.TROP_LOIN;
				
			// log.debug("Mesure d'un capteur trop loin ou trop proche.");
			return null;
		}*/

		XY_RW positionVue = new XY_RW(mesure, c.orientationRelativeRotate, true);
		positionVue.plus(c.positionRelativeRotate);
		positionVue.rotate(cinematique.orientationReelle);
		positionVue.plus(cinematique.getPosition());
		return positionVue;
	}

	/**
	 * Renvoie l'orientation du mur le plus proche.
	 * Renvoie null si aucun mur proche ou ambiguité (dans un coin)
	 * 
	 * @param pos
	 * @return
	 */
/*	private Mur orientationMurProche(XY pos)
	{
		double distanceMax = 3 * imprecisionMaxPos; // c'est une première
													// approximation, on peut
													// être bourrin
		boolean murBas = Math.abs(pos.getY()) < distanceMax;
		boolean murDroit = Math.abs(pos.getX() - 1500) < distanceMax;
		boolean murHaut = Math.abs(pos.getY() - 2000) < distanceMax;
		boolean murGauche = Math.abs(pos.getX() + 1500) < distanceMax;
		log.write(distanceMax+" "+murBas+" "+murDroit+" "+murHaut+" "+murGauche, Subject.CORRECTION);

		// log.debug("État mur : "+murBas+" "+murDroit+" "+murHaut+"
		// "+murGauche);

		if(!(murBas ^ murDroit ^ murHaut ^ murGauche)) // cette condition est
														// vraie si on est près
														// de 0 ou de 2 murs
			return null;

		if(murBas)
			return Mur.MUR_BAS;
		else if(murDroit)
			return Mur.MUR_DROIT;
		else if(murGauche)
			return Mur.MUR_GAUCHE;
		return Mur.MUR_HAUT;
	}*/
	
	public void startStaticCorrection(CapteursCorrection... c)
	{
		for(CapteursCorrection cc : c)
			cc.enable = true;
		ongoingStaticCorrection = true;		
	}
	
	public Integer getDistance(CapteursCorrection c, int nb)
	{
		if(c.valc1.isEmpty() || c.valc2.isEmpty())
			return null;
		List<Integer> l;
		if(nb == 0)
			l = c.valc1;
		else
			l = c.valc2;
		Collections.sort(l);
		return l.get(l.size() / 2);
	}
	
	public XYO doStaticCorrection(long duree, CapteursCorrection... c) throws InterruptedException
	{
		startStaticCorrection(c);
		Thread.sleep(duree);
		return endStaticCorrection();
	}
	
	public void cancelStaticCorrection()
	{
		ongoingStaticCorrection = false;	
		for(CapteursCorrection c : CapteursCorrection.values())			
		{
			c.enable = false;
			c.valc1.clear();
			c.valc2.clear();
		}
	}
		
	public XYO endStaticCorrection()
	{
		Cinematique cinem = robot.getCinematique();
		ongoingStaticCorrection = false;
		
		XY_RW totalDeltaPos = new XY_RW();
		double totalDeltaAngle = 0;
		int nb = 0;
		
		for(CapteursCorrection c : CapteursCorrection.values())			
		{
			if(c.enable)
			{
				if(c.valc1.isEmpty() || c.valc2.isEmpty())
				{
					log.write("Aucune valeur pour "+c+" !", Subject.CORRECTION);
					continue;
				}

				int mesure1 = getDistance(c, 0);
				int mesure2 = getDistance(c, 1);

				/*Collections.sort(c.valc1);
				int mesure1 = c.valc1.get(c.valc1.size() / 2);
	
				Collections.sort(c.valc2);
				int mesure2 = c.valc2.get(c.valc2.size() / 2);*/
				
				log.write("Distance médiane de "+c.c1+" : "+mesure1+" ("+c.valc1.size()+" valeurs)", Subject.CORRECTION);
				log.write("Distance médiane de "+c.c2+" : "+mesure2+" ("+c.valc2.size()+" valeurs)", Subject.CORRECTION);
				
				XY_RW pointVu1 = getPositionVue(capteurs[c.c1.ordinal()], mesure1, cinem, 0, 0, 0);
				if(pointVu1 == null)
					continue;
		
				XY_RW pointVu2 = getPositionVue(capteurs[c.c2.ordinal()], mesure2, cinem, 0, 0, 0);
				if(pointVu2 == null)
					continue;
		
				double deltaOrientation;
				if(c.c1 == c.c2)
					deltaOrientation = 0;
				else
				{
					XY delta = pointVu1.minusNewVector(pointVu2);
					deltaOrientation = (-delta.getArgument()) % (Math.PI / 2); // on
																						// veut
																						// une
																						// mesure
																						// précise,
																						// donc
																						// on
																						// évite
																						// getFastArgument
			
					// le delta d'orientation qu'on cherche est entre -PI/4 et PI/4
					if(deltaOrientation > Math.PI / 4)
						deltaOrientation -= Math.PI / 2;
					else if(deltaOrientation < -Math.PI / 4)
						deltaOrientation += Math.PI / 2;
			
					log.write(c+", delta orientation : "+deltaOrientation, Subject.CORRECTION);

					// log.debug("Delta orientation : "+deltaOrientation);
				}
				
				// orientation corrigée du robot entre -pi/4 et pi/4
				double orientationRobot = (cinem.orientationReelle + deltaOrientation) % (Math.PI / 2);
				if(orientationRobot > Math.PI / 4)
					orientationRobot -= Math.PI / 2;
				else if(orientationRobot < -Math.PI / 4)
					orientationRobot += Math.PI / 2;
				
				double distanceRobotMur = ((mesure1 + mesure2) / 2 + c.distanceToRobot) * Math.cos(orientationRobot);

				log.write("Delta distance "+distanceRobotMur, Subject.CORRECTION);

//				XY delta = new XY(distanceRobotMur, c.c1.angle + cinem.orientationReelle + deltaOrientation, true); 
				XY delta = new XY(0,0);
				log.write("Correction "+c+" : "+new XYO(delta.getX(), delta.getY(), deltaOrientation), Subject.CORRECTION);
				
				totalDeltaPos.plus(delta);
				totalDeltaAngle += deltaOrientation;				
				nb++;
				
				c.enable = false;
				c.valc1.clear();
				c.valc2.clear();
			}
		}

		if(nb != 0)
		{
			totalDeltaAngle /= nb;
			XYO correction = new XYO(totalDeltaPos, totalDeltaAngle);
			log.write("Envoi d'une correction XYO statique: " + correction, Subject.STATUS);
			robot.correctPosition(correction.position, correction.orientation);
			return correction;
		}
		return null;
	}
	
	public synchronized int[] getLast() throws InterruptedException
	{
		needLast = true;
		while(needLast)
			wait();
		return last;
	}
}
