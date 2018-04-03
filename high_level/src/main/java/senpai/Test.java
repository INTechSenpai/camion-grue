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

import java.util.LinkedList;
import pfg.config.Config;
import pfg.kraken.Kraken;
import pfg.kraken.exceptions.NoPathException;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.log.Log;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.CommProtocol;
import senpai.comm.DataTicket;
import senpai.comm.Ticket;
import senpai.robot.Robot;
import senpai.robot.RobotColor;

/**
 * Test rapide
 * @author pf
 *
 */

public class Test {

	public static void main(String[] args) throws InterruptedException
	{
		Senpai senpai = new Senpai("match.conf", "default", "graphic", "test", "noLL");
		try {
			Log log = senpai.getService(Log.class);
			Config config = senpai.getService(Config.class);
			OutgoingOrderBuffer data = senpai.getService(OutgoingOrderBuffer.class);
			Robot robot = senpai.getService(Robot.class);
			Kraken kraken = senpai.getService(Kraken.class);
			
			log.write("Initialisation des actionneurs…", Subject.STATUS);
			robot.initActionneurs();
			
			boolean simuleComm = config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);
			RobotColor couleur;
			if(!simuleComm)
			{
				DataTicket etat;
				do
				{
					// Demande la couleur toute les 100ms et s'arrête dès qu'elle est connue
					Ticket tc = data.demandeCouleur();
					etat = tc.attendStatus();
					Thread.sleep(100);
				} while(etat.status != CommProtocol.State.OK);
				couleur = (RobotColor) etat.data;
			}
			else
				couleur = RobotColor.ORANGE;
			
			robot.updateColorAndSendPosition(couleur, config);
		
			kraken.initializeNewSearch(new XYO(robot.getCinematique().getPosition().clone(), robot.getCinematique().orientationReelle), new XY(0, 1000));
			LinkedList<ItineraryPoint> path = kraken.search();
			Cinematique c = robot.getCinematique().clone();
		
			for(ItineraryPoint p : path)
			{
				c.updateReel(p.x, p.y, p.orientation, p.goingForward, p.curvature);
				robot.setCinematique(c);
				Thread.sleep(100);
			}
			
			Thread.sleep(5000);
			System.out.println("Code du match !");
		}
		catch(PathfindingException e)
		{
			Thread.sleep(5000);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			senpai.destructor();
		}
	}
	
}
