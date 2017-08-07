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
import java.io.InputStream;
import utils.Log;
import utils.Log.Verbose;
import container.Service;
import container.dependances.SerialClass;
import exceptions.serie.ClosedSerialException;
import exceptions.serie.MissingCharacterException;

/**
 * Buffer très bas niveau qui récupère les octets sur la série
 * 
 * @author pf
 *
 */

public class BufferIncomingBytes implements Service, SerialClass
{
	private Log log;

	private InputStream input;
	private volatile boolean ping = false; // y a-t-il eu le ping initial avec le LL ?
	private volatile boolean closed = false;
	
	private int bufferReading[] = new int[16384];

	private volatile int indexBufferStart = 0;
	private volatile int indexBufferStop = 0;

	public BufferIncomingBytes(Log log)
	{
		this.log = log;
	}
	
	public void setPingDone()
	{
		ping = true;
	}

	public boolean hasPing()
	{
		return ping;
	}
	
	public void setInput(InputStream input)
	{
		this.input = input;
	}

	/**
	 * Récupération des données de la série
	 */
	public void dataAvailable()
	{
		try
		{
			do
			{
				synchronized(this)
				{
					bufferReading[indexBufferStop++] = input.read();
					indexBufferStop &= 0x3FFF;

					if(indexBufferStart == indexBufferStop)
						log.critical("Overflow du buffer de réception série !");

					notifyAll();
				}
			} while(input.available() > 0);

		}
		catch(IOException e)
		{
			log.critical(e);
		}
	}

	/**
	 * Retourne "true" ssi un octet est lisible en utilisant "read"
	 */
	public final synchronized boolean available()
	{
		return indexBufferStart != indexBufferStop;
	}

	/**
	 * Lit un octet
	 * On sait qu'un octet doit s'y trouver ; soit parce que available()
	 * retourne "true", soit parce que le protocole l'impose.
	 * 
	 * @return
	 * @throws IOException
	 * @throws MissingCharacterException
	 * @throws InterruptedException
	 * @throws ClosedSerialException 
	 */
	public final synchronized int read() throws MissingCharacterException, InterruptedException, ClosedSerialException
	{
		int essai = 0;
		while(indexBufferStart == indexBufferStop && essai < 100)
		{
			wait(0, 10000);
			essai++;
		}

		if(closed)
			throw new ClosedSerialException();
		
		if(indexBufferStart == indexBufferStop)
			throw new MissingCharacterException("Un caractère attendu n'est pas arrivé");

		int out = bufferReading[indexBufferStart++];
		indexBufferStart &= 0x3FFF;

		String s = Integer.toHexString(out).toUpperCase();
		if(s.length() == 1)
		{
			if(out >= 32 && out < 127)
				log.debug("Reçu : " + "0" + s + " (" + (char) (out) + ")", Verbose.TRAME.masque);
			else
				log.debug("Reçu : " + "0" + s, Verbose.TRAME.masque);
		}
		else
		{
			if(out >= 32 && out < 127)
				log.debug("Reçu : " + s.substring(s.length() - 2, s.length()) + " (" + (char) (out) + ")", Verbose.TRAME.masque);
			else
				log.debug("Reçu : " + s.substring(s.length() - 2, s.length()), Verbose.TRAME.masque);
		}

		return out;
	}

	/**
	 * Fermeture du flux d'arrivée
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		closed = true;
		input.close();
	}
}
