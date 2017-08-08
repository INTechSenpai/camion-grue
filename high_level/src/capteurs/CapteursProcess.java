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

package capteurs;

import robot.RobotReal;
import senpai.ConfigInfoSenpai;
import senpai.Senpai;
import senpai.Subject;

import java.util.ArrayList;
import java.util.List;

import comm.buffer.BufferOutgoingOrder;
import pfg.config.Config;
import pfg.kraken.obstacles.ObstacleRobot;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import table.GameElementNames;
import table.RealTable;
import table.EtatElement;

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
	private RealTable table;
	private RobotReal robot;
	private BufferOutgoingOrder serie;

	private int nbCapteurs;
	private int largeurEnnemi, longueurEnnemi;
	private int distanceApproximation;
	private ObstacleRobot obstacleRobot;
	private Capteur[] capteurs;
	private double imprecisionMaxPos;
	private double imprecisionMaxAngle;

	private long dateLastMesureCorrection = -1;
	private long peremptionCorrection;
	private boolean enableCorrection;
	private int indexCorrection = 0;
	private boolean scan = false;
	private Cinematique[] bufferCorrection;

	private List<SensorsData> mesuresScan = new ArrayList<SensorsData>();

	public CapteursProcess(Senpai container, Log log, RealTable table, RobotReal robot, BufferOutgoingOrder serie, Config config)
	{
		this.table = table;
		this.log = log;
		this.robot = robot;
		this.serie = serie;

		largeurEnnemi = config.getInt(ConfigInfoSenpai.LARGEUR_OBSTACLE_ENNEMI);
		longueurEnnemi = config.getInt(ConfigInfoSenpai.LONGUEUR_OBSTACLE_ENNEMI);
		distanceApproximation = config.getInt(ConfigInfoSenpai.DISTANCE_MAX_ENTRE_MESURE_ET_OBJET);
		nbCapteurs = CapteursRobot.values().length;
		imprecisionMaxPos = config.getDouble(ConfigInfoSenpai.IMPRECISION_MAX_POSITION);
		imprecisionMaxAngle = config.getDouble(ConfigInfoSenpai.IMPRECISION_MAX_ORIENTATION);
		bufferCorrection = new Cinematique[config.getInt(ConfigInfoSenpai.TAILLE_BUFFER_RECALAGE)];
		peremptionCorrection = config.getInt(ConfigInfoSenpai.PEREMPTION_CORRECTION);
		enableCorrection = config.getBoolean(ConfigInfoSenpai.ENABLE_CORRECTION);

		int demieLargeurNonDeploye = config.getInt(ConfigInfoSenpai.LARGEUR_NON_DEPLOYE) / 2;
		int demieLongueurArriere = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		int demieLongueurAvant = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);

		// on ne veut pas prendre en compte la marge quand on vérifie qu'on
		// collisionne un élément de jeu
		obstacleRobot = null;//new ObstacleRobot(demieLargeurNonDeploye, demieLongueurArriere, demieLongueurAvant, 0);

		capteurs = new Capteur[nbCapteurs];

		for(int i = 0; i < nbCapteurs; i++)
		{
			CapteursRobot c = CapteursRobot.values()[i];
//				capteurs[i] = container.make(c.classe, c.pos, c.angle, c.type, c.sureleve);
		}

