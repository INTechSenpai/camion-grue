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

package senpai.capteurs;

/**
 * Les différents modes d'utilisation des capteurs
 * 
 * @author pf
 *
 */

public enum SensorMode
{

	NONE(0),
	FRONT_AND_BACK(1),
	FRONT_AND_SIDES(2),
	BACK_AND_SIDES(3),
	ALL(4);

	public final byte code;

	private SensorMode(int code)
	{
		this.code = (byte) code;
	}

}
