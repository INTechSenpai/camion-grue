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

package threads;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import pfg.config.Config;
import pfg.graphic.ExternalPrintBuffer;
import pfg.log.Log;
import senpai.Subject;

/**
 * Thread du serveur d'affichage
 * 
 * @author pf
 *
 */

public class ThreadPrintServer extends Thread
{

	/**
	 * Thread qui envoie les données au socket donné en paramètre du
	 * constructeur
	 * 
	 * @author pf
	 *
	 */
	private class ThreadSocket implements Runnable
	{
		protected Log log;
		private ExternalPrintBuffer buffer;
		private Socket socket;
		private int nb;

		public ThreadSocket(Log log, ExternalPrintBuffer buffer, Socket socket, int nb)
		{
			this.log = log;
			this.buffer = buffer;
			this.socket = socket;
			this.nb = nb;
		}

		@Override
		public void run()
		{
			Thread.currentThread().setName(getClass().getSimpleName() + "-" + nb);
			log.write("Connexion d'un client au serveur d'affichage", Subject.DUMMY);
			try
			{
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				while(true)
				{
					buffer.send(out);
					Thread.sleep(200); // on met à jour toutes les 200ms
				}
			}
			catch(InterruptedException | IOException e)
			{
				log.write("Arrêt de " + Thread.currentThread().getName(), Subject.DUMMY);
				Thread.currentThread().interrupt();
			}
		}

	}

	private class ThreadDifferential implements Runnable
	{
		protected Log log;
		private ExternalPrintBuffer buffer;

		public ThreadDifferential(Log log, ExternalPrintBuffer buffer)
		{
			this.log = log;
			this.buffer = buffer;
		}

		@Override
		public void run()
		{
			Thread.currentThread().setName(getClass().getSimpleName());
			log.write("Démarrage de " + Thread.currentThread().getName(), Subject.DUMMY);
			try
			{
				while(true)
				{
					Thread.sleep(100);
					synchronized(buffer)
					{
						buffer.wait(400);
						buffer.write();
					}
				}
			}
			catch(InterruptedException | IOException e)
			{
				log.write("Arrêt de " + Thread.currentThread().getName(), Subject.DUMMY);
				Thread.currentThread().interrupt();
			}
		}

	}

	private boolean print, deporte, file;
	protected Log log;
	private ExternalPrintBuffer buffer;
	private int nbConnexions = 0;
	private ServerSocket ssocket = null;
	private List<Thread> threads = new ArrayList<Thread>();

	public ThreadPrintServer(Log log, ExternalPrintBuffer buffer, Config config)
	{
		this.log = log;
		this.buffer = buffer;
/*		print = config.getBoolean(ConfigInfo.GRAPHIC_ENABLE);
		deporte = config.getBoolean(ConfigInfo.GRAPHIC_EXTERNAL);
		file = config.getBoolean(ConfigInfo.GRAPHIC_DIFFERENTIAL);
		try {
			Runtime.getRuntime().exec("rm videos/last.dat");
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(file && print)
		{
			Thread t = new Thread(new ThreadDifferential(log, buffer));
			t.start();
			threads.add(t);
		}*/
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.DUMMY);
		try
		{
/*			if(!print || !deporte)
			{
				log.debug(getClass().getSimpleName() + " annulé (" + ConfigInfo.GRAPHIC_ENABLE + " = " + print + ", " + ConfigInfo.GRAPHIC_EXTERNAL + " = " + deporte + ")");
				while(true)
					Thread.sleep(10000);
			}*/

			ssocket = new ServerSocket(13370);
			while(true)
			{
				try
				{
					Thread t = new Thread(new ThreadSocket(log, buffer, ssocket.accept(), nbConnexions++));
					t.start();
					threads.add(t);
				}
				catch(SocketTimeoutException e)
				{}
			}
		}
		catch(IOException e)
		{
			if(ssocket != null && !ssocket.isClosed())
				try
				{
					ssocket.close();
				}
				catch(IOException e1)
				{
					e1.printStackTrace();
					e1.printStackTrace(log.getPrintWriter());
				}

			/*
			 * On arrête tous les threads de socket en cours
			 */
			for(Thread t : threads)
				t.interrupt();
			log.write("Arrêt de " + Thread.currentThread().getName(), Subject.DUMMY);
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Surcharge d'interrupt car accept() y est insensible
	 */
	@Override
	public void interrupt()
	{
		try
		{
			if(ssocket != null && !ssocket.isClosed())
				ssocket.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
		}
		super.interrupt();
	}

}
