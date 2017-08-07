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

package serie.trame;

import serie.Ticket;
import serie.SerialProtocol.OutOrder;
import serie.trame.Frame.IncomingCode;

/**
 * Paquet série haut niveau reçu
 * 
 * @author pf
 *
 */

public class Paquet
{
	public OutOrder origine;
	public IncomingCode code;
	public int[] message;
	public Ticket ticket;

	public Paquet(int[] message, Ticket ticket, OutOrder origine, IncomingCode code)
	{
		this.origine = origine;
		this.message = message;
		this.ticket = ticket;
		this.code = code;
	}

	@Override
	public String toString()
	{
		String aff = "";
		for(int i = 0; i < message.length; i++)
		{
			int out = message[i];
			String s = Integer.toHexString(out).toUpperCase();
			if(s.length() == 1)
				aff += "0" + s + " ";
			else
				aff += s.substring(s.length() - 2, s.length()) + " ";
		}
		return origine + " " + code + " " + aff;
	}
}
