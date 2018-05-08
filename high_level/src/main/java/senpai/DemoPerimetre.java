package senpai;

import senpai.Senpai.ErrorCode;
import senpai.comm.CommProtocol.Id;
import senpai.comm.CommProtocol.LLCote;
import senpai.exceptions.ActionneurException;
import senpai.robot.Robot;

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

/**
 * Montre le périmètre max
 * @author pf
 *
 */

public class DemoPerimetre
{
	public static void main(String[] args)
	{
		String configfile = "demo-perimetre.conf";
		
		if(args.length == 0)
		{
			System.out.println("Paramètres possible : droite, gauche");
			return;
		}

		Senpai senpai = new Senpai();
		ErrorCode error = ErrorCode.NO_ERROR;
		try
		{
			senpai = new Senpai();
			senpai.initialize(configfile, "default");
			Robot robot = senpai.getService(Robot.class);
			
			double cote;
			if(args[0].equals("droite"))
				cote = -1;
			else if(args[0].equals("gauche"))
				cote = 1;
			else
			{
				System.out.println("Paramètres possible : droite, gauche");
				return;
			}
			robot.execute(Id.ARM_GO_TO, cote * 80. * Math.PI / 180., 0., 4.974, 25.);
			robot.execute(Id.ARM_GO_TO, cote * 80. * Math.PI / 180., 0., 4.974 - Math.PI / 2, 25.);
			while(true)
				Thread.sleep(Integer.MAX_VALUE);
		}
		catch(Exception e)
		{
			Robot robot = senpai.getExistingService(Robot.class);
			if(robot != null)
				try {
					robot.rangeBras(LLCote.AU_PLUS_VITE);
				} catch (InterruptedException | ActionneurException e1) {
					e1.printStackTrace();
				}
			error = ErrorCode.EXCEPTION;
			error.setException(e);
		}
		finally
		{
			try
			{
				senpai.destructor(error);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
}
