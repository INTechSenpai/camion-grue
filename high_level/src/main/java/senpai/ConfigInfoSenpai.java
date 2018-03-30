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

package senpai;

import java.util.ArrayList;
import java.util.List;

import pfg.config.ConfigInfo;

/**
 * La config du robot
 * @author pf
 *
 */

public enum ConfigInfoSenpai implements ConfigInfo
{
	AFFICHAGE_TIMEOUT(5000), // timeout sur l'affichage (0 pour infini)
	REMOTE_CONTROL_PORT_NUMBER(13371), // port de la télécommande
	REMOTE_CONTROL(false), // active ou non le contrôle à distance
	CHECK_LATENCY(false), // estime la latence de la communication
	
	/**
	 * Infos sur le robot
	 */
	DILATATION_ROBOT_DSTARLITE(60), // dilatation des obstacles dans le D* Lite.
									// Comme c'est une heuristique, on peut
									// prendre plus petit que la vraie valeur
	CENTRE_ROTATION_ROUE_X(204), // la position du centre de rotation des roues.
									// Est utilisé pour la rotation des capteurs
	CENTRE_ROTATION_ROUE_Y(64),
	DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE(167), // distance entre le centre du robot
											// et le bord arrière du robot
											// non-déployé
	DEMI_LONGUEUR_NON_DEPLOYE_AVANT(248), // distance entre le centre du
												// robot et le bord avant du
												// robot non-déployé
	LARGEUR_NON_DEPLOYE(182), // distance entre le bord gauche et le bord droit
								// du robot non-déployé
	DILATATION_OBSTACLE_ROBOT(30), // la dilatation du robot dans l'A*. S'ajoute
									// à gauche et à droite

	/**
	 * Paramètres des scripts
	 */
	VITESSE_ROBOT_TEST(300), // vitesse de test en mm/s
	VITESSE_ROBOT_STANDARD(500), // vitesse standard en mm/s
	VITESSE_ROBOT_REPLANIF(200), // vitesse en replanification en mm/s

	/**
	 * Paramètres du log
	 */
//	FAST_LOG(false), // affichage plus rapide des logs
//	SAUVEGARDE_LOG(false), // sauvegarde les logs dans un fichier externe
//	AFFICHE_CONFIG(false), // affiche la configuration complète au lancement
//	COLORED_LOG(false), // de la couleur dans les sauvegardes de logs !

	/**
	 * Infos sur l'ennemi
	 */
	LARGEUR_OBSTACLE_ENNEMI(100), // largeur du robot vu
	LONGUEUR_OBSTACLE_ENNEMI(200), // longueur / profondeur du robot vu

	/**
	 * Paramètres du pathfinding
	 */
//	COURBURE_MAX(3), // quelle courbure maximale la trajectoire du robot
						// peut-elle avoir
//	TEMPS_ARRET(800), // temps qu'il faut au robot pour s'arrêter et repartir
						// (par exemple à cause d'un rebroussement)
//	PF_MARGE_AVANT_COLLISION(100), // combien de mm laisse-t-on au plus % avant
									// la collision
//	PF_MARGE_NECESSAIRE(40), // combien de mm le bas niveau DOIT-il toujours
								// avoir
//	PF_MARGE_PREFERABLE(60), // combien de mm le bas niveau devrait-il toujours
								// avoir
//	PF_MARGE_INITIALE(100), // combien de mm garde-t-on obligatoirement au début
							// de la replanification
//	DUREE_MAX_RECHERCHE_PF(10000), // durée maximale que peut prendre le
									// pathfinding
//	TAILLE_FAISCEAU_PF(20), // combien de voisins sont ajoutés à l'openset à
							// chaque itération. CONFIG IGNORÉE !
//	NB_ESSAIS_PF(100), // nombre d'essais du pathfinding
	ALLOW_PRECOMPUTED_PATH(true), // autorise-t-on l'utilisation de chemins
									// précalculés
	SAVE_FOUND_PATH(true), // sauvegarde tous les trajets calculés en match

	/**
	 * Paramètres de la comm
	 */
	SIMULE_COMM(false), // la comm doit-elle être simulée (utile pour debug du HL)
	COMM_MEDIUM("Ethernet"),

	SERIAL_BAUDRATE(115200), // le baudrate de la liaison série
	SERIAL_LOCAL_PORT("/dev/ttyS0"), // le port de la liaison série
	
	ETH_LL_PORT_NUMBER(80), // port socket LL
	ETH_LL_HOSTNAME_SERVER("172.16.0.2"), // adresse ip du LL. Un hostname fonctionne aussi

	/**
	 * Paramètres bas niveau des capteurs
	 */
//	SENSORS_SEND_PERIOD(40), // période d'envoi des infos des capteurs (ms)
//	SENSORS_PRESCALER(1), // sur combien de trames a-t-on les infos des capteurs

