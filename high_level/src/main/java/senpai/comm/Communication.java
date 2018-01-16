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
import senpai.comm.CommProtocol.Id;
import senpai.exceptions.UnexpectedClosedCommException;

/**
 * La connexion série
 * 
 * @author pf
 *
 */

public class Communication
{
	protected Log log;
	
	private int port;
	private InetAddress adresse;
	private Socket socket;

	private OutputStream output;
	private InputStream input;

	private volatile boolean initialized = false;
	
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
		try {
			openSocket(500);
			
			// open the streams
			input = socket.getInputStream();
			output = socket.getOutputStream();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			assert false : e;
		}
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
	 * Doit être appelé quand on arrête de se servir de la série
	 */
	public synchronized void close()
	{
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
	 */
	public void communiquer(Order o) throws UnexpectedClosedCommException
	{
		if(isClosed())
			throw new UnexpectedClosedCommException("La communication a été arrêtée");
		
		try
		{
			output.write(o.trame, 0, o.tailleTrame);
			output.flush();
		}
		catch(IOException e)
		{
			throw new UnexpectedClosedCommException("Connexion perdue ! "+e);
			/*
			 * Le code ci-dessous a été retiré car, de toute façon, il ne gère pas la récupération de la lecture
			 */
			
			/*
			 * Si la carte ne répond vraiment pas, on recommence de manière
			 * infinie.
			 * De toute façon, on n'a pas d'autre choix...
			 */
/*			log.write("Ne peut pas parler à la carte. Tentative de reconnexion.", Severity.WARNING, Subject.COMM);
			openSocket(50);
			// On a retrouvé la série, on renvoie le message
			communiquer(o);*/
		}
	}

	public Paquet readPaquet() throws InterruptedException, UnexpectedClosedCommException
	{
		if(isClosed())
			throw new UnexpectedClosedCommException("La communication a été arrêtée");

		try {
			int k = input.read();

			if(k == -1)
				throw new UnexpectedClosedCommException("EOF de l'input de communication");
			
			assert k == 0xFF : "Mauvais entête de paquet : "+k;
			
			int origineInt = input.read();
			Id origine = Id.LUT[origineInt];
			assert origine != null : "ID inconnu ! "+origineInt;
			origine.answerReceived();
			
			int taille = input.read();
			assert taille >= 0 && taille <= 254 : "Le message reçu a un mauvais champ \"length\" : "+taille;
			int[] message = new int[taille];
			for(int i = 0; i < taille; i++)
				message[i] = input.read();
			
			return new Paquet(message, origine);
		} catch (IOException e) {
			if(isClosed()) // arrêt volontaire
				throw new UnexpectedClosedCommException("La communication a été arrêtée");
			throw new UnexpectedClosedCommException(e.getMessage());
		}
	}
}
