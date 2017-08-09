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

package comm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import comm.CommProtocol.Id;
import exceptions.ClosedSerialException;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.ConfigSenpai;
import senpai.Severity;
import senpai.Subject;

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

	private boolean simuleSerie;
	private boolean mustClose = false;

	private OutputStream output;
	private DataInputStream input;

	/**
	 * Constructeur pour la série de test
	 * 
	 * @param log
	 */
	public Communication(Log log, ConfigSenpai config)
	{
		this.log = log;

		adresse = config.getAdresse();
		port = config.getInt(ConfigInfoSenpai.LL_PORT_NUMBER);
		simuleSerie = config.getBoolean(ConfigInfoSenpai.SIMULE_SERIE);

		if(simuleSerie)
			log.write("SÉRIE SIMULÉE !", Severity.CRITICAL, Subject.DUMMY);
		
		initialize();
	}

	/**
	 * Ouverture du port
	 * 
	 * @throws InterruptedException
	 * @throws ClosedSerialException 
	 */
	protected synchronized void openSocket(int delay) throws InterruptedException, ClosedSerialException
	{
		if(mustClose)
			throw new ClosedSerialException("La série est fermée et ne peut envoyer un message");
		
		if(socket == null || !socket.isConnected() || socket.isClosed())
		{
			socket = null;
			do {
				try
				{
					socket = new Socket(adresse, port);
				}
				catch(IOException e)
				{
					log.write("Erreur lors de la connexion au LL", Severity.WARNING, Subject.COMM);
					Thread.sleep(delay);
				}
			} while(socket == null);
		}
	}

	/**
	 * Il donne à la série tout ce qu'il faut pour fonctionner
	 * 
	 * @param port_name
	 * Le port où est connecté la carte
	 * @param baudrate
	 * Le baudrate que la carte utilise
	 */
	private boolean initialize()
	{
		if(simuleSerie)
			return true;

		try {
			openSocket(500);
			
			// open the streams
			input = new DataInputStream(socket.getInputStream());
			output = socket.getOutputStream();
	
			return true;
		}
		catch(IOException | InterruptedException | ClosedSerialException e)
		{
			e.printStackTrace();
			assert false : e;
			return false;
		}		
	}

	/**
	 * Doit être appelé quand on arrête de se servir de la série
	 */
	public synchronized void close()
	{
		if(simuleSerie)
			return;

		if(socket.isConnected() && !socket.isClosed())
		{
			try
			{
				log.write("Fermeture de la carte", Subject.COMM);
				socket.close();
				output.close();
				mustClose = false;
			}
			catch(IOException e)
			{
				log.write(e, Severity.WARNING, Subject.COMM);
			}
		}
		else if(socket.isClosed())
			log.write("Carte déjà fermée", Severity.WARNING, Subject.COMM);
		else// if(!socket.isConnected())
			log.write("Carte jamais ouverte", Severity.WARNING, Subject.COMM);
	}

	/**
	 * Envoie une frame sur la série
	 * Cette méthode est synchronized car deux threads peuvent l'utiliser :
	 * ThreadSerialOutput et ThreadSerialOutputTimeout
	 * 
	 * @param message
	 * @throws InterruptedException
	 */
	public void communiquer(Order o) throws InterruptedException, ClosedSerialException
	{
		if(simuleSerie)
			return;

		/**
		 * Un appel à une série fermée ne devrait jamais être effectué.
		 */

		try
		{
			output.write(o.trame, 0, o.tailleTrame);
			output.flush();
		}
		catch(IOException e)
		{
			/**
			 * Si la carte ne répond vraiment pas, on recommence de manière
			 * infinie.
			 * De toute façon, on n'a pas d'autre choix...
			 */
			log.write("Ne peut pas parler à la carte. Tentative de reconnexion.", Severity.WARNING, Subject.COMM);
			openSocket(50);
			// On a retrouvé la série, on renvoie le message
			communiquer(o);
		}
	}

	public boolean isClosed()
	{
		return mustClose;
	}

	public Paquet readPaquet() throws InterruptedException, ClosedSerialException
	{
		if(simuleSerie)
			while(true)
				Thread.sleep(10000);

		openSocket(50);
		try {
			int k = input.read();

			if(k == -1)
				throw new ClosedSerialException("EOF de l'input de communication");
			
			assert k == 0xFF : "Mauvais entête de paquet : "+k;
			
			int origineInt = input.read();
			Id origine = Id.parseId(origineInt);
			origine.answerReceived();
			
			int taille = input.read();
			assert taille >= 0 && taille <= 254 : "Le message reçu a un mauvais champ \"length\" : "+taille;
			int[] message = new int[taille];
			for(int i = 0; i < taille; i++)
				message[i] = input.read();
			
			return new Paquet(message, origine);
		} catch (IOException e) {
			throw new ClosedSerialException(e.getMessage());
		}
	}
}