	/**
	 * Paramètres du traitement des capteurs
	 */
	DUREE_PEREMPTION_OBSTACLES(1000), // pendant combien de temps va-t-on garder
										// un obstacle de proximité
	DISTANCE_MAX_ENTRE_MESURE_ET_OBJET(50), // quelle marge d'erreur
											// autorise-t-on entre un objet et
											// sa détection
	DISTANCE_BETWEEN_PROXIMITY_OBSTACLES(5), // sous quelle distance
												// fusionne-t-on deux obstacles
												// de proximité ?
	IMPRECISION_MAX_POSITION(50.), // quelle imprecision maximale sur la
									// position du robot peut-on attendre (en
									// mm)
	IMPRECISION_MAX_ORIENTATION(0.1), // quelle imprecision maximale sur l'angle
										// du robot peut-on attendre (en
										// radians)
	TAILLE_BUFFER_RECALAGE(50), // combien de mesures sont nécessaires pour
								// obtenir une correction de recalage
	PEREMPTION_CORRECTION(100), // temps maximal entre deux mesures de
								// correction au sein d'un même buffer (en ms)
	ENABLE_CORRECTION(true), // la correction de position et d'orientation
								// est-elle activée ?
	WARM_UP_DURATION(5000), // durée du warm-up
	RAYON_ROBOT_SUPPRESSION_OBSTACLES_FIXES(300), // dans quel rayon
													// supprime-t-on les
													// obstacles fixes si on est
													// dedans
	SUPPRESSION_AUTO_OBSTACLES_FIXES(true), // si on démarre dans un obstacle
											// fixe, est-ce qu'on le vire ?
//	ENABLE_SCAN(true), // scan-t-on autour du robot s'il est coincé ?

	/**
	 * Interface graphique
	 */
//	GRAPHIC_HEURISTIQUE(false), // affichage des orientations heuristiques
								// données par le D* Lite
	GRAPHIC_ENABLE(false), // désactive tout affichage si faux (empêche le
							// thread d'affichage de se lancer)
//	GRAPHIC_D_STAR_LITE(false), // affiche les calculs du D* Lite
//	GRAPHIC_D_STAR_LITE_FINAL(false), // affiche l'itinéraire final du D* Lite
//	GRAPHIC_PROXIMITY_OBSTACLES(true), // affiche les obstacles de proximité
//	GRAPHIC_TRAJECTORY(false), // affiche les trajectoires temporaires
//	GRAPHIC_TRAJECTORY_ALL(false), // affiche TOUTES les trajectoires
									// temporaires
//	GRAPHIC_TRAJECTORY_FINAL(true), // affiche les trajectoires
//	GRAPHIC_FIXED_OBSTACLES(true), // affiche les obstacles fixes
//	GRAPHIC_GAME_ELEMENTS(true), // affiche les éléments de jeux
//	GRAPHIC_ROBOT_COLLISION(false), // affiche les obstacles du robot lors de la
									// vérification des collisions
	GRAPHIC_BACKGROUND_PATH("img/background-2017-color.png"), // affiche d'image
																// de la table
	GRAPHIC_ROBOT_PATH("img/robot_sans_roues.png"), // image du robot sans les
													// roues
	GRAPHIC_ROBOT_ROUE_GAUCHE_PATH("img/robot_roue_gauche.png"), // image de la
																	// roue
																	// gauche
	GRAPHIC_ROBOT_ROUE_DROITE_PATH("img/robot_roue_droite.png"), // image de la
																	// roue
																	// droite
//	GRAPHIC_PRODUCE_GIF(false), // produit un gif ?
//	GIF_FILENAME("output.gif"), // le nom du fichier du gif généré
//	GRAPHIC_BACKGROUND(true), // affiche d'image de la table
//	GRAPHIC_SIZE_X(1000), // taille par défaut (sans image) de la fenêtre
//	GRAPHIC_ALL_OBSTACLES(false), // affiche absolument tous les obstacles créés
	GRAPHIC_ROBOT_AND_SENSORS(true), // affiche le robot et ses capteurs
//	GRAPHIC_CERCLE_ARRIVEE(false), // affiche le cercle d'arrivée
//	GRAPHIC_TIME(false), // affiche le temps écoulé
	GRAPHIC_TRACE_ROBOT(true), // affiche la trace du robot
	GRAPHIC_EXTERNAL(true), // l'affichage doit-il être déporté par le serveur
							// d'affichage ?
	SAVE_VIDEO(true), // sauvegarde d'une "vidéo" pour visionner les
								// images plus tard
//	GRAPHIC_ZOOM(0), // zoom de la fenêtre. Si 0, aucun zoom. Sinon, zoom + focus sur le robot
	GRAPHIC_COMM_CHART(false), // active les graphes de debug de la communication
	GRAPHIC_CAPTEURS_CHART(false); // active les graphes de debug des capteurs


	private Object defaultValue;
	public boolean overridden = false;
	public volatile boolean uptodate;

	public static List<ConfigInfo> getGraphicConfigInfo()
	{
		List<ConfigInfo> out = new ArrayList<ConfigInfo>();
		for(ConfigInfoSenpai c : values())
			if(c.toString().startsWith("GRAPHIC_"))
				out.add(c);
		return out;
	}
	
	/**
	 * Par défaut, une valeur est constante
	 * 
	 * @param defaultValue
	 */
	private ConfigInfoSenpai(Object defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public Object getDefaultValue()
	{
		return defaultValue;
	}
}
