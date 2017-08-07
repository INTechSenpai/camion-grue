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

import exceptions.serie.IncorrectChecksumException;

/**
 * Une trame qu'on a reçue
 * 
 * @author pf
 *
 */

public class IncomingFrame extends Frame
{
	public IncomingCode code;

	public int[] message;

	/**
	 * Constructeur d'une trame reçue
	 * 
	 * @return
	 */
	public IncomingFrame(int code, int id, int checksum, int longueur, int[] message) throws IncorrectChecksumException, IllegalArgumentException
	{
		this.id = id;
		this.message = message;

		int c = code + id + longueur;
		for(int i = 0; i < message.length; i++)
			c += message[i];
		c = c & 0xFF;

		/**
		 * On cherche à quel type de trame correspond la valeur reçue
		 */
		if(code == IncomingCode.EXECUTION_BEGIN.code)
			this.code = IncomingCode.EXECUTION_BEGIN;
		else if(code == IncomingCode.EXECUTION_END.code)
			this.code = IncomingCode.EXECUTION_END;
		else if(code == IncomingCode.STATUS_UPDATE.code)
			this.code = IncomingCode.STATUS_UPDATE;
		else if(code == IncomingCode.VALUE_ANSWER.code)
			this.code = IncomingCode.VALUE_ANSWER;
		else
		{
			this.code = null;
			throw new IllegalArgumentException("Type de trame inconnu : " + code + " (" + toString() + ")");
		}

		if(c != checksum)
			throw new IncorrectChecksumException("Checksum attendu : " + checksum + ", checksum calculé : " + c);
	}

	@Override
	public String toString()
	{
		if(message.length == 0)
			return "Incoming : " + code + " " + id + " (pas de données)";

		String m = "Incoming : " + code + " " + id + " // ";
		for(int i = 0; i < message.length; i++)
		{
			String s = Integer.toHexString(message[i]).toUpperCase();
			if(s.length() == 1)
				m += "0" + s + " ";
			else
				m += s.substring(s.length() - 2, s.length()) + " ";
		}
		return m;
	}

	public static void check(int code) throws IllegalArgumentException
	{
		if(code != IncomingCode.EXECUTION_BEGIN.code && code != IncomingCode.EXECUTION_END.code && code != IncomingCode.STATUS_UPDATE.code && code != IncomingCode.VALUE_ANSWER.code)
			throw new IllegalArgumentException("Type de trame inconnu : " + code);
	}

}
