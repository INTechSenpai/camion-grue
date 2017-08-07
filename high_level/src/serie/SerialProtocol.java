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

import serie.trame.Order.Type;

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

	public enum OutOrder
	{
		/**
		 * Protocole Java vers bas niveau
		 */
		FOLLOW_TRAJECTORY(0x38, Type.LONG),
		STOP(0x39, Type.LONG),
		WAIT_FOR_JUMPER(0x3A, Type.LONG),
		START_MATCH_CHRONO(0x3B, Type.LONG),
		START_STREAM_ALL(0x3C, Type.LONG),
		PULL_DOWN_NET(0x3D, Type.LONG),
		PUT_NET_HALFWAY(0x3E, Type.LONG),
		PULL_UP_NET(0x3F, Type.LONG),
		OPEN_NET(0x40, Type.LONG),
		CLOSE_NET(0x41, Type.LONG),
		CROSS_FLIP_FLOP(0x42, Type.LONG),
		EJECT_LEFT_SIDE(0x43, Type.LONG),
		REARM_LEFT_SIDE(0x44, Type.LONG),
		EJECT_RIGHT_SIDE(0x45, Type.LONG),
		REARM_RIGHT_SIDE(0x46, Type.LONG),
		LOCK_NET(0x48, Type.LONG),
		SCAN(0x49, Type.LONG),
		CLOSE_NET_FORCE(0x4A, Type.LONG),
		RUN(0x4B, Type.LONG),

		ASK_COLOR(0x59, Type.SHORT),
		PING(0x5A, Type.SHORT),
		SEND_ARC(0x5B, Type.SHORT),
		SET_MAX_SPEED(0x5C, Type.SHORT),
		EDIT_POSITION(0x5D, Type.SHORT),
		STOP_STREAM_ALL(0x5E, Type.SHORT),
		SET_SENSOR_MODE(0x5F, Type.SHORT),
		SET_POSITION(0x60, Type.SHORT),
		SET_CURVATURE(0x61, Type.SHORT);

		public final byte code;
		public final Type type;

		private OutOrder(int code, Type type)
		{
			this.type = type;
			this.code = (byte) code;
		}
	}

	public enum InOrder
	{
		/**
		 * Protocole bas niveau vers Java
		 */

		// Réponse à "FollowTrajectory"
		ROBOT_ARRIVE(0x00, State.OK),
		ROBOT_BLOCAGE_EXTERIEUR(0x01, State.KO),
		ROBOT_BLOCAGE_INTERIEUR(0x02, State.KO),
		PLUS_DE_POINTS(0x03, State.KO),
		STOP_REQUIRED(0x04, State.KO),
		TROP_LOIN(0x05, State.KO),

		// Couleur
		COULEUR_BLEU(0x00, State.OK),
		COULEUR_JAUNE(0x01, State.OK),
		COULEUR_ROBOT_INCONNU(0x02, State.KO),

		// Réponse à "StartMatchChrono"
		MATCH_FINI(0x00, State.OK),
		ARRET_URGENCE(0x01, State.KO),

		// Actionneurs
		ACT_SUCCESS(0x00, State.OK),
		ACT_FAILURE(0x01, State.KO),

		SHORT_ORDER_ACK(-1, State.OK),
		LONG_ORDER_ACK(-1, State.OK),
		ORDER_ACK(-1, State.OK);

		public final int codeInt;
		public final State etat;

		private InOrder(int code, State etat)
		{
			codeInt = code;
			this.etat = etat;
		}
	}

}
