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

package capteurs;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import utils.Log;
import container.Service;
import container.dependances.SerialClass;

/**
 * Buffer qui contient les infos provenant des capteurs de la STM
 * 
 * @author pf
 *
 */

public class SensorsDataBuffer implements Service, SerialClass
{
	protected Log log;

	public SensorsDataBuffer(Log log)
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

	private Queue<SensorsData> buffer = new ConcurrentLinkedQueue<SensorsData>();

	/**
	 * Ajout d'un élément dans le buffer et provoque un "notifyAll"
	 * 
	 * @param elem
	 */
	public synchronized void add(SensorsData elem)
	{
		buffer.add(elem);
		if(buffer.size() > 5)
		{
			buffer.poll(); // on évacue une ancienne valeur
			log.critical("Capteurs traités trop lentement !");
		}
		notify();
	}

	/**
	 * Retire un élément du buffer
	 * 
	 * @return
	 */
	public synchronized SensorsData poll()
	{
		return buffer.poll();
	}

}
