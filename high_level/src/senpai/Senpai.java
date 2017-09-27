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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import buffer.OutgoingOrderBuffer;
import comm.Communication;
import obstacles.ObstaclesFixes;
import pfg.config.Config;
import pfg.config.ConfigInfo;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.ThreadPrintServer;
import pfg.graphic.ThreadPrinting;
import pfg.graphic.ThreadSaveVideo;
import pfg.graphic.DebugTool;
import pfg.graphic.Vec2RO;
import pfg.graphic.WindowFrame;
import pfg.injector.Injector;
import pfg.injector.InjectorException;
import pfg.kraken.Kraken;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.utils.XY;
import pfg.log.Log;
import robot.Robot;
import robot.Speed;
import threads.ThreadName;
import threads.ThreadRemoteControl;
import threads.ThreadShutdown;

/**
 * 
 * Gestionnaire de la durée de vie des objets dans le code.
 * Permet à n'importe quelle classe implémentant l'interface "Service"
 * d'appeller d'autres instances de services via son constructeur.
 * Une classe implémentant Service n'est instanciée que par la classe
 * "Container"
 * 
 * @author pf
 */
public class Senpai
{
	private Log log;
	private Config config;
	private Injector injector;

	private static int nbInstances = 0;
	private Thread mainThread;
	private ErrorCode errorCode = ErrorCode.NO_ERROR;
	private boolean shutdown = false;
	private boolean simuleComm;
	
	public boolean isShutdownInProgress()
	{
		return shutdown;
	}
	
	public enum ErrorCode
	{
		NO_ERROR(0),
		END_OF_MATCH(0),
		EMERGENCY_STOP(2),
		TERMINATION_SIGNAL(3);
		
		public final int code;
		
		private ErrorCode(int code)
		{
			this.code = code;
		}
	}
	
	/**
	 * Fonction appelé automatiquement à la fin du programme.
	 * ferme la connexion serie, termine les différents threads, et ferme le
	 * log.
	 * 
	 * @throws InterruptedException
	 * @throws ContainerException
	 */
	public synchronized ErrorCode destructor() throws InterruptedException
	{
		assert Thread.currentThread().getId() == mainThread.getId() : "Appel au destructeur depuis un thread !";

		/*
		 * Il ne faut pas appeler deux fois le destructeur
		 */
		assert nbInstances == 1 : "Double appel au destructor !";
	
		shutdown = true;

		Communication s = injector.getExistingService(Communication.class);
		assert s != null;
		
		if(!simuleComm)
			s.close();

		// On appelle le destructeur du PrintBuffer
		WindowFrame f = injector.getExistingService(WindowFrame.class);
		if(f != null)
			f.close();

		// arrêt des threads
		try {
			for(ThreadName n : ThreadName.values())
				if(injector.getService(n.c).isAlive())
					injector.getService(n.c).interrupt();
	
			for(ThreadName n : ThreadName.values())
			{
				try {
					log.write("Attente de "+n, Severity.INFO, Subject.DUMMY);
					injector.getService(n.c).join(1000); // on attend un peu que le thread
														// s'arrête
				}
				catch(InterruptedException e)
				{
					e.printStackTrace(log.getPrintWriter());
				}
			}
	
			Thread.sleep(100);
			for(ThreadName n : ThreadName.values())
				if(injector.getService(n.c).isAlive())
					log.write(n.c.getSimpleName() + " encore vivant !", Severity.CRITICAL, Subject.DUMMY);
	
			if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_DIFFERENTIAL))
				injector.getService(ThreadSaveVideo.class).interrupt();

