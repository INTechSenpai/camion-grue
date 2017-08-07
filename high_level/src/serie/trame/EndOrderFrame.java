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
 * Une trame sortant qui contient un END_ORDER
 * 
 * @author pf
 *
 */

public class EndOrderFrame extends OutgoingFrame
{
	private int base;

	/**
	 * Trame de END_ORDER
	 */
	public EndOrderFrame()
	{
		super();
		trame[0] = OutgoingCode.END_ORDER.code;
		trame[1] = 4; // longueur de la trame

		base = trame[0] + trame[1];

	}

	public void updateId(int id)
	{
		trame[2] = (byte) id;
		trame[3] = (byte) (base + id); // checksum
	}
}
