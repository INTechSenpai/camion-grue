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

/**
 * Protocole série entre le bas niveau et la Java
 * 
 * @author pf
 *
 */

public class CommProtocol
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
		public final boolean started = ordinal() == 1;
	}

	public enum Id
	{
		/**
		 * Protocole Java vers bas niveau
		 */
		
		// Canaux de données (0x00 à 0x1F)
		SENSORS_CHANNEL(0x00, true),
		
		// Ordres longs (0x20 à 0x7F)
		FOLLOW_TRAJECTORY(0x38, true),
		STOP(0x39, true),
		WAIT_FOR_JUMPER(0x3A, true),
		START_MATCH_CHRONO(0x3B, true),
		SCAN(0x49, false),
		RUN(0x4B, false),
		ASK_COLOR(0x59, true),
		PING(0x5A, true),
		SEND_ARC(0x5B, false),
		SET_MAX_SPEED(0x5C, false),
		EDIT_POSITION(0x5D, false),
		SET_SENSOR_MODE(0x5F, false),
		SET_POSITION(0x60, false),
		SET_CURVATURE(0x61, false);
		
		// Ordres immédiats (0x80 à 0xFF)
		
		public final byte code;
		private boolean sendIsPossible; // "true" si un ordre est lancé, "false" sinon
		public final Ticket ticket;
		private final boolean isStream;
		private boolean streamStarted;
		private final boolean expectAnswer;
		private boolean waitingForAnswer;
		
		public void changeStreamState(Channel newState)
		{
			assert isStream : "changeStreamState sur "+this+" qui n'est pas un canal de données (code = "+code+")";
			assert streamStarted != newState.started : "Canal déjà ouvert ou fermé !";
			assert waitingForAnswer == streamStarted : "Invariant rompu : waitingForAnswer = "+waitingForAnswer+" et streamStarted = "+streamStarted; // invariant pour les streams
			streamStarted = newState.started;
			waitingForAnswer = streamStarted;
		}
		
		private Id(int code, boolean expectAnswer)
		{
			this.expectAnswer = expectAnswer;
			isStream = code < 23;
			// les streams doivent toujours pouvoir attendre une réponse
			assert !isStream || expectAnswer : "Les canaux de données doivent pouvoir attendre une réponse";
			sendIsPossible = true;
			waitingForAnswer = false;
			streamStarted = false;
			
			this.code = (byte) code;
			// Les canaux de données n'ont pas de tickets
			if(expectAnswer)
				ticket = new Ticket();
			else
				ticket = null;
		}
		
		public void answerReceived()
		{
			assert waitingForAnswer && expectAnswer : "Réponse inattendue reçue pour "+this;
			if(!isStream)
				waitingForAnswer = true;
			sendIsPossible = true;
		}
		
		public void orderSent()
		{
			assert sendIsPossible : "Envoi impossible pour "+this+" : on attend encore une réponse";
			if(expectAnswer && !isStream)
			{
				sendIsPossible = false;
				waitingForAnswer = true;
			}
		}
		
		public static Id parseId(int code)
		{
			for(Id id : values())
				if(id.code == code)
					return id;
			assert false : "Code d'ordre inconnu : "+code;
			return null;
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

		ACK_SUCCESS(0x00, State.OK),
		ACK_FAILURE(0x01, State.KO);

		public final int codeInt;
		public final State etat;

		private InOrder(int code, State etat)
		{
			codeInt = code;
			this.etat = etat;
		}
	}

}
