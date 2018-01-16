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

import java.io.Closeable;
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
import senpai.comm.CommProtocol.Id;

/**
 * La connexion série
 * 
 * @author pf
 *
 */

public class Communication implements Closeable
{
	protected Log log;
	
	private int port;
	private InetAddress adresse;
	private Socket socket;

	private OutputStream output;
	private InputStream input;

	private volatile boolean initialized = false;
	private volatile boolean closed = false; // fermeture normale
	
	/**
	 * Constructeur pour la série de test
	 * 
	 * @param log
	 */
	public Communication(Log log, Config config)
	{
		this.log = log;
		String hostname = config.getString(ConfigInfoSenpai.LL_HOSTNAME_SERVER);
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
		port = config.getInt(ConfigInfoSenpai.LL_PORT_NUMBER);
		assert port >= 0 && port < 655356 : "Port invalide";
	}

	private void openIfClosed() throws InterruptedException
	{
		if(isClosed())
			openSocket(10);
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
	private synchronized void openSocket(int delayBetweenTries) throws InterruptedException
	{
		assert !closed;
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
	 * Il donne à la communication tout ce qu'il faut pour fonctionner
	 * @throws InterruptedException 
	 */
	public synchronized void initialize() throws InterruptedException
	{
		openSocket(500);
		initialized = true;
		notifyAll();
	}

	public synchronized void waitForInitialization() throws InterruptedException
	{
		if(!initialized)
			wait();
		assert initialized;
	}
	
	/**
	 * Doit être appelé quand on arrête de se servir de la communication
	 */
	public synchronized void close()
	{
		assert !closed : "Seconde demande de fermeture !";
		closed = true;
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

	/**
	 * Envoie une frame sur la série
	 * Cette méthode est synchronized car deux threads peuvent l'utiliser :
	 * ThreadSerialOutput et ThreadSerialOutputTimeout
	 * 
	 * @param message
	 * @throws UnexpectedClosedCommException
	 * @throws InterruptedException 
	 */
	public void communiquer(Order o) throws InterruptedException
	{
		// série fermée normalement
		if(closed)
			return;
		
		boolean error;
		do {
			error = false;
			openIfClosed();
			try
			{
				output.write(o.trame, 0, o.tailleTrame);
				output.flush();
			}
			catch(IOException e)
			{
				e.printStackTrace();
				error = true;
			}
		} while(error);
	}

	public Paquet readPaquet() throws InterruptedException
	{
		// série fermée définitivement
		if(closed)
			while (true)
			    Thread.sleep(Long.MAX_VALUE);
			
		while(true)
		{
			openIfClosed();
	
			try {
				int k = input.read();
				if(k == -1)
					throw new IOException("EOF de l'input de communication");
				
				assert k == 0xFF : "Mauvais entête de paquet : "+k;
				
				int origineInt = input.read();
				if(origineInt == -1)
					throw new IOException("EOF de l'input de communication");

				Id origine = Id.LUT[origineInt];
				assert origine != null : "ID inconnu ! "+origineInt;
				origine.answerReceived();
				
				int taille = input.read();
				if(taille == -1)
					throw new IOException("EOF de l'input de communication");
				
				assert taille >= 0 && taille <= 254 : "Le message reçu a un mauvais champ \"length\" : "+taille;
				int[] message = new int[taille];
				for(int i = 0; i < taille; i++)
				{
					message[i] = input.read();
					if(message[i] == -1)
						throw new IOException("EOF de l'input de communication");
				}
				
				return new Paquet(message, origine);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
