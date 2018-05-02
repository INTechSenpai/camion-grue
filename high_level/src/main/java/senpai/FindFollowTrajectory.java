package senpai;

import pfg.kraken.SearchParameters;
import pfg.kraken.astar.DirectionStrategy;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import senpai.Senpai.ErrorCode;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.CommProtocol;
import senpai.comm.DataTicket;
import senpai.comm.Ticket;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.threads.comm.ThreadCommProcess;
import senpai.utils.Subject;

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

public class FindFollowTrajectory
{
	public static void main(String[] args)
	{
		if(args.length < 6 || args.length > 8)
		{
			System.out.println("Usage : ./run.sh "+FindFollowTrajectory.class.getSimpleName()+" mode vitesse_max x_depart y_depart o_depart x_arrivee y_arrivee [o_arrivee]");
			return;
		}
		
		boolean modeXY = args[0].equals("XY");
		String configfile = "senpai-trajectory.conf";
		
		double vitesseMax = Double.parseDouble(args[1]) / 1000.;
		
		double x = Double.parseDouble(args[2]);
		double y = Double.parseDouble(args[3]);
		double o = Double.parseDouble(args[4]);
		XYO depart = new XYO(x,y,o);
		
		x = Double.parseDouble(args[5]);
		y = Double.parseDouble(args[6]);
		
		SearchParameters sp;
		if(modeXY)
		{
			XY arriveeXY = new XY(x,y);
			sp = new SearchParameters(depart, arriveeXY);
		}
		else
		{
			o = Double.parseDouble(args[7]);
			XYO arriveeXYO = new XYO(x,y,o);
			sp = new SearchParameters(depart, arriveeXYO);
		}
		
		sp.setDirectionStrategy(DirectionStrategy.FASTEST);
		sp.setMaxSpeed(vitesseMax);

		Senpai senpai = new Senpai();
		ErrorCode error = ErrorCode.NO_ERROR;
		try
		{
//			Log log = new Log(Severity.INFO, configfile, "");
			
			senpai = new Senpai();
			senpai.initialize(configfile, "default");
			senpai.getService(ThreadCommProcess.class).capteursOn = true;
			OutgoingOrderBuffer data = senpai.getService(OutgoingOrderBuffer.class);

			
//			data.setScore(1337);
/*			DataTicket etat;
			do
			{
				// Demande la couleur toute les 100ms et s'arrête dès qu'elle est connue
				Ticket tc = data.demandeCouleur();
				etat = tc.attendStatus();
				Thread.sleep(100);
			} while(etat.status != CommProtocol.State.OK);
			
			System.out.println("Couleur : "+etat.data);
			data.waitForJumper().attendStatus();*/
			
			data.setPosition(sp.start.getPosition(), sp.start.orientationReelle);
			Thread.sleep(5000);
			Robot robot = senpai.getExistingService(Robot.class);
			robot.setDegrade();
			boolean restart;
			do {
				try {
					restart = false;
					robot.goTo(sp.arrival.getXYO());
					System.out.println("On est arrivé !");
				}
				catch(UnableToMoveException e)
				{
					System.out.println("On a eu un problème : "+e);
					restart = true;
				}
			} while(restart);
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
