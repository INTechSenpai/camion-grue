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

package senpai.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import pfg.config.Config;
import pfg.graphic.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.Severity;
import senpai.Subject;

/**
 * Une connexion Ethernet
 * @author pf
 *
 */

public class Ethernet implements CommMedium
{
	private Log log;
	private int port;
	private InetAddress adresse;
	private Socket socket;

	private OutputStream output;
	private InputStream input;
	
	public Ethernet(Log log)
	{
		this.log = log;
	}
	
	/**
	 * Constructeur pour la série de test
	 * 
	 * @param log
	 */
	@Override
	public void initialize(Config config)
	{
		String hostname = config.getString(ConfigInfoSenpai.ETH_LL_HOSTNAME_SERVER);
		try
		{
			String[] s = hostname.split("\\.");
			// on découpe avec les points
			if(s.length == 4) // une adresse ip, probablement
			{
				byte[] addr = new byte[4];
				for(int j = 0; j < 4; j++)
				addr[j] = Byte.parseByte(s[j]);
				adresse = InetAddress.getByAddress(addr);
			}
			else // le nom du serveur, probablement
				adresse = InetAddress.getByName(hostname);
		}
		catch(UnknownHostException e)
		{
			assert false : e+" "+hostname;
			e.printStackTrace();
		}
		port = config.getInt(ConfigInfoSenpai.ETH_LL_PORT_NUMBER);
		assert port >= 0 && port < 655356 : "Port invalide";
	}

	@Override
	public boolean openIfClosed() throws InterruptedException
	{
		if(isClosed())
		{
			open(10);
			return true;
		}
		return false;
	}
	
	private boolean isClosed()
	{
		return socket == null || !socket.isConnected() || socket.isClosed();
	}
	
	/**
	 * Ouverture du port
	 * 
	 * @throws InterruptedException
	 */
	@Override
	public synchronized void open(int delayBetweenTries) throws InterruptedException
	{
		if(isClosed())
		{
			socket = null;
			do {
				try
				{
					socket = new Socket(adresse, port);
					socket.setTcpNoDelay(true);
					// on essaye de garder la connexion
					socket.setKeepAlive(true);
					// faible latence > temps de connexion court > haut débit
					socket.setPerformancePreferences(1, 2, 0);
					// reconnexion rapide
					socket.setReuseAddress(true);

					// open the streams
					input = socket.getInputStream();
					output = socket.getOutputStream();
				}
				catch(IOException e)
				{
					log.write("Erreur lors de la connexion au LL : "+e, Severity.WARNING, Subject.COMM);
					Thread.sleep(delayBetweenTries);
				}
			} while(socket == null);
		}
	}
	
	/**
	 * Doit être appelé quand on arrête de se servir de la communication
	 */
	@Override
	public synchronized void close()
	{
		assert socket.isConnected() && !socket.isClosed() : "État du socket : "+socket.isConnected()+" "+socket.isClosed();
		
		if(socket.isConnected() && !socket.isClosed())
		{
			try
			{
				log.write("Fermeture de la communication", Subject.COMM);
				socket.close();
				output.close();
			}
			catch(IOException e)
			{
				log.write(e, Severity.WARNING, Subject.COMM);
			}
		}
		else if(socket.isClosed())
			log.write("Fermeture impossible : carte déjà fermée", Severity.WARNING, Subject.COMM);
		else// if(!socket.isConnected())
			log.write("Fermeture impossible : carte jamais ouverte", Severity.WARNING, Subject.COMM);
	}

	@Override
	public OutputStream getOutput()
	{
		return output;
	}

	@Override
	public InputStream getInput()
	{
		return input;
	}

}
