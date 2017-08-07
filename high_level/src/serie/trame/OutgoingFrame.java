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

/**
 * Trame qu'on envoie
 * 
 * @author pf
 *
 */

public class OutgoingFrame extends Frame
{
	public OutgoingCode code;
	public final byte[] trame = new byte[256]; // la taille maximale
	public int tailleTrame;

	protected OutgoingFrame()
	{
		code = OutgoingCode.END_ORDER;
		tailleTrame = 4;
	}

	/**
	 * Constructeur d'une trame à envoyer (NEW_ORDER ou VALUE_REQUEST)
	 * 
	 * @param o
	 */
	public OutgoingFrame(int id)
	{
		this.id = id;
		trame[2] = (byte) id;
	}

	@Override
	public String toString()
	{
		if(tailleTrame == 0)
			return "Outgoing : " + code + " " + id + " (pas de données)";

		String m = "Outgoing : " + code + " " + id + " // ";
		for(int i = 0; i < tailleTrame; i++)
		{
			String s = Integer.toHexString(trame[i]).toUpperCase();
			if(s.length() == 1)
				m += "0" + s + " ";
			else
				m += s.substring(s.length() - 2, s.length()) + " ";
		}
		return m;
	}

	/**
	 * Met à jour la trame à envoyer (NEW_ORDER ou VALUE_REQUEST)
	 * 
	 * @param o
	 */
	public void update(Order o)
	{
		int tailleMessage;
		if(o.message == null)
			tailleMessage = 0;
		else
			tailleMessage = o.message.limit();
		tailleTrame = tailleMessage + 5;
		if(tailleTrame > 255)
			throw new IllegalArgumentException("La trame est trop grande ! (" + tailleTrame + " octets)");
		code = o.ordre.type == Order.Type.LONG ? OutgoingCode.NEW_ORDER : OutgoingCode.VALUE_REQUEST;
		trame[0] = code.code;
		trame[1] = (byte) (tailleTrame);

		trame[3] = o.ordre.code;
		for(int i = 0; i < tailleMessage; i++)
			trame[i + 4] = o.message.get();

		/**
		 * Calcul du checksum
		 */
		int c = 0;
		for(int i = 0; i < tailleTrame - 1; i++)
			c += trame[i];
		trame[tailleTrame - 1] = (byte) (c);
	}

}
