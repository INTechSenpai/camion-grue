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
import java.util.Enumeration;
import java.util.TooManyListenersException;
import exceptions.serie.ClosedSerialException;
import pfg.config.Config;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;

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
	private SerialListener listener;

	protected volatile boolean isClosed;
	private int baudrate;
	private boolean simuleSerie;

	private String portName;

	/** The output stream to the port */
	private OutputStream output;

	// Permet d'ouvrir le port à la première utilisation de la série
	protected volatile boolean portOuvert = false;

	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;

	/**
	 * Constructeur pour la série de test
	 * 
	 * @param log
	 */
	public SerieCouchePhysique(Log log, BufferIncomingBytes buffer, Config config, SerialListener listener)
	{
		this.log = log;
		this.buffer = buffer;
		this.listener = listener;

		portName = config.getString(ConfigInfoSenpai.SERIAL_PORT);
		baudrate = config.getInt(ConfigInfo.BAUDRATE);
		simuleSerie = config.getBoolean(ConfigInfo.SIMULE_SERIE);

		if(simuleSerie)
			log.critical("SÉRIE SIMULÉE !");
	}

	/**
	 * Ouverture du port
	 * 
	 * @throws InterruptedException
	 */
	protected synchronized void openPort() throws InterruptedException
	{
		// TODO ouverture du socket
	}

	/**
	 * Recherche du port
	 * 
	 * @return
	 */
	protected synchronized boolean searchPort()
	{
		if(simuleSerie)
			return true;

		portOuvert = false;
		CommPortIdentifier port;
		try
		{
			port = CommPortIdentifier.getPortIdentifier(portName);
			log.debug("Port " + port.getName() + " trouvé !");

			if(port.isCurrentlyOwned())
			{
				log.warning("Port déjà utilisé par " + port.getCurrentOwner());
				return false;
			}

			if(!initialize(port, baudrate))
				return false;

			portOuvert = true;
			return true;

		}
		catch(NoSuchPortException e)
		{
			log.warning("Port " + portName + " introuvable : " + e);
			String out = "Les ports disponibles sont : ";

			Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();

			while(ports.hasMoreElements())
			{
				out += ((CommPortIdentifier) ports.nextElement()).getName();
				if(ports.hasMoreElements())
					out += ", ";
			}

			log.debug(out);
			return false;
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
	private boolean initialize(CommPortIdentifier portId, int baudrate)
	{
		if(simuleSerie)
			return true;

		try
		{
			serialPort = (SerialPort) portId.open("MoonRover", TIME_OUT);
			// set port parameters
			serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			// serialPort.setInputBufferSize(100);
			// serialPort.setOutputBufferSize(100);
			// serialPort.enableReceiveTimeout(100);
			// serialPort.enableReceiveThreshold(1);

			// open the streams
			buffer.setInput(serialPort.getInputStream());
			output = serialPort.getOutputStream();

			// Configuration du Listener
			serialPort.addEventListener(listener);

			serialPort.notifyOnDataAvailable(true); // activation du listener
													// pour vérifier qu'on a des
													// données disponible
			serialPort.notifyOnOutputEmpty(true); // activation du listener pour
													// vérifier que l'envoi est
													// fini

			isClosed = false;
			return true;
		}
		catch(TooManyListenersException | PortInUseException | UnsupportedCommOperationException | IOException e2)
		{
			log.critical(e2);
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

		if(!isClosed && portOuvert)
		{
			try
			{
				log.debug("Fermeture de la carte");
				serialPort.removeEventListener();
				serialPort.close();
				buffer.close();
				output.close();
			}
			catch(IOException e)
			{
				log.warning(e);
			}
			isClosed = true;
		}
		else if(isClosed)
			log.warning("Carte déjà fermée");
		else
			log.warning("Carte jamais ouverte");
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

		openPort();

		/**
		 * Un appel à une série fermée ne devrait jamais être effectué.
		 */
		if(isClosed)
			throw new ClosedSerialException("La série est fermée et ne peut envoyer un message");

		try
		{
			// On vérifie bien que toutes les données précédentes ont été
			// envoyées
			synchronized(listener)
			{
				if(!listener.isOutputEmpty())
					listener.wait();

				listener.setOutputNonEmpty();

				output.write(bufferWriting, offset, length);
				output.flush();

				String aff = "";
				for(int i = offset; i < offset + length; i++)
				{
					int out = bufferWriting[i];
					String s = Integer.toHexString(out).toUpperCase();
					if(s.length() == 1)
						aff += "0" + s + " ";
					else
						aff += s.substring(s.length() - 2, s.length()) + " ";
				}
				log.debug("Envoi terminé de " + aff, Verbose.SERIE.masque);
			}
		}
		catch(IOException e)
		{
			/**
			 * Si la carte ne répond vraiment pas, on recommence de manière
			 * infinie.
			 * De toute façon, on n'a pas d'autre choix...
			 */
			log.critical("Ne peut pas parler à la carte. Tentative de reconnexion.");
			while(!searchPort())
			{
				log.critical("Pas trouvé... On recommence");
				// On laisse la série respirer un peu
				Thread.sleep(100);
			}
			// On a retrouvé la série, on renvoie le message
			communiquer(bufferWriting, offset, length);
		}
	}

	public boolean isClosed()
	{
		return isClosed;
	}
	
	public void init() throws InterruptedException
	{
		openPort();
	}

}
