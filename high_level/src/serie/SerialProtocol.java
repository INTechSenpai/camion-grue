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

/**
 * Protocole série entre le bas niveau et la Java
 * 
 * @author pf
 *
 */

public class SerialProtocol
{

	public enum State
	{
		OK,
		KO;
	}
	
	public enum Channel
	{
		DESINSCRIPTION,
		INSCRIPTION;
		
		public final byte code = (byte) ordinal();		
	}

	public enum OutOrder
	{
		/**
		 * Protocole Java vers bas niveau
		 */
		FOLLOW_TRAJECTORY(0x38),
		STOP(0x39),
		WAIT_FOR_JUMPER(0x3A),
		START_MATCH_CHRONO(0x3B),
		SCAN(0x49),
		RUN(0x4B),
		SENSORS_CHANNEL(0x00),

		ASK_COLOR(0x59),
		PING(0x5A),
		SEND_ARC(0x5B),
		SET_MAX_SPEED(0x5C),
		EDIT_POSITION(0x5D),
		STOP_STREAM_ALL(0x5E),
		SET_SENSOR_MODE(0x5F),
		SET_POSITION(0x60),
		SET_CURVATURE(0x61);
		public final byte code;
		
		private OutOrder(int code)
		{
			this.code = (byte) code;
		}
	}

	public enum InOrder
	{
		;

		public final int codeInt;
		public final State etat;

		private InOrder(int code, State etat)
		{
			codeInt = code;
			this.etat = etat;
		}
	}

}
