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

import pfg.config.Config;
import pfg.graphic.log.Log;
import senpai.ConfigInfoSenpai;
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
		System.out.println("Initialisation communication");
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
	public synchronized void communiquer(Order o) throws InterruptedException
	{
		// série fermée normalement
		if(closed)
			return;
		
		boolean error;
		do {
			error = false;
			if(medium.openIfClosed())
			{
				input = medium.getInput();
				output = medium.getOutput();
			}
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

	public synchronized Paquet readPaquet() throws InterruptedException
	{
		// série fermée définitivement
		if(closed)
			while (true)
			    Thread.sleep(Long.MAX_VALUE);
			
		while(true)
		{
			if(medium.openIfClosed())
			{
				input = medium.getInput();
				output = medium.getOutput();
			}
	
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