			if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_EXTERNAL))
				injector.getService(ThreadPrintServer.class).interrupt();

			if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ENABLE))
				injector.getService(ThreadPrinting.class).interrupt();
			
			if(config.getBoolean(ConfigInfoSenpai.REMOTE_CONTROL))
				injector.getService(ThreadRemoteControl.class).interrupt();
			
			injector.getService(ThreadShutdown.class).interrupt();
		} catch(InjectorException e)
		{
			assert false : e;
		}
		nbInstances--;
		printMessage("outro.txt");

		Thread.sleep(300);
		return errorCode;
	}
	
	public synchronized <S> S getService(Class<S> service) throws InjectorException
	{
		return injector.getService(service);
	}

	public synchronized <S> S getExistingService(Class<S> classe)
	{
		return injector.getExistingService(classe);
	}
	
	/**
	 * Instancie le gestionnaire de dépendances et quelques services critiques
	 * (log et config qui sont interdépendants)
	 * 
	 * @throws ContainerException si un autre container est déjà instancié
	 * @throws InterruptedException
	 */
	public Senpai(String...profiles) throws InterruptedException
	{
		/**
		 * On vérifie qu'il y ait un seul container à la fois
		 */
		assert nbInstances == 0 : "Un autre container existe déjà! Annulation du constructeur.";

		nbInstances++;
		
		mainThread = Thread.currentThread();
		Thread.currentThread().setName("ThreadPrincipal");

		/**
		 * Affichage d'un petit message de bienvenue
		 */
		printMessage("intro.txt");

		injector = new Injector();

		DebugTool debug = new DebugTool(Severity.INFO);
		log = debug.getLog();
		config = new Config(ConfigInfoSenpai.values(), false, "senpai.conf", profiles);

		injector.addService(this);
		injector.addService(log);
		injector.addService(config);
		injector.addService(new Robot(log));		

		Speed.TEST.translationalSpeed = config.getDouble(ConfigInfoSenpai.VITESSE_ROBOT_TEST) / 1000.;
		Speed.REPLANIF.translationalSpeed = config.getDouble(ConfigInfoSenpai.VITESSE_ROBOT_REPLANIF) / 1000.;
		Speed.STANDARD.translationalSpeed = config.getDouble(ConfigInfoSenpai.VITESSE_ROBOT_STANDARD) / 1000.;

		/**
		 * Affiche la version du programme (dernier commit et sa branche)
		 */
		try
		{
			Process p = Runtime.getRuntime().exec("git log -1 --oneline");
			Process p2 = Runtime.getRuntime().exec("git branch");
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader in2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
			String s = in.readLine();
			int index = s.indexOf(" ");
			in.close();
			String s2 = in2.readLine();

			while(!s2.contains("*"))
				s2 = in2.readLine();

			int index2 = s2.indexOf(" ");
			log.write("Version : " + s.substring(0, index) + " on " + s2.substring(index2 + 1) + " - [" + s.substring(index + 1) + "]", Subject.DUMMY);
			in2.close();
		}
		catch(IOException e1)
		{
			System.out.println(e1);
		}

		/**
		 * Infos diverses
		 */
		log.write("Système : " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"), Subject.DUMMY);
		log.write("Java : " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + ", mémoire max : " + Math.round(100. * Runtime.getRuntime().maxMemory() / (1024. * 1024. * 1024.)) / 100. + "G, coeurs : " + Runtime.getRuntime().availableProcessors(), Subject.DUMMY);
		log.write("Date : " + new SimpleDateFormat("E dd/MM à HH:mm").format(new Date()), Subject.DUMMY);

		assert checkAssert();
		
		List<Obstacle> obstaclesFixes = new ArrayList<Obstacle>();
		for(ObstaclesFixes o : ObstaclesFixes.values())
			obstaclesFixes.add(o.obstacle);
		

		int demieLargeurNonDeploye = config.getInt(ConfigInfoSenpai.LARGEUR_NON_DEPLOYE) / 2;
		int demieLongueurArriere = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		int demieLongueurAvant = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);

		RectangularObstacle robotTemplate = new RectangularObstacle(demieLargeurNonDeploye, demieLargeurNonDeploye, demieLongueurArriere, demieLongueurAvant);
		injector.addService(RectangularObstacle.class, robotTemplate);
		
		Kraken k = new Kraken(robotTemplate, obstaclesFixes, new XY(-1500, 0), new XY(1500, 2000), profiles);
		
		injector.addService(k);
		
		/**
		 * Planification du hook de fermeture
		 */
		try
		{
			log.write("Mise en place du hook d'arrêt", Subject.DUMMY);
			Runtime.getRuntime().addShutdownHook(injector.getService(ThreadShutdown.class));
		}
		catch(InjectorException e)
		{
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
			assert false : e;
		}
		
		if(!config.getBoolean(ConfigInfoSenpai.GRAPHIC_ENABLE))
		{
			HashMap<ConfigInfo, Object> override = new HashMap<ConfigInfo, Object>();
			List<ConfigInfo> graphicConf = ConfigInfoSenpai.getGraphicConfigInfo();
			for(ConfigInfo c : graphicConf)
				override.put(c, false);
			config.override(override);
		}
		
		startAllThreads();
		
		/**
		 * L'initialisation est bloquante (on attend le LL), donc on le f ait le plus tardivement possible
		 */
		try {
			if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ENABLE))
			{
				WindowFrame f = debug.getWindowFrame(new Vec2RO(0,1000));
				injector.addService(WindowFrame.class, f);
				injector.addService(GraphicDisplay.class, f.getPrintBuffer());
			}
			
			if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_DIFFERENTIAL))
				debug.getThreadSaveVideo().start();

			if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_EXTERNAL))
				debug.getThreadComm().start();

			if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ENABLE))
				debug.getThreadPrinting().start();
			
			if(config.getBoolean(ConfigInfoSenpai.REMOTE_CONTROL))
				injector.getService(ThreadRemoteControl.class).start();

			simuleComm = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM); 
			if(!simuleComm)
			{
				injector.getService(Communication.class).initialize();
				injector.getService(OutgoingOrderBuffer.class).checkLatence();
			}
			else
				log.write("COMMUNICATION SIMULÉE !", Severity.CRITICAL, Subject.DUMMY);						
		} catch (InjectorException e) {
			assert false;
			e.printStackTrace();
		}
	}

	private boolean checkAssert()
	{
		log.write("Assertions vérifiées -- à ne pas utiliser en match !", Severity.CRITICAL, Subject.DUMMY);
		return true;
	}

	public void restartThread(ThreadName n) throws InterruptedException
	{
		try
		{
			Thread t = injector.getService(n.c);
			if(t.isAlive()) // s'il est encore en vie, on le tue
			{
				t.interrupt();
				t.join(1000);
			}
			injector.removeService(n.c);
			injector.getService(n.c).start(); // et on le redémarre
		}
		catch(InjectorException e)
		{
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
			assert false;
		}
	}

	/**
	 * Démarrage de tous les threads
	 */
	private void startAllThreads() throws InterruptedException
	{
		for(ThreadName n : ThreadName.values())
		{
			try
			{
				injector.getService(n.c).start();
			}
			catch(InjectorException | IllegalThreadStateException e)
			{
				log.write("Erreur lors de la création de thread " + n + " : " + e, Severity.CRITICAL, Subject.DUMMY);
				e.printStackTrace();
				e.printStackTrace(log.getPrintWriter());
			}
		}
	}

	/**
	 * Mise à jour de la config pour tous les services démarrés
	 * 
	 * @param s
	 * @return
	 */
/*	public void updateConfigForAll()
	{
		synchronized(dynaConf)
		{
			for(DynamicConfigurable s : dynaConf)
				s.updateConfig(config);
		}
	}*/

	/**
	 * Affichage d'un fichier
	 * 
	 * @param filename
	 */
	private void printMessage(String filename)
	{
		BufferedReader reader;
		try
		{
			reader = new BufferedReader(new FileReader(filename));
			String line;

			while((line = reader.readLine()) != null)
				System.out.println(line);
			reader.close();
		}
		catch(IOException e)
		{
			System.err.println(e); // peut-être que log n'est pas encore
									// démarré…
		}
	}

	public void interruptWithCodeError(ErrorCode code)
	{
		log.write("Demande d'interruption avec le code : "+code, Severity.WARNING, Subject.DUMMY);
		errorCode = code;
		mainThread.interrupt();
	}

}
