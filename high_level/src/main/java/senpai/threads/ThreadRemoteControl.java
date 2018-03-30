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

package senpai.threads;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import pfg.config.Config;
import pfg.log.Log;
import senpai.Subject;
import senpai.ConfigInfoSenpai;
import senpai.Senpai;
import senpai.Senpai.ErrorCode;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.Ticket;
import senpai.Severity;

/**
 * Thread du contrôle à distance
 * 
 * @author pf
 *
 */

public class ThreadRemoteControl extends Thread
{
	private Log log;
	private OutgoingOrderBuffer data;
	private Senpai container;
	private ServerSocket ssocket = null;
	private double l;
	private int port;

	public enum Commandes implements Serializable
	{
		SPEED_UP, // 0
		SPEED_DOWN, // 1
		SET_SPEED, // 2
		TURN_RIGHT, // 3
		TURN_LEFT, // 4
		SET_DIRECTION, // 5
		STOP, // 6
		RESET_WHEELS, // 7
		SHUTDOWN, // 8
		PING, // 9
		LEVE_FILET, // 10
		BAISSE_FILET, // 11
		FERME_FILET, // 12
		OUVRE_FILET, // 13
		EJECTE_GAUCHE, // 14
		EJECTE_DROITE, // 15
		REARME_GAUCHE, // 16
		REARME_DROITE; // 17
	}
	
	private class CommandesParam
	{
		public Commandes com;
		public int param;
		
		public CommandesParam(Commandes com, int param)
		{
			this.com = com;
			if(param > 127)
				param -= 256;
			this.param = param;
		}		
	}
	
	/**
	 * Étant donné que l'écoute est bloquante, il faut un thread pour écouter et un thread pour exécuter
	 * @author pf
	 *
	 */
	private class ThreadListen extends Thread
	{
		protected Log log;
		private InputStream in;
		private Queue<CommandesParam> buffer = new ConcurrentLinkedQueue<CommandesParam>();
		
		public ThreadListen(Log log, InputStream in)
		{
			this.log = log;
			this.in = in;
			setDaemon(true);
		}
		
		private boolean isEmpty()
		{
			return buffer.isEmpty();
		}
		
		private CommandesParam poll()
		{
			return buffer.poll();
		}

		@Override
		public void run()
		{
			Thread.currentThread().setName(getClass().getSimpleName());
			log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);
			try
			{
				while(true)
				{
					int val;
					while((val = in.read()) == -1)
						Thread.sleep(0, 1000);
					
					Commandes com = Commandes.values()[val];

					int param;
					while((param = in.read()) == -1)
						Thread.sleep(0, 1000);
					
					buffer.add(new CommandesParam(com, param));
				}
			}
			catch(IOException | InterruptedException e)
			{
				log.write("Arrêt de " + Thread.currentThread().getName(), Subject.STATUS);
				Thread.currentThread().interrupt();
			}
		}

	}
	
	public ThreadRemoteControl(Log log, Config config, OutgoingOrderBuffer data, Senpai container)
	{
		this.log = log;
		this.data = data;
		this.container = container;
		l = config.getDouble(ConfigInfoSenpai.CENTRE_ROTATION_ROUE_X) / 1000.;
		port = config.getInt(ConfigInfoSenpai.REMOTE_CONTROL_PORT_NUMBER);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.STATUS);
		try
		{
			ssocket = new ServerSocket(port);
			control(ssocket.accept());
		}
		catch(InterruptedException | IOException | ClassNotFoundException e)
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
			log.write("Arrêt de " + Thread.currentThread().getName(), Subject.STATUS);
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

	private void control(Socket socket) throws IOException, ClassNotFoundException, InterruptedException
	{
		log.write("Connexion d'un client !", Subject.REMOTE_CONTROL);
		short vitesse = 0;
		double courbure = 0;
		Ticket run = null;
		double angleRoues = 0;
		InputStream in = socket.getInputStream();
		int autostopseuil = 100;
		int autostopcompteur = 0;
		
		ThreadListen t = new ThreadListen(log, in);
		t.start();

		// TODO refaire proprement

		while(true)
		{
			CommandesParam cp = null;
			
			if(!t.isEmpty())
			{
				cp = t.poll();
				autostopcompteur = 0;
			}
			else
			{
				autostopcompteur++;
				if(autostopcompteur == autostopseuil)
				{
					cp = new CommandesParam(Commandes.STOP, 0);
					log.write("Timeout télécommande !", Severity.CRITICAL, Subject.REMOTE_CONTROL);
				}
				Thread.sleep(5);
			}
			

			Commandes c = null;
			
			if(cp != null)
			{
				log.write("On exécute : "+cp.com+", param = "+cp.param, Subject.REMOTE_CONTROL);
				c = cp.com;
			}
			
			if(run != null/* && !run.isEmpty()*/) // le robot s'est arrêté
			{
				vitesse = 0;
				run = null;
			}

			if(c == null)
				continue;
			else if(c == Commandes.PING);
			else if(c == Commandes.SET_SPEED)
			{
				short nextVitesse = (short) (cp.param * 10);
				if(vitesse != nextVitesse)
				{
					vitesse = nextVitesse;
					data.setMaxSpeed(vitesse);					
				}
				
				if(vitesse != 0 && run == null)
					run = data.run();
			}
			else if(c == Commandes.STOP)
			{
				if(run != null)
				{
					run = null;
					vitesse = 0;
					data.immobilise();
					data.waitStop();
				}
			}
			else if(c == Commandes.RESET_WHEELS)
			{
				angleRoues = 0;
				data.setCurvature(0);
			}
			else if(c == Commandes.SET_DIRECTION)
			{
				double nextAnglesRoues = cp.param * Math.PI / 180.;
				if(angleRoues != nextAnglesRoues)
				{
					angleRoues = nextAnglesRoues;
					courbure = Math.tan(angleRoues) / l;
					data.setCurvature(courbure);
				}
			}
			
			else if(c == Commandes.SHUTDOWN)
			{
				if(run != null)
				{
					run = null;
					vitesse = 0;
					data.immobilise();
					data.waitStop();
				}
				container.interruptWithCodeError(ErrorCode.EMERGENCY_STOP);
				break;
			}
		}
		Thread.currentThread().interrupt();
	}

}
