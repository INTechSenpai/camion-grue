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
import java.nio.ByteBuffer;
import pfg.config.Config;
import pfg.log.Log;
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
	
	private CommMedium medium;
	
	private volatile OutputStream output;
	private volatile InputStream input;

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
		String connexion = config.getString(ConfigInfoSenpai.COMM_MEDIUM);
		if(connexion.compareToIgnoreCase("ethernet") == 0)
			medium = new Ethernet(log);
		else if(connexion.compareToIgnoreCase("serie") == 0)
			medium = new Serie(log);
		else
			assert false;
		
		medium.initialize(config);
	}


	/**
	 * Il donne à la communication tout ce qu'il faut pour fonctionner
	 * @throws InterruptedException 
	 */
	public synchronized void initialize() throws InterruptedException
	{
		medium.open(500);
		input = medium.getInput();
		output = medium.getOutput();
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
		if(medium != null)
			medium.close();
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
			checkDisconnection();
			try
			{
				output.write(o.trame, 0, o.tailleTrame);
				output.flush();
			}
			catch(IOException e)
			{
				log.write("Erreur d'envoi de données : "+e, Subject.COMM);
				error = true;
			}
		} while(error);
	}

	public Paquet readPaquet() throws InterruptedException
	{
		// série fermée définitivement
		if(closed)
			throw new InterruptedException("Série fermée normalement");
			
		while(true)
		{
			checkDisconnection();
			assert input != null && output != null;

			try {
				int k = read();
				
				assert k == 0xFF : "Mauvais entête de paquet : "+k;
				if(k != 0xFF) // probablement pas un début de trame
					continue;
				
				int origineInt = read();

				int taille = read();
				if(taille == 0xFF) // trame d'information !
				{
//					log.write("On ignore une trame d'information", Subject.COMM);
					while(read() != 0x00); // on lit jusqu'à tomber sur un caractère de fin de chaîne 
				}
				
				else
				{	
					assert taille >= 0 && taille <= 254 : "Le message reçu a un mauvais champ \"length\" : "+taille;
					ByteBuffer message = ByteBuffer.allocate(taille);

					for(int i = 0; i < taille; i++)
						message.put((byte) read());
					message.flip();
					
					Id origine = Id.LUT[origineInt];
					assert origine != null : "ID inconnu : "+origineInt+", data : "+message;

					if(origine == null)
					{
						log.write("Un ordre d'ID inconnu a été ignoré : "+origineInt+", data = "+message, Severity.WARNING, Subject.COMM);
						continue;
					}

					origine.answerReceived();

					return new Paquet(message, origine);
				}
			} catch(IOException e)
			{
				// communication fermé
				if(closed)
					throw new InterruptedException("Série fermée normalement");
				log.write("Erreur de lecture de données : "+e, Subject.COMM);
			}
		}
	}
	
	private int read() throws IOException
	{
		int out = input.read();
		if(out == -1)
			throw new IOException("EOF de l'input de communication");
		return out;
	}

	private synchronized void checkDisconnection() throws InterruptedException
	{
		if(medium.openIfClosed())
		{
			input = medium.getInput();
			output = medium.getOutput();
		}
		assert input != null && output != null;		
	}
}
