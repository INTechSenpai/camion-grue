/*
 * Copyright (C) 2013-2018 Pierre-François Gimenez
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

package senpai.threads;

import senpai.threads.comm.ThreadCommEmitter;
import senpai.threads.comm.ThreadCommListener;
import senpai.threads.comm.ThreadCommProcess;

/**
 * Tous les threads à instancier au début du match. Utilisé par le container
 * 
 * @author pf
 *
 */

public enum ThreadName
{
	CAPTEURS(ThreadCapteurs.class),
//	KRAKEN(ThreadKraken.class),
	TOURELLE(ThreadTourelles.class),
	PEREMPTION(ThreadPeremption.class),
//	CLIGOTE_DEGRADE(ThreadClignoteDegrade.class),
//	CLIGNOTE_NORMAL(ThreadClignoteNormal.class),
	COLLISION_DEGRADE(ThreadCollisionDegrade.class),
	SERIAL_INPUT_ORDRE(ThreadCommProcess.class),
	SERIAL_INPUT_TRAME(ThreadCommListener.class),
	SERIAL_OUTPUT_ORDER(ThreadCommEmitter.class);

	public Class<? extends Thread> c;

	private ThreadName(Class<? extends Thread> c)
	{
		this.c = c;
	}

}
