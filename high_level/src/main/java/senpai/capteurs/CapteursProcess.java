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

package senpai.capteurs;

import senpai.ConfigInfoSenpai;
import senpai.CouleurSenpai;
import senpai.Subject;
import senpai.buffer.ObstaclesBuffer;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.capteurs.SensorsData.TraitementEtat;
import senpai.obstacles.ObstacleProximity;
import senpai.obstacles.ObstaclesFixes;
import senpai.table.EtatElement;
import senpai.table.GameElementNames;
import senpai.table.Table;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import pfg.config.Config;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XY_RW;
import pfg.graphic.log.Log;

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
	private OutgoingOrderBuffer serie;
	private ObstaclesBuffer obsbuffer;

	private int nbCapteurs;
	private int largeurEnnemi, longueurEnnemi;
	private int distanceApproximation;
	private RectangularObstacle obstacleRobot;
	private Capteur[] capteurs;
	private double imprecisionMaxPos;
	private double imprecisionMaxAngle;

	private long dateLastMesureCorrection = -1;
	private long peremptionCorrection;
	private boolean enableCorrection;
	private int indexCorrection = 0;
	private boolean scan = false;
	private Cinematique[] bufferCorrection;
	private long dureeAvantPeremption;

	private List<SensorsData> mesuresScan = new ArrayList<SensorsData>();

	public CapteursProcess(Log log, RectangularObstacle obstacleRobot, Table table, OutgoingOrderBuffer serie, Config config, ObstaclesBuffer obsbuffer)
	{
		this.table = table;
		this.log = log;
		this.serie = serie;
		this.obsbuffer = obsbuffer;

		dureeAvantPeremption = config.getInt(ConfigInfoSenpai.DUREE_PEREMPTION_OBSTACLES);
		largeurEnnemi = config.getInt(ConfigInfoSenpai.LARGEUR_OBSTACLE_ENNEMI);
		longueurEnnemi = config.getInt(ConfigInfoSenpai.LONGUEUR_OBSTACLE_ENNEMI);
		distanceApproximation = config.getInt(ConfigInfoSenpai.DISTANCE_MAX_ENTRE_MESURE_ET_OBJET);
		nbCapteurs = CapteursRobot.values().length;
		imprecisionMaxPos = config.getDouble(ConfigInfoSenpai.IMPRECISION_MAX_POSITION);
		imprecisionMaxAngle = config.getDouble(ConfigInfoSenpai.IMPRECISION_MAX_ORIENTATION);
		bufferCorrection = new Cinematique[config.getInt(ConfigInfoSenpai.TAILLE_BUFFER_RECALAGE)];
		peremptionCorrection = config.getInt(ConfigInfoSenpai.PEREMPTION_CORRECTION);
		enableCorrection = config.getBoolean(ConfigInfoSenpai.ENABLE_CORRECTION);

		this.obstacleRobot = obstacleRobot;
		
		capteurs = new Capteur[nbCapteurs];

		for(int i = 0; i < nbCapteurs; i++)
		{
			try {
				CapteursRobot c = CapteursRobot.values()[i];
				@SuppressWarnings("unchecked")
				// On utilise le seul constructeur
				Constructor<? extends Capteur> constructor = (Constructor<? extends Capteur>) c.classe.getConstructors()[0];
				capteurs[i] = constructor.newInstance(config, c.pos, c.angle, c.type, c.sureleve);
			}
			catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				assert false : e;
			}
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
		// on ne s'occupe que des tof avant
		int[] tofAvant = new int[] {CapteursRobot.ToF_AVANT_DROITE.ordinal(), CapteursRobot.ToF_AVANT_GAUCHE.ordinal()};
		for(SensorsData data : mesuresScan)
		{			
			double orientationRobot = data.cinematique.orientationReelle;
			XY positionRobot = data.cinematique.getPosition();
			for(int i = 0; i < nbCapteurs; i++)
				data.etats[i] = TraitementEtat.DESACTIVE_SCAN;
			for(int j = 0; j < 2; j++)
			{
				int i = tofAvant[j];
				data.etats[i] = TraitementEtat.SCAN;
 				XY positionVue = getPositionVue(data, i, capteurs[i], data.mesures[i], data.cinematique, data.angleRoueGauche, data.angleRoueDroite);
				if(positionVue == null)
					continue;
				
				XY_RW positionEnnemi = new XY_RW(data.mesures[i] + longueurEnnemi / 2, capteurs[i].orientationRelativeRotate, true);
				positionEnnemi.plus(capteurs[i].positionRelativeRotate);
				positionEnnemi.rotate(orientationRobot);
				positionEnnemi.plus(positionRobot);
				ObstacleProximity obs = new ObstacleProximity(positionEnnemi, longueurEnnemi, (int)(data.mesures[i] * 0.2), orientationRobot + capteurs[i].orientationRelativeRotate, System.currentTimeMillis(), data, i);

				if(obs.isHorsTable())
					continue; // hors table

				obsbuffer.addNewObstacle(obs);
			}

		}
		
		scan = false;
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

		try
		{
			/**
			 * Suppression des mesures qui sont hors-table ou qui voient un obstacle
			 * de table
			 */
			for(int i = 0; i < nbCapteurs; i++)
			{
				CapteursRobot c = CapteursRobot.values[i];
	
				XY positionVue = getPositionVue(data, i, capteurs[i], data.mesures[i], data.cinematique, data.angleRoueGauche, data.angleRoueDroite);
				if(positionVue == null)
					continue;
	
				boolean stop = false;
	
				/**
				 * Si ce qu'on voit est un obstacle de table, on l'ignore
				 */
				for(ObstaclesFixes o : ObstaclesFixes.values())
					if(o.isVisible(capteurs[i].sureleve) && o.obstacle.squaredDistance(positionVue) < distanceApproximation * distanceApproximation)
					{
						log.write("Obstacle de table vu : " + o, Subject.CAPTEURS);
						data.etats[i] = TraitementEtat.DANS_OBSTACLE_FIXE;
						stop = true;
						break;
					}
	
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
	
				ObstacleProximity obs = new ObstacleProximity(positionEnnemi, longueurEnnemi, largeurEnnemi, orientationRobot + capteurs[i].orientationRelativeRotate, System.currentTimeMillis() + dureeAvantPeremption, data, i);
	
				if(obs.isHorsTable())
				{
					// if(debugCapteurs)
					// log.debug("Hors table !");
					data.etats[i] = TraitementEtat.HORS_TABLE;
					continue; // hors table
				}
	
				log.write("Ajout d'un obstacle d'ennemi en " + positionEnnemi + " vu par " + c, Subject.CAPTEURS);
	
				obsbuffer.addNewObstacle(obs);

				data.etats[i] = TraitementEtat.OBSTACLE_CREE;
				
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
	
			if(enableCorrection)
				correctXYO(data);
		}
		finally
		{
			assert data.checkTraitementEtat();
		}
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
			
			XY_RW pointVu1 = getPositionVue(data, index1, capteurs[index1], data.mesures[index1], data.cinematique, data.angleRoueGauche, data.angleRoueDroite);
			if(pointVu1 == null)
				continue;

			XY_RW pointVu2 = getPositionVue(data, index2, capteurs[index2], data.mesures[index2], data.cinematique, data.angleRoueGauche, data.angleRoueDroite);
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
				log.write("Imprécision en angle trop grande !" + Math.abs(deltaOrientation), Subject.CORRECTION);
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
				log.write("Imprécision en position trop grande ! ("+deltaX+","+deltaY+")", Subject.CORRECTION);
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
				log.write("Correction timeout", Subject.CORRECTION);
				indexCorrection = 0;
			}

			bufferCorrection[indexCorrection] = correction;
			indexCorrection++;
			log.write("Intégration d'une donnée de correction", Subject.CORRECTION);
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
				log.write("Envoi d'une correction XYO : " + posmoy + " " + orientationmoy, Subject.CORRECTION);
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
	private XY_RW getPositionVue(SensorsData data, int nbCapteur, Capteur c, int mesure, Cinematique cinematique, double angleRoueGauche, double angleRoueDroite)
	{
		c.computePosOrientationRelative(cinematique, angleRoueGauche, angleRoueDroite);

		/**
		 * Si le capteur voit trop proche ou trop loin, on ne peut pas lui faire
		 * confiance
		 */
		if(mesure <= c.distanceMin || mesure >= c.portee)
		{
			if(mesure == 0)
				data.etats[nbCapteur] = TraitementEtat.DESACTIVE;
			else if(mesure <= c.distanceMin)
				data.etats[nbCapteur] = TraitementEtat.TROP_PROCHE;
			else
				data.etats[nbCapteur] = TraitementEtat.TROP_LOIN;
				
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
														// vraie si on est près
														// de 1 ou de 3 murs
			return null;

		// la correction sur les murs gauche et droit sont désactivés
		
		// TODO
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
