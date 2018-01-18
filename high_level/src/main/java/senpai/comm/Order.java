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

import java.nio.ByteBuffer;
import senpai.comm.CommProtocol.Id;

/**
 * Un ordre à envoyer sur la série
 * 
 * @author pf
 *
 */

public class Order
{	
	public Id ordre;
	public final byte[] trame = new byte[300]; // un peu de marge
	public int tailleTrame;
	
	public final int getPriority()
	{
		return ordre.priority;
	}
	
	public Order(ByteBuffer message, Id ordre)
	{
		if(message != null)
			message.flip();
		this.ordre = ordre;
		update(message);
	}

	public Order(Id ordre)
	{
		this(null, ordre);
	}

	private void update(ByteBuffer message)
	{
		int tailleMessage;
		if(message == null)
			tailleMessage = 0;
		else
			tailleMessage = message.limit();
		tailleTrame = tailleMessage + 3;
		assert tailleMessage <= 254 : "Le message est trop long ! (" + tailleTrame + " octets)";
		trame[0] = (byte) 0xFF;
		trame[1] = ordre.code;
		trame[2] = (byte) (tailleMessage);

		for(int i = 0; i < tailleMessage; i++)
			trame[i + 3] = message.get();
	}
	
	@Override
	public String toString()
	{
		return ordre+", taille de la trame: "+tailleTrame;
	}
}
