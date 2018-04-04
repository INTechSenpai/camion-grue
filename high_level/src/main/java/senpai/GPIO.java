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

package senpai;

import java.io.IOException;

/**
 * Classe pour contrôler les GPIO
 * @author pf
 *
 */

public class GPIO
{
	public static void allumeDiode()
	{
		try
		{
			Runtime.getRuntime().exec("gpio write 22 1");
		}
		catch(IOException e)
		{}
	}
	
	public static void eteintDiode()
	{
		try
		{
			Runtime.getRuntime().exec("gpio write 22 0");
		}
		catch(IOException e)
		{}
	}
	
	public static void clignoteDiode(int nbSecondes)
	{
		try
		{
			for(int i = 0; i < nbSecondes; i++)
			{
				Runtime.getRuntime().exec("gpio write 22 1");
				Thread.sleep(500);
				Runtime.getRuntime().exec("gpio write 22 0");
				Thread.sleep(500);
			}
		}
		catch(IOException  | InterruptedException e)
		{}
	}
	
}
