package senpai;

import java.awt.Color;

import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.kraken.exceptions.TimeoutException;
import pfg.kraken.robot.Cinematique;
import pfg.log.Log;
import senpai.Senpai.ErrorCode;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.scripts.Script;
import senpai.scripts.ScriptDomotique;
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
 * Permet de lancer facilement un test
 * @author pf
 *
 */

public class TestDomotique
{
	public static void main(String[] args)
	{
		String configfile = "senpai-trajectory.conf";
		

		Senpai senpai = new Senpai();
		ErrorCode error = ErrorCode.NO_ERROR;
		try
		{			
			senpai = new Senpai();
			senpai.initialize(configfile, "default", "graphic");
			senpai.getService(ThreadCommProcess.class).capteursOn = true;
			Robot robot = senpai.getService(Robot.class);
			Table table = senpai.getService(Table.class);
			Log log = senpai.getService(Log.class);
			GraphicDisplay buffer = senpai.getService(GraphicDisplay.class);
			robot.updateColorAndSendPosition(RobotColor.VERT);
			table.updateCote(true);
			Script script = new ScriptDomotique(log, true);
			buffer.addPrintable(new Cinematique(script.getPointEntree()), Color.BLUE, Layer.FOREGROUND.layer);
			boolean restart;
			do {
				try {
					restart = false;
					robot.goTo(script.getPointEntree());
					System.out.println("On est arrivé !");
				}
				catch(UnableToMoveException | TimeoutException e)
				{
					System.out.println("On a eu un problème : "+e);
					restart = true;
				}
			} while(restart);
			
			script.execute(robot, table);
		}
		catch(Exception e)
		{
			e.printStackTrace();
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
