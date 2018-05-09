package senpai;

import pfg.kraken.robot.Cinematique;
import pfg.kraken.utils.XYO;
import pfg.log.Log;
import senpai.Senpai.ErrorCode;
import senpai.capteurs.CapteursProcess;
import senpai.comm.CommProtocol.LLCote;
import senpai.exceptions.ActionneurException;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.scripts.ScriptDomotiqueV2;
import senpai.table.Table;
import senpai.threads.comm.ThreadCommProcess;

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
 * Vérifie la hauteur de l'interrupteur
 * @author pf
 *
 */

public class VerifInterrupteur
{
	public static void main(String[] args)
	{
		String configfile = "demo-perimetre.conf";
		
		if(args.length == 0)
		{
			System.out.println("Paramètres possible : haut, bas");
			return;
		}

		Senpai senpai = new Senpai();
		ErrorCode error = ErrorCode.NO_ERROR;
		try
		{
			senpai = new Senpai();
			senpai.initialize(configfile, "default");
			Robot robot = senpai.getService(Robot.class);
			Log log = senpai.getService(Log.class);
			Table table = senpai.getService(Table.class);
			CapteursProcess cp = senpai.getService(CapteursProcess.class);
			robot.updateColorAndSendPosition(RobotColor.VERT, true);
			double x = robot.getCinematique().getPosition().getX();
			double y = robot.getCinematique().getPosition().getX();
			robot.setCinematique(new Cinematique(new XYO(x, y, Math.PI / 2)));
			senpai.getService(ThreadCommProcess.class).capteursOn = true;
			boolean interrupteurRehausse;
			if(args[0].equals("haut"))
				interrupteurRehausse = true;
			else if(args[0].equals("bas"))
				interrupteurRehausse = false;
			else
			{
				System.out.println("Paramètres possible : droite, gauche");
				return;
			}
			robot.updateScore(42);
			ScriptDomotiqueV2 s = new ScriptDomotiqueV2(log, robot, table, interrupteurRehausse, cp, false);
			s.execute();
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
