package senpai;

import java.awt.Color;

import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.kraken.exceptions.TimeoutException;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.utils.XYO;
import pfg.log.Log;
import senpai.Senpai.ErrorCode;
import senpai.capteurs.CapteursProcess;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.scripts.Script;
import senpai.scripts.ScriptDomotique;
import senpai.scripts.ScriptManager;
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
			Robot robot = senpai.getService(Robot.class);
			Table table = senpai.getService(Table.class);
			Log log = senpai.getService(Log.class);
			GraphicDisplay buffer = senpai.getService(GraphicDisplay.class);
			robot.updateColorAndSendPosition(RobotColor.VERT, true);
			senpai.getService(ThreadCommProcess.class).capteursOn = true;
			ScriptManager scripts = senpai.getService(ScriptManager.class);
			scripts.setCouleur(RobotColor.VERT);
			Script rec = scripts.getScriptRecalage();
			rec.execute();
/*			XYO initialCorrection = cp.doStaticCorrection(1000);
			double deltaX = Math.round(initialCorrection.position.getX())/10.;
			double deltaY = Math.round(initialCorrection.position.getY())/10.;
			double orientation = initialCorrection.orientation;*/
///			log.write("Je suis "+", categorie);
			table.updateCote(true);
			Script script = scripts.getScriptDomotique();
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
			
			script.execute();
			
			do {
				try {
					restart = false;
					robot.goTo(rec.getPointEntree());
					System.out.println("On est arrivé !");
				}
				catch(UnableToMoveException | TimeoutException e)
				{
					System.out.println("On a eu un problème : "+e);
					restart = true;
				}
			} while(restart);
			
			rec.execute();
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
