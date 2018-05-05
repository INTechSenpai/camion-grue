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

package senpai.utils;

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
//	AFFICHAGE_TIMEOUT(5000), // timeout sur l'affichage (0 pour infini)
	CHECK_LATENCY(false), // estime la latence de la communication
	
	/**
	 * Tourelles
	 */
	ANGLE_MIN_TOURELLE(10), // en degré
	ANGLE_MAX_TOURELLE(160),
	ANTICIPATION_TOURELLE(300), // distance où vise la tourelle sur la trajectoire, en mm
	MARGE_IGNORE_TOURELLE(100), // distance en mm autour du robot où sont ignorés les obstacles vus par les tourelles
	ENABLE_TOURELLE(true),
	
	/**
	 * Position initiale du robot
	 */
	INITIAL_X(1200),
	INITIAL_Y(1600),
	INITIAL_O(-Math.PI/2),
	COLOR_PATTERN(""),
	
	/**
	 * Infos sur le robot
	 */
//	CENTRE_ROTATION_TOURELLE_X(40), // par rapport au centre de rotation de la tourelle !
//	CENTRE_ROTATION_TOURELLE_Y(52),
	
	CENTRE_ROTATION_GRUE_X(-50), // position de la grue dans le repère du robot
	CENTRE_ROTATION_GRUE_Y(0),
	
	DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE(167), // distance entre le centre du robot
											// et le bord arrière du robot
											// non-déployé
	DEMI_LONGUEUR_NON_DEPLOYE_AVANT(248), // distance entre le centre du
												// robot et le bord avant du
												// robot non-déployé
	LARGEUR_NON_DEPLOYE(182), // distance entre le bord gauche et le bord droit
								// du robot non-déployé
	MARGE_PATHFINDING(20), // marge sur la dimension du robot
	DILATATION_OBSTACLE_ROBOT(30), // la dilatation du robot dans l'A*. S'ajoute
									// à gauche et à droite

	/**
	 * Paramètres des scripts
	 */
	VITESSE_ROBOT_TEST(300), // vitesse de test en mm/s
	VITESSE_ROBOT_STANDARD(500), // vitesse standard en mm/s
	VITESSE_ROBOT_REPLANIF(200), // vitesse en replanification en mm/s

	/**
	 * Paramètres du pathfinding
	 */
	ALLOW_PRECOMPUTED_PATH(true), // autorise-t-on l'utilisation de chemins
									// précalculés
//	SAVE_FOUND_PATH(true), // sauvegarde tous les trajets calculés en match

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
	DISTANCE_MAX_ENTRE_MESURE_ET_OBJET(50), // quelle marge d'erreur
											// autorise-t-on entre un objet et
											// sa détection
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
	ENABLE_DYNAMIC_CORRECTION(false), // la correction de position et d'orientation
								// est-elle activée ?
	WARM_UP_DURATION(5000), // durée du warm-up

	ENABLE_KNOWN_PATHS(true), // active les chemins enregistrés ?
	/**
	 * Log
	 */
	PRINT_STATUS(true),
	PRINT_CAPTEURS(false),
	PRINT_CORRECTION(false),
	PRINT_COMM(false),
	PRINT_TRAJECTORY(true),
	PRINT_SCRIPT(true),
	PRINT_WARNING(true),
	PRINT_CRITICAL(true),
	
	NO_OBSTACLES(false),
	
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
	GRAPHIC_ROBOT_PATH("/camion.png"), // image du robot sans les
													// roues
	GRAPHIC_SEEN_OBSTACLES(false), // affiche les obstacles vus
	GRAPHIC_ROBOT_AND_SENSORS(false), // affiche le robot et ses capteurs
	GRAPHIC_TRACE_ROBOT(false), // affiche la trace du robot
	GRAPHIC_EXTERNAL(false), // l'affichage doit-il être déporté par le serveur
							// d'affichage ?
	GRAPHIC_PATH(false), // affiche le chemin en cours
	SAVE_VIDEO(false), // sauvegarde d'une "vidéo" pour visionner les
								// images plus tard
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
