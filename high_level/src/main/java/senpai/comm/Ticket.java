/*
 * Copyright (C) 2013-2018 Pierre-François Gimenez
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Un ticket. Tu tires un numéro et tu attends ton tour.
 * Utilisé par la série pour notifier des infos.
 * 
 * @author pf
 *
 */

public class Ticket
{	
	private final BlockingQueue<DataTicket> ordre = new ArrayBlockingQueue<DataTicket>(1); // seulement un élément !
	/*
	public boolean isEmpty()
	{
		return ordre.isEmpty();
	}
*/
	
	public void set(CommProtocol.State order, Object data)
	{
//		assert ordre.isEmpty();
		ordre.offer(new DataTicket(data, order));
	}
	
	public void set(CommProtocol.State order)
	{
//		assert ordre.isEmpty();
		ordre.offer(new DataTicket(order));
	}

	/**
	 * Récupère le status. Bloquant
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public DataTicket attendStatus() throws InterruptedException
	{
		return ordre.take();
	}
	
	/**
	 * Attend le status avec un timeout.
	 * @param timeout
	 * @throws InterruptedException
	 */
	public DataTicket attendStatus(long timeout) throws InterruptedException
	{
		return ordre.poll(Math.max(0,timeout), TimeUnit.MILLISECONDS);
	}

}
