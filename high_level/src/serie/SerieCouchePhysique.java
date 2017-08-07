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

package serie;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import exceptions.serie.ClosedSerialException;
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

public class SerieCouchePhysique
{
	protected Log log;
	private BufferIncomingBytes buffer;
//	private SerialListener listener;
	
	private int port;
	private InetAddress adresse;
	private Socket socket;

	private boolean simuleSerie;
	private boolean mustClose = false;

	/** The output stream to the port */
	private OutputStream output;

	/**
	 * Constructeur pour la série de test
	 * 
	 * @param log
	 */
	public SerieCouchePhysique(Log log, BufferIncomingBytes buffer, ConfigSenpai config)
	{
		this.log = log;
		this.buffer = buffer;
//		this.listener = listener;

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

		if(!socket.isConnected() || socket.isClosed())
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
			buffer.setInput(socket.getInputStream());
			output = socket.getOutputStream();
	
			// Configuration du listener TODO
	
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
				buffer.close();
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
	public synchronized void communiquer(byte[] bufferWriting, int offset, int length) throws InterruptedException, ClosedSerialException
	{
		if(simuleSerie)
			return;

		/**
		 * Un appel à une série fermée ne devrait jamais être effectué.
		 */

		try
		{
			output.write(bufferWriting, offset, length);
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
			communiquer(bufferWriting, offset, length);
		}
	}

	public boolean isClosed()
	{
		return mustClose;
	}
}
