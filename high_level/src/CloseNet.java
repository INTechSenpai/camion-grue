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

import container.Container;
import exceptions.ContainerException;
import robot.RobotReal;

/**
 * Ferme le filet
 * 
 * @author pf
 *
 */

public class CloseNet
{

	/**
	 * Position de départ : (550, 1905), -Math.PI/2
	 * 
	 * @param args
	 */

	public static void main(String[] args)
	{
		Container container = null;
		try
		{
			container = new Container();
			RobotReal robot = container.getService(RobotReal.class);
			robot.fermeFilet();
		} catch (ContainerException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally
		{
			try {
				System.exit(container.destructor().code);
			} catch (ContainerException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
