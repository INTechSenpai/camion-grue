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
 * Une trame série
 * 
 * @author pf
 *
 */

public abstract class Frame
{
	public enum IncomingCode
	{
		EXECUTION_BEGIN(0xFC),
		EXECUTION_END(0xFB),
		STATUS_UPDATE(0xFA),
		VALUE_ANSWER(0xF9);

		public final int code;

		private IncomingCode(int code)
		{
			this.code = code;
		}
	}

	public enum OutgoingCode
	{
		NEW_ORDER(0xFF),
		END_ORDER(0xFE),
		VALUE_REQUEST(0xFD);

		public final byte code;

		private OutgoingCode(int code)
		{
			this.code = (byte) code;
		}
	}

	public int id;

}