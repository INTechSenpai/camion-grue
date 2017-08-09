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

import comm.CommProtocol.InOrder;

/**
 * Un ticket. Tu tires un numéro et tu attends ton tour.
 * Utilisé par la série pour notifier des infos.
 * 
 * @author pf
 *
 */

public class Ticket
{
	private volatile InOrder order;

	private synchronized InOrder getAndClear()
	{
		InOrder out = order;
		order = null;
		return out;
	}

	public synchronized boolean isEmpty()
	{
		return order == null;
	}

	public synchronized void set(InOrder order)
	{
		this.order = order;
		notify();
	}

	/**
	 * Récupère le status. Bloquant
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public synchronized InOrder attendStatus() throws InterruptedException
	{
		if(isEmpty())
			wait();

		return getAndClear();
	}
	
	/**
	 * Attend le status avec un timeout.
	 * @param timeout
	 * @throws InterruptedException
	 */
	public synchronized void attendStatus(long timeout) throws InterruptedException
	{
		if(isEmpty())
			wait(Math.max(0, timeout));
	}

}
