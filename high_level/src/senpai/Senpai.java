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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import utils.*;
import exceptions.ContainerException;
import pfg.config.Config;
import pfg.graphic.DebugTool;
import pfg.injector.Injector;
import pfg.injector.InjectorException;
import pfg.log.Log;
import robot.Speed;
import scripts.Script;
import threads.ThreadName;
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
	private boolean showGraph;
	
	public boolean isShutdownInProgress()
	{
		return shutdown;
	}
	
	public enum ErrorCode
	{
		NO_ERROR(0),
		END_OF_MATCH(0),
		EMERGENCY_STOP(2),
		TERMINATION_SIGNAL(3),
		DOUBLE_DESTRUCTOR(4);
		
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
	public synchronized ErrorCode destructor() throws ContainerException, InterruptedException
	{
		if(Thread.currentThread().getId() != mainThread.getId())
			throw new ContainerException("Le destructor de container doit être appelé depuis le thread principal !");
	
		/*
		 * Il ne faut pas appeler deux fois le destructeur
		 */
		if(nbInstances == 0)
		{
			log.write("Double appel au destructor !", SeverityCategorySenpai.CRITICAL, LogCategorySenpai.DUMMY);
			return ErrorCode.DOUBLE_DESTRUCTOR;
		}

		shutdown = true;

		/**
		 * Mieux vaut écrire SerieCouchePhysique.class.getSimpleName()) que
		 * "SerieCouchePhysique",
		 * car en cas de refactor, le premier est automatiquement ajusté
		 */
		if(instanciedServices.containsKey(SerieCoucheTrame.class.getSimpleName()))
			((SerieCoucheTrame) instanciedServices.get(SerieCoucheTrame.class.getSimpleName())).close();

		// On appelle le destructeur du PrintBuffer
		if(instanciedServices.containsKey(PrintBufferInterface.class.getSimpleName()))
			((PrintBufferInterface) instanciedServices.get(PrintBufferInterface.class.getSimpleName())).destructor();

		// arrêt des threads
		for(ThreadName n : ThreadName.values())
			if(getService(n.c).isAlive())
				getService(n.c).interrupt();

		for(ThreadName n : ThreadName.values())
		{
			try {
				if(n == ThreadName.FENETRE && config.getBoolean(ConfigInfo.GRAPHIC_PRODUCE_GIF))
				{
					log.write("Attente de "+n, SeverityCategorySenpai.INFO, LogCategorySenpai.DUMMY);
					injector.getService(n.c).join(120000); // spécialement pour lui qui
													// enregistre un gif…
				}
				else
				{
					log.write("Attente de "+n, SeverityCategorySenpai.INFO, LogCategorySenpai.DUMMY);
					injector.getService(n.c).join(1000); // on attend un peu que le thread
													// s'arrête
				}
			}
			catch(InterruptedException e)
			{
				e.printStackTrace(log.getPrintWriter());
			}
		}

		Thread.sleep(100);
		for(ThreadName n : ThreadName.values())
			if(injector.getService(n.c).isAlive())
				log.write(n.c.getSimpleName() + " encore vivant !", SeverityCategorySenpai.CRITICAL, LogCategorySenpai.DUMMY);

		injector.getService(ThreadShutdown.class).interrupt();

		// fermeture du log
		log.write("Code d'erreur : " + errorCode, LogCategorySenpai.DUMMY);
		log.write("Fermeture du log", LogCategorySenpai.DUMMY);
		log.close();
		nbInstances--;
		printMessage("outro.txt");

		Thread.sleep(300);
		return errorCode;
	}

	/**
	 * Instancie le gestionnaire de dépendances et quelques services critiques
	 * (log et config qui sont interdépendants)
	 * 
	 * @throws ContainerException si un autre container est déjà instancié
	 * @throws InterruptedException
	 */
	public Senpai() throws ContainerException, InterruptedException
	{
		/**
		 * On vérifie qu'il y ait un seul container à la fois
		 */
		if(nbInstances != 0)
			throw new ContainerException("Un autre container existe déjà! Annulation du constructeur.");

		nbInstances++;

		mainThread = Thread.currentThread();
		Thread.currentThread().setName("ThreadPrincipal");

		/**
		 * Affichage d'un petit message de bienvenue
		 */
		printMessage("intro.txt");

		DebugTool debug = new DebugTool();
		log = debug.getLog(SeverityCategorySenpai.INFO);
		config = new ConfigSenpai(ConfigInfoSenpai.values(), "senpai.conf", false);

		injector.addService(Log.class, log);
		injector.addService(Config.class, config);

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
			log.write("Version : " + s.substring(0, index) + " on " + s2.substring(index2 + 1) + " - [" + s.substring(index + 1) + "]", LogCategorySenpai.DUMMY);
			in2.close();
		}
		catch(IOException e1)
		{
			System.out.println(e1);
		}

		/**
		 * Infos diverses
		 */
		log.write("Système : " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"), LogCategorySenpai.DUMMY);
		log.write("Java : " + System.getProperty("java.vendor") + " " + System.getProperty("java.version") + ", mémoire max : " + Math.round(100. * Runtime.getRuntime().maxMemory() / (1024. * 1024. * 1024.)) / 100. + "G, coeurs : " + Runtime.getRuntime().availableProcessors(), LogCategorySenpai.DUMMY);
		log.write("Date : " + new SimpleDateFormat("E dd/MM à HH:mm").format(new Date()), LogCategorySenpai.DUMMY);

		assert checkAssert();
		
		/**
		 * Planification du hook de fermeture
		 */
		try
		{
			log.write("Mise en place du hook d'arrêt", LogCategorySenpai.DUMMY);
			Runtime.getRuntime().addShutdownHook(injector.getService(ThreadShutdown.class));
		}
		catch(ContainerException e)
		{
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
		}
		
		Obstacle.set(log, getService(PrintBufferInterface.class));
		Obstacle.useConfig(config);
		ArcCourbe.useConfig(config);
		Script.setLogCercle(log, getService(CercleArrivee.class));
		
		startAllThreads();
	}

	private boolean checkAssert()
	{
		log.write("Assertions vérifiées -- à ne pas utiliser en match !", SeverityCategorySenpai.CRITICAL, LogCategorySenpai.DUMMY);
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
				log.write("Erreur lors de la création de thread " + n + " : " + e, SeverityCategorySenpai.CRITICAL, LogCategorySenpai.DUMMY);
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
	public void updateConfigForAll()
	{
		synchronized(dynaConf)
		{
			for(DynamicConfigurable s : dynaConf)
				s.updateConfig(config);
		}
	}

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
		log.write("Demande d'interruption avec le code : "+code, SeverityCategorySenpai.WARNING, LogCategorySenpai.DUMMY);
		errorCode = code;
		mainThread.interrupt();
	}

}
