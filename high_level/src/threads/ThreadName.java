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

package threads;

import threads.comm.ThreadCommProcess;
import threads.comm.ThreadCommListener;
import threads.comm.ThreadCommEmitter;

/**
 * Tous les threads à instancier au début du match. Utilisé par le container
 * 
 * @author pf
 *
 */

public enum ThreadName
{
	CAPTEURS(ThreadCapteurs.class),
	FENETRE(ThreadFenetre.class),
	PRINT_SERVER(ThreadPrintServer.class),
	REMOTE_CONTROL(ThreadRemoteControl.class),
	PEREMPTION(ThreadPeremption.class),
	SERIAL_INPUT_ORDRE(ThreadCommProcess.class),
	SERIAL_INPUT_TRAME(ThreadCommListener.class),
	SERIAL_OUTPUT_ORDER(ThreadCommEmitter.class);

	public Class<? extends Thread> c;

	private ThreadName(Class<? extends Thread> c)
	{
		this.c = c;
	}

}
