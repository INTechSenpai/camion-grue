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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import pfg.config.Config;
import pfg.config.ConfigInfo;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.DebugTool;
import pfg.injector.Injector;
import pfg.injector.InjectorException;
import pfg.kraken.Kraken;
import pfg.kraken.astar.DefaultCheminPathfinding;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.utils.XY;
import pfg.log.Log;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.Communication;
import senpai.obstacles.ObstaclesDynamiques;
import senpai.obstacles.ObstaclesFixes;
import senpai.robot.Robot;
import senpai.robot.Speed;
import senpai.threads.ThreadName;
import senpai.threads.ThreadWarmUp;

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
	private DebugTool debug;

	private volatile static int nbInstances = 0;
	private Thread mainThread;
	private volatile ErrorCode errorCode = ErrorCode.NO_ERROR;
	private volatile boolean shutdown = false;
	private boolean simuleComm;
	private boolean threadsDemarres = false;
	
	public enum ErrorCode
	{
		NO_ERROR(true),
		END_OF_MATCH(true),
		EXCEPTION(false),
		SIGTERM(false);
		
		public final boolean normal;
		public Exception e = null;
		
		private ErrorCode(boolean normal)
		{
			this.normal = normal;
		}
		
		public void setException(Exception e)
		{
			this.e = e;
		}
		
		@Override
		public String toString()
		{
			if(e == null)
				return name();
			return name()+" : "+e.getMessage();
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
	public synchronized void destructor(ErrorCode error) throws InterruptedException
	{
		if(errorCode == ErrorCode.NO_ERROR)
			this.errorCode = error;
		
		assert Thread.currentThread().getId() == mainThread.getId() : "Appel au destructeur depuis un thread !";
		
		log.write("Arrêt : "+errorCode, errorCode.normal ? Severity.INFO : Severity.CRITICAL, Subject.STATUS);
		
		if(errorCode.e != null)
			errorCode.e.printStackTrace();
		
		/*
		 * Il ne faut pas appeler deux fois le destructeur
		 */
		assert nbInstances == 1 : "Double appel au destructor !";
	
		shutdown = true;

		Communication s = injector.getExistingService(Communication.class);
		
		if(s != null && !simuleComm)
			s.close();
		
		// On appelle le destructeur graphique
		debug.destructor();
		
		// arrêt des threads
		if(threadsDemarres)
			for(ThreadName n : ThreadName.values())
			{
				Thread t = getService(n.c);
				if(t.isAlive())
				{
					t.interrupt();
					t.join(1000);
					if(t.isAlive())
						log.write(n.c.getSimpleName() + " encore vivant !", Severity.CRITICAL, Subject.STATUS);
				}
				else
					log.write(n.c.getSimpleName() + " déjà mort !", Severity.CRITICAL, Subject.STATUS);
			}

		nbInstances--;
		printMessage("outro.txt");

		// en cas d'erreur, la led clignote
		if(!errorCode.normal)
			GPIO.clignoteDiode(5);
	}
	
	public synchronized <S> S getService(Class<S> service)
	{		
		try
		{
			return injector.getService(service);
		}
		catch(InjectorException e)
		{
			e.printStackTrace();
			return null;
		}
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
	public void initialize(String configfile, String...profiles) throws InterruptedException
	{
		/**
		 * On vérifie qu'il y ait un seul container à la fois
		 */
		assert nbInstances == 0 : "Un autre \"Senpai\" existe déjà! Annulation du constructeur.";

		nbInstances++;
		
		mainThread = Thread.currentThread();
		Thread.currentThread().setName("ThreadPrincipal");
		
		/**
		 * Affichage d'un petit message de bienvenue
		 */
		printMessage("intro.txt");

		injector = new Injector();
		
		
		log = new Log(Severity.INFO, configfile, profiles);
		config = new Config(ConfigInfoSenpai.values(), false, configfile, profiles);
		
		Subject.STATUS.setShouldPrint(config.getBoolean(ConfigInfoSenpai.PRINT_STATUS));
		Subject.CAPTEURS.setShouldPrint(config.getBoolean(ConfigInfoSenpai.PRINT_CAPTEURS));
		Subject.CORRECTION.setShouldPrint(config.getBoolean(ConfigInfoSenpai.PRINT_CORRECTION));
		Subject.COMM.setShouldPrint(config.getBoolean(ConfigInfoSenpai.PRINT_COMM));
		Subject.TRAJECTORY.setShouldPrint(config.getBoolean(ConfigInfoSenpai.PRINT_TRAJECTORY));
		Subject.SCRIPT.setShouldPrint(config.getBoolean(ConfigInfoSenpai.PRINT_SCRIPT));
		
		injector.addService(this);
		injector.addService(log);
		injector.addService(config);
		
		/**
		 * Planification du hook de fermeture (le plus tôt possible)
		 */
		log.write("Mise en place du hook d'arrêt", Subject.STATUS);
		Runtime.getRuntime().addShutdownHook(new ThreadShutdown(this));

		Cinematique positionRobot = new Cinematique(0, 0, 0, true, 0, false);
		
		debug = DebugTool.getDebugTool(new HashMap<ConfigInfo, Object>(), new XY(0,1000), positionRobot.getPosition(), Severity.INFO, configfile, profiles);
		injector.addService(GraphicDisplay.class, debug.getGraphicDisplay());

		Robot robot = getService(Robot.class);
		robot.initPositionObject(positionRobot);
		
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
			log.write("Version : " + s.substring(0, index) + " on " + s2.substring(index2 + 1) + " - [" + s.substring(index + 1) + "]", Subject.STATUS);
			in2.close();
		}
		catch(IOException e1)
		{}

		/**
		 * Infos diverses
		 */
		log.write("Système : " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"), Subject.STATUS);
		log.write("Java : " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + ", mémoire max : " + Math.round(Runtime.getRuntime().maxMemory() / (1024. * 1024.)) + "M, coeurs : " + Runtime.getRuntime().availableProcessors(), Subject.STATUS);
		log.write("Date : " + new SimpleDateFormat("E dd/MM à HH:mm").format(new Date()), Subject.STATUS);

		assert checkAssert();
		
		List<Obstacle> obstaclesFixes = new ArrayList<Obstacle>();
		for(ObstaclesFixes o : ObstaclesFixes.values())
			obstaclesFixes.add(o.obstacle);
		ObstaclesDynamiques obsDyn = getService(ObstaclesDynamiques.class);

		int marge = config.getInt(ConfigInfoSenpai.MARGE_PATHFINDING);
		int demieLargeurNonDeploye = config.getInt(ConfigInfoSenpai.LARGEUR_NON_DEPLOYE) / 2 + marge;
		int demieLongueurArriere = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_ARRIERE);
		int demieLongueurAvant = config.getInt(ConfigInfoSenpai.DEMI_LONGUEUR_NON_DEPLOYE_AVANT);

		RectangularObstacle robotTemplate = new RectangularObstacle(demieLongueurAvant, demieLongueurArriere, demieLargeurNonDeploye, demieLargeurNonDeploye);
		injector.addService(RectangularObstacle.class, robotTemplate);
		
		if(Thread.currentThread().isInterrupted())
			throw new InterruptedException();
		
		/*
		 * Warm-up without verbose
		 */
		ThreadWarmUp warmUp = new ThreadWarmUp(log, new Kraken(robotTemplate, obstaclesFixes, new XY(-1500, 0), new XY(1500, 2000), "warmup.conf", "default"), config);
		warmUp.start();

		Kraken k = new Kraken(robotTemplate, obstaclesFixes, obsDyn, new XY(-1500, 0), new XY(1500, 2000), configfile, profiles);
		injector.addService(k);

		try {
			Method m = Kraken.class.getDeclaredMethod("getInjector");
			m.setAccessible(true);
			Injector injectorKraken = (Injector) m.invoke(k);
			injector.addService(injectorKraken.getExistingService(DefaultCheminPathfinding.class));
		} catch(NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			assert false;
		}

		if(Thread.currentThread().isInterrupted())
			throw new InterruptedException();

		System.out.println("Configuration pour eurobotruck");
		config.printChangedValues();
		System.out.println("Configuration pour Kraken");
		k.displayOverriddenConfigValues();
		System.out.println("Configuration pour l'interface graphique");
		debug.displayOverriddenConfigValues();
		
		startAllThreads();
		
		if(config.getBoolean(ConfigInfoSenpai.SAVE_VIDEO))
			debug.startSaveVideo();

		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_EXTERNAL))
			debug.startPrintServer();

		simuleComm = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM); 
		if(!simuleComm)
		{
			/**
			 * L'initialisation est bloquante (on attend le LL), donc on le fait le plus tardivement possible
			 */		
			getService(Communication.class).initialize();
			
			OutgoingOrderBuffer outBuffer = getService(OutgoingOrderBuffer.class);
			log.write("On attend la réponse du LL…", Subject.COMM);
			boolean response;
			do {
				response = outBuffer.ping().attendStatus(500) != null;
			} while(!response);

			if(config.getBoolean(ConfigInfoSenpai.CHECK_LATENCY))
				getService(OutgoingOrderBuffer.class).checkLatence();
		}
		else
			log.write("COMMUNICATION SIMULÉE !", Severity.CRITICAL, Subject.STATUS);
	}

	private boolean checkAssert()
	{
		log.write("Assertions vérifiées -- à ne pas utiliser en match !", Severity.CRITICAL, Subject.STATUS);
		return true;
	}

	public void restartThread(ThreadName n) throws InterruptedException
	{
		Thread t = getService(n.c);
		if(t.isAlive()) // s'il est encore en vie, on le tue
		{
			t.interrupt();
			t.join(1000);
		}
		injector.removeService(n.c);
		getService(n.c).start(); // et on le redémarre
	}

	/**
	 * Démarrage de tous les threads
	 */
	private void startAllThreads()
	{
		for(ThreadName n : ThreadName.values())
		{
			try
			{
				getService(n.c).start();
			}
			catch(IllegalThreadStateException e)
			{
				log.write("Erreur lors de la création de thread " + n + " : " + e, Severity.CRITICAL, Subject.STATUS);
				e.printStackTrace();
			}
		}
		threadsDemarres = true;
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
			InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
			if(is != null)
			{
				reader = new BufferedReader(new InputStreamReader(is));
				String line;
	
				while((line = reader.readLine()) != null)
					System.out.println(line);
				reader.close();
			}
		}
		catch(IOException e)
		{
			System.err.println(e); // peut-être que log n'est pas encore
									// démarré…
		}
	}

	public void interruptWithCodeError(ErrorCode code) throws InterruptedException
	{
		errorCode = code;
		mainThread.interrupt();
	}
	
	private class ThreadShutdown extends Thread
	{
		protected Senpai container;

		public ThreadShutdown(Senpai container)
		{
			this.container = container;
			setDaemon(true);
		}

		@Override
		public void run()
		{
			try {
				Thread.currentThread().setName(getClass().getSimpleName());
				// c'est le thread principal qui va terminer ce thread
				if(!shutdown)
				{
					container.interruptWithCodeError(ErrorCode.SIGTERM);
					mainThread.join(); // on attend que le thread principal se termine, car dès que le shutdown hook s'arrête, tout s'arrête !
				}				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