/*		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ROBOT_AND_SENSORS))
			for(Capteur c : capteurs)
				buffer.add(c);*/
	}

	public synchronized void startScan()
	{
		mesuresScan.clear();
		scan = true;
	}

	public synchronized void endScan()
	{
		// on ne s'occupe que tes tof avant
		int[] tofAvant = new int[] {CapteursRobot.ToF_AVANT_DROITE.ordinal(), CapteursRobot.ToF_AVANT_GAUCHE.ordinal()};
		for(SensorsData data : mesuresScan)
		{			
			double orientationRobot = data.cinematique.orientationReelle;
			XY positionRobot = data.cinematique.getPosition();
			for(int j = 0; j < 2; j++)
			{
				int i = tofAvant[j];
 				XY positionVue = getPositionVue(capteurs[i], data.mesures[i], data.cinematique, data.angleRoueGauche, data.angleRoueDroite);
				if(positionVue == null)
					continue;
				
				XY_RW positionEnnemi = new XY_RW(data.mesures[i] + longueurEnnemi / 2, capteurs[i].orientationRelativeRotate, true);
				positionEnnemi.plus(capteurs[i].positionRelativeRotate);
				positionEnnemi.rotate(orientationRobot);
				positionEnnemi.plus(positionRobot);
//				RectangularObstacle obs = new RectangularObstacle(positionEnnemi, longueurEnnemi, (int)(data.mesures[i] * 0.2), orientationRobot + capteurs[i].orientationRelativeRotate, Couleur.SCAN);

//				if(obs.isHorsTable())
//					continue; // hors table

//				gridspace.addObstacleAndRemoveNearbyObstacles(obs);
			}

		}
		
		scan = false;
//		dstarlite.updateObstaclesEnnemi();
//		dstarlite.updateObstaclesTable();
	}

	/**
	 * Met à jour les obstacles mobiles
	 */
	public synchronized void updateObstaclesMobiles(SensorsData data)
	{
		double orientationRobot = data.cinematique.orientationReelle;
		XY positionRobot = data.cinematique.getPosition();

		obstacleRobot.update(positionRobot, orientationRobot);

		/**
		 * On update la table avec notre position
		 */
		for(GameElementNames g : GameElementNames.values())
			if(table.isDone(g).hash < EtatElement.PRIS_PAR_NOUS.hash && g.obstacle.isColliding(obstacleRobot))
			{
				// if(debugCapteurs)
				// log.debug("Élément shooté : "+g);
				table.setDone(g, EtatElement.PRIS_PAR_NOUS); // on est sûr de
																// l'avoir
																// shooté
			}

		// parfois on n'a pas de mesure
		if(data.mesures == null)
			return;

		if(scan)
		{
			mesuresScan.add(data);
			return;
		}

		/**
		 * Suppression des mesures qui sont hors-table ou qui voient un obstacle
		 * de table
		 */
		for(int i = 0; i < nbCapteurs; i++)
		{
			CapteursRobot c = CapteursRobot.values[i];

			XY positionVue = getPositionVue(capteurs[i], data.mesures[i], data.cinematique, data.angleRoueGauche, data.angleRoueDroite);
			if(positionVue == null)
				continue;

			boolean stop = false;

			/**
			 * Si ce qu'on voit est un obstacle de table, on l'ignore
			 */
/*			for(ObstaclesFixes o : ObstaclesFixes.values())
				if(o.isVisible(capteurs[i].sureleve) && o.getObstacle().squaredDistance(positionVue) < distanceApproximation * distanceApproximation)
				{
					log.debug("Obstacle de table vu : " + o, Verbose.CAPTEURS.masque);
					stop = true;
					break;
				}*/

			if(stop)
				continue;

			for(GameElementNames o : GameElementNames.values())
				if(table.isDone(o) != EtatElement.PRIS_PAR_NOUS && o.isVisible(c, capteurs[i].sureleve) && o.obstacle.squaredDistance(positionVue) < distanceApproximation * distanceApproximation)
				{
					log.write("Élément de jeu vu : " + o, Subject.CAPTEURS);
					stop = true;
					break;
				}

			if(stop)
				continue;

			/**
			 * Sinon, on ajoute
			 */
			XY_RW positionEnnemi = new XY_RW(data.mesures[i] + longueurEnnemi / 2, capteurs[i].orientationRelativeRotate, true);
			positionEnnemi.plus(capteurs[i].positionRelativeRotate);
			positionEnnemi.rotate(orientationRobot);
			positionEnnemi.plus(positionRobot);

			RectangularObstacle obs = null;//new RectangularObstacle(positionEnnemi, longueurEnnemi, largeurEnnemi, orientationRobot + capteurs[i].orientationRelativeRotate, c.type.couleurOrig);

/*			if(obs.isHorsTable())
			{
				// if(debugCapteurs)
				// log.debug("Hors table !");
				continue; // hors table
			}*/

			log.write("Ajout d'un obstacle d'ennemi en " + positionEnnemi + " vu par " + c, Subject.CAPTEURS);

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

//		dstarlite.updateObstaclesEnnemi();
//		dstarlite.updateObstaclesTable();
//		chemin.checkColliding(false);

		if(enableCorrection)
			correctXYO(data);
	}

	/**
	 * Corrige les données et envoie la correction au robot
	 * La correction n'est pas toujours possible
	 * 
	 * @param data
	 */
	private void correctXYO(SensorsData data)
	{
		int index1, index2;
		for(int k = 0; k < 2; k++)
		{
			if(k == 0)
			{
				index1 = CapteursRobot.ToF_LATERAL_GAUCHE_AVANT.ordinal();
				index2 = CapteursRobot.ToF_LATERAL_GAUCHE_ARRIERE.ordinal();
			}
			else
			{
				index1 = CapteursRobot.ToF_LATERAL_DROITE_AVANT.ordinal();
				index2 = CapteursRobot.ToF_LATERAL_DROITE_ARRIERE.ordinal();
			}
			
			// on serait pas assez précis
			if(data.mesures[index1] <= 4 || data.mesures[index2] <= 4)
				continue;
			
			XY_RW pointVu1 = getPositionVue(capteurs[index1], data.mesures[index1], data.cinematique, data.angleRoueGauche, data.angleRoueDroite);
			if(pointVu1 == null)
				continue;

			XY_RW pointVu2 = getPositionVue(capteurs[index2], data.mesures[index2], data.cinematique, data.angleRoueGauche, data.angleRoueDroite);
			if(pointVu2 == null)
				continue;

			Mur mur1 = orientationMurProche(pointVu1);
			Mur mur2 = orientationMurProche(pointVu2);

			// log.debug("PointVu1 : "+pointVu1);
			// log.debug("PointVu2 : "+pointVu2);

			// log.debug("Murs : "+mur1+" "+mur2);

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

			/*
			 * L'imprécision mesurée est trop grande. C'est probablement une
			 * erreur.
			 */
			if(Math.abs(deltaOrientation) > imprecisionMaxAngle)
			{
				log.write("Imprécision en angle trop grande !" + Math.abs(deltaOrientation), Subject.DUMMY);
				continue;
			}

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

			/*
			 * L'imprécision mesurée est trop grande. C'est probablement une
			 * erreur.
			 */
			if(Math.abs(deltaX) > imprecisionMaxPos || Math.abs(deltaY) > imprecisionMaxPos)
			{
				log.write("Imprécision en position trop grande ! ("+deltaX+","+deltaY+")", Subject.DUMMY);
				continue;
			}

			// log.debug("Correction : "+deltaX+" "+deltaY+"
			// "+deltaOrientation);

			Cinematique correction = new Cinematique(deltaX, deltaY, deltaOrientation, true, 0);
			if(System.currentTimeMillis() - dateLastMesureCorrection > peremptionCorrection) // trop
																								// de
																								// temps
																								// depuis
																								// le
																								// dernier
																								// calcul
			{
				log.write("Correction timeout", Subject.DUMMY);
				indexCorrection = 0;
			}

			bufferCorrection[indexCorrection] = correction;
			indexCorrection++;
			log.write("Intégration d'une donnée de correction", Subject.DUMMY);
			if(indexCorrection == bufferCorrection.length)
			{
				XY_RW posmoy = new XY_RW();
				double orientationmoy = 0;
				for(int i = 0; i < bufferCorrection.length; i++)
				{
					if(i >= bufferCorrection.length / 2)
						posmoy.plus(bufferCorrection[i].getPosition());
					orientationmoy += bufferCorrection[i].orientationReelle;
				}
				posmoy.scalar(2. / bufferCorrection.length);
				orientationmoy /= bufferCorrection.length;
				log.write("Envoi d'une correction XYO : " + posmoy + " " + orientationmoy, Subject.DUMMY);
				serie.correctPosition(posmoy, orientationmoy);
				indexCorrection = 0;
			}
		}
		dateLastMesureCorrection = System.currentTimeMillis();

	}

	/**
	 * Renvoie la position vue par ce capteurs
	 * 
	 * @param c
	 * @param mesure
	 * @param cinematique
	 * @return
	 */
	private XY_RW getPositionVue(Capteur c, int mesure, Cinematique cinematique, double angleRoueGauche, double angleRoueDroite)
	{
		c.computePosOrientationRelative(cinematique, angleRoueGauche, angleRoueDroite);

		/**
		 * Si le capteur voit trop proche ou trop loin, on ne peut pas lui faire
		 * confiance
		 */
		if(mesure <= c.distanceMin || mesure >= c.portee)
		{
			// log.debug("Mesure d'un capteur trop loin ou trop proche.");
			return null;
		}

		XY_RW positionVue = new XY_RW(mesure, c.orientationRelativeRotate, true);
		positionVue.plus(c.positionRelativeRotate);
		positionVue.rotate(cinematique.orientationReelle);
		positionVue.plus(cinematique.getPosition());
		return positionVue;
	}

	private enum Mur
	{
		MUR_HAUT(0),
		MUR_BAS(0),
		MUR_GAUCHE(Math.PI / 2),
		MUR_DROIT(Math.PI / 2);

		private double orientation;

		private Mur(double orientation)
		{
			this.orientation = orientation;
		}
	}

	/**
	 * Renvoie l'orientation du mur le plus proche.
	 * Renvoie null si aucun mur proche ou ambiguité (dans un coin)
	 * 
	 * @param pos
	 * @return
	 */
	private Mur orientationMurProche(XY pos)
	{
		double distanceMax = 3 * imprecisionMaxPos; // c'est une première
													// approximation, on peut
													// être bourrin
		boolean murBas = Math.abs(pos.getY()) < distanceMax;
		boolean murDroit = Math.abs(pos.getX() - 1500) < distanceMax;
		boolean murHaut = Math.abs(pos.getY() - 2000) < distanceMax;
		boolean murGauche = Math.abs(pos.getX() + 1500) < distanceMax;

		// log.debug("État mur : "+murBas+" "+murDroit+" "+murHaut+"
		// "+murGauche);

		if(!(murBas ^ murDroit ^ murHaut ^ murGauche)) // cette condition est
														// fausse si on est près
														// de 0 ou de 2 murs
			return null;

		// la correction sur les murs gauche et droit sont désactivés
		
		if(murBas)
			return Mur.MUR_BAS;
		else if(murDroit)
			return null;
//			return Mur.MUR_DROIT;
		else if(murGauche)
			return null;
//			return Mur.MUR_GAUCHE;
		return Mur.MUR_HAUT;
	}
}
