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
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import config.Config;
import config.ConfigInfo;
import container.Container;
import container.Container.ErrorCode;
import container.dependances.GUIClass;
import remoteControl.Commandes;
import serie.BufferOutgoingOrder;
import serie.Ticket;
import utils.Log;

/**
 * Thread du contrôle à distance
 * 
 * @author pf
 *
 */

public class ThreadRemoteControl extends ThreadService implements GUIClass
{
	private Log log;
	private Config config;
	private BufferOutgoingOrder data;
	private Container container;
	private ServerSocket ssocket = null;
	private boolean remote;
	private double l;

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
	
	private class ThreadListen extends Thread
	{
		protected Log log;
		private InputStream in;
		private Queue<CommandesParam> buffer = new ConcurrentLinkedQueue<CommandesParam>();
		
		public ThreadListen(Log log, InputStream in)
		{
			this.log = log;
			this.in = in;
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
			log.debug("Démarrage de " + Thread.currentThread().getName());
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
				log.debug("Arrêt de " + Thread.currentThread().getName());
				Thread.currentThread().interrupt();
			}
		}

	}

	
	public ThreadRemoteControl(Log log, Config config, BufferOutgoingOrder data, Container container)
	{
		this.log = log;
		this.config = config;
		this.data = data;
		this.container = container;
		remote = config.getBoolean(ConfigInfo.REMOTE_CONTROL);
		l = config.getDouble(ConfigInfo.CENTRE_ROTATION_ROUE_X) / 1000.;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.debug("Démarrage de " + Thread.currentThread().getName());
		try
		{
			if(!remote)
			{
				log.debug(getClass().getSimpleName() + " annulé");
				while(true)
					Thread.sleep(10000);
			}

			ssocket = new ServerSocket(13371);
			control(ssocket.accept(), config);
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
			log.debug("Arrêt de " + Thread.currentThread().getName()+" : "+e);
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

	private void control(Socket socket, Config config) throws IOException, ClassNotFoundException, InterruptedException
	{
		log.debug("Connexion d'un client !");
		short vitesse = 0;
		short vitesseMax = 1000;
		short pasVitesse = config.getShort(ConfigInfo.DELTA_VITESSE);
		double pasAngle = config.getDouble(ConfigInfo.DELTA_ANGLE_ROUES);
		double courbure = 0;
		Ticket run = null;
		double angleRoues = 0;
		double courbureMax = 5;
		InputStream in = socket.getInputStream();
		int autostopseuil = 100;
		int autostopcompteur = 0;
		
		ThreadListen t = new ThreadListen(log, in);
		t.start();

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
					log.critical("Timeout télécommande !");
				}
				Thread.sleep(5);
			}
			

			Commandes c = null;
			
			if(cp != null)
			{
				log.debug("On exécute : "+cp.com+", param = "+cp.param);
				c = cp.com;
			}
			
			if(run != null && !run.isEmpty()) // le robot s'est arrêté
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
			else if(c == Commandes.SPEED_UP || c == Commandes.SPEED_DOWN)
			{
				short nextVitesse;
				if(c == Commandes.SPEED_UP)
				{
					nextVitesse = (short) (vitesse + pasVitesse);
					if(vitesse < 0)
						nextVitesse = (short) (vitesse + pasVitesse);
				}
				else
				{
					nextVitesse = (short) (vitesse - pasVitesse);
					if(vitesse > 0)
						nextVitesse = (short) (vitesse - pasVitesse);
				}
					
				if(nextVitesse <= vitesseMax && nextVitesse >= -vitesseMax)
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
			else if(c == Commandes.TURN_LEFT || c == Commandes.TURN_RIGHT)
			{
				double prochainAngleRoues;
				double pasAngleTmp = (pasAngle / 2. - 2. * pasAngle) * Math.abs(vitesse) / 1000. + 2 * pasAngle;
				
				if(c == Commandes.TURN_LEFT)		
					prochainAngleRoues = angleRoues + pasAngleTmp;
				else
					prochainAngleRoues = angleRoues - pasAngleTmp;
				
				double nextCourbure = Math.tan(prochainAngleRoues) / l;
			
				if(nextCourbure <= courbureMax && nextCourbure >= -courbureMax)
				{
					angleRoues = prochainAngleRoues;
					courbure = nextCourbure;
					data.setCurvature(courbure);
				}
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
			else if(c == Commandes.BAISSE_FILET)
				data.baisseFilet();
			else if(c == Commandes.LEVE_FILET)
				data.leveFilet();
			else if(c == Commandes.OUVRE_FILET)
				data.ouvreFilet();
			else if(c == Commandes.FERME_FILET)
				data.fermeFilet();
			else if(c == Commandes.EJECTE_DROITE)
				data.ejecteBalles(true);
			else if(c == Commandes.EJECTE_GAUCHE)
				data.ejecteBalles(false);
			else if(c == Commandes.REARME_GAUCHE)
				data.rearme(false);
			else if(c == Commandes.REARME_DROITE)
				data.rearme(true);
			
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
