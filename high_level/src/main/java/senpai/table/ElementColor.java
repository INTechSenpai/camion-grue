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

package senpai.table;

import java.awt.Color;

/**
 * Les couleurs des éléments de jeux
 * @author Pierre-François Gimenez
 *
 */

public enum ElementColor
{
	VERT(Color.GREEN),
	NOIR(Color.BLACK),
	JAUNE(Color.YELLOW),
	ORANGE(Color.ORANGE),
	BLEU(Color.BLUE);
	public final Color color;
	
	private ElementColor(Color color)
	{
		this.color = color;
	}
}
