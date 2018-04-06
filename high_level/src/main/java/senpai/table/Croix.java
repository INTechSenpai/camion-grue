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

package senpai.table;

import pfg.kraken.utils.XY;

/**
 * Une croix de cube
 * @author pf
 *
 */

public enum Croix
{
	CROIX_HAUT_GAUCHE(-650, 1460),
	CROIX_CENTRE_GAUCHE(-400, 500),
	CROIX_TOUT_GAUCHE(-1200, 810),
	CROIX_HAUT_DROITE(650, 1460),
	CROIX_CENTRE_DROITE(400, 500),
	CROIX_TOUT_DROITE(1200, 810);
	
	public final XY center;

	private Croix(double x, double y)
	{
		this.center = new XY(x,y);
	}
}