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

package comm.buffer;

import comm.SerieCouchePhysique;
import exceptions.ClosedSerialException;
import pfg.log.Log;
import senpai.Severity;
import senpai.Subject;

/**
 * Buffer très bas niveau qui envoie les octets sur le socket
 * 
 * @author pf
 *
 */

public class BufferOutgoingBytes
{
	protected Log log;
	private SerieCouchePhysique serie;

	private byte bufferWriting[] = new byte[16384];

	private volatile int indexBufferStart = 0;
	private volatile int indexBufferStop = 0;

	public BufferOutgoingBytes(Log log, SerieCouchePhysique serie)
	{
		this.log = log;
		this.serie = serie;
	}

	public synchronized void add(byte[] b, int taille) throws ClosedSerialException
	{
		if(serie.isClosed())
			throw new ClosedSerialException("Communication fermée !");


		int diffOld = (indexBufferStop - indexBufferStart + 16384) & 0x3FFF;
		if(taille + indexBufferStop <= 16384)
		{
			System.arraycopy(b, 0, bufferWriting, indexBufferStop, taille);
			indexBufferStop += taille;
			indexBufferStop &= 0x3FFF;
		}
		else
		{
			int l = 16384 - indexBufferStop;
			System.arraycopy(b, 0, bufferWriting, indexBufferStop, l);
			System.arraycopy(b, l, bufferWriting, 0, taille - l);
			indexBufferStop = taille - l;
		}
		int diffNew = (indexBufferStop - indexBufferStart + 16384) & 0x3FFF;

		if(diffNew < diffOld) // cette différence ne peut diminuer qu'en cas
								// d'overflow
			log.write("Overflow du buffer d'envoi !", Severity.CRITICAL, Subject.COMM);
		notify();
	}

	/**
	 * Lit un octet
	 * On sait qu'un octet doit s'y trouver ; soit parce que available()
	 * retourne "true", soit parce que le protocole l'impose.
	 * 
	 * @return
	 * @throws InterruptedException
	 * @throws ClosedSerialException 
	 */
	public final synchronized void send() throws InterruptedException, ClosedSerialException
	{
		if(!isEmpty())
		{
			if(indexBufferStop > indexBufferStart) // un seul envoi
				serie.communiquer(bufferWriting, indexBufferStart, indexBufferStop - indexBufferStart);
			else // deux envois
			{
				serie.communiquer(bufferWriting, indexBufferStart, 16384 - indexBufferStart);
				if(indexBufferStop != 0)
					serie.communiquer(bufferWriting, 0, indexBufferStop);
			}
			indexBufferStart = indexBufferStop;
		}
	}

	/**
	 * Fermeture de la série
	 */
	public void close()
	{
		serie.close();
	}
	
	public synchronized boolean isEmpty()
	{
		return indexBufferStart == indexBufferStop;
	}

}
