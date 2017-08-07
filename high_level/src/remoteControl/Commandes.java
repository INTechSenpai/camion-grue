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

package remoteControl;

import java.io.Serializable;

/**
 * Commande de contrôle à distance
 * @author pf
 *
 */

public enum Commandes implements Serializable
{
	SPEED_UP, // 0
	SPEED_DOWN, // 1
	SET_SPEED, // 2
	TURN_RIGHT, // 3
	TURN_LEFT, // 4
	SET_DIRECTION, // 5
	STOP, // 6
	RESET_WHEELS, // 7
	SHUTDOWN, // 8
	PING, // 9
	LEVE_FILET, // 10
	BAISSE_FILET, // 11
	FERME_FILET, // 12
	OUVRE_FILET, // 13
	EJECTE_GAUCHE, // 14
	EJECTE_DROITE, // 15
	REARME_GAUCHE, // 16
	REARME_DROITE; // 17
	
	private Commandes()
	{}
	
	public int code = -1;
	
	public void setCode(int code)
	{
		this.code = code;
	}
	
}
