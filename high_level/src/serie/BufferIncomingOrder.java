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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import pfg.log.Log;
import senpai.Subject;
import senpai.Severity;
import serie.trame.Paquet;

/**
 * Buffer qui contient les ordres provenant de la série
 * 
 * @author pf
 *
 */

public class BufferIncomingOrder
{
	protected Log log;

	public BufferIncomingOrder(Log log)
	{
		this.log = log;
	}

	/**
	 * Le buffer est-il vide?
	 * 
	 * @return
	 */
	public synchronized boolean isEmpty()
	{
		return buffer.isEmpty();
	}

	private Queue<Paquet> buffer = new ConcurrentLinkedQueue<Paquet>();

	/**
	 * Ajout d'un élément dans le buffer et provoque un "notify"
	 * 
	 * @param elem
	 */
	public synchronized void add(Paquet elem)
	{
		buffer.add(elem);
		if(buffer.size() > 20)
			log.write("Ordres entrants traités trop lentement ! Taille buffer : " + buffer.size(), Severity.CRITICAL, Subject.COMM);
		else if(buffer.size() > 5)
			log.write("Ordres entrants traités trop lentement ! Taille buffer : " + buffer.size(), Severity.WARNING, Subject.COMM);

		notify();
	}

	/**
	 * Retire un élément du buffer
	 * 
	 * @return
	 */
	public synchronized Paquet poll()
	{
		return buffer.poll();
	}

}
