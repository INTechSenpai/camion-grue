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

package senpai;

import java.awt.Color;
import java.util.LinkedList;
import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.kraken.Kraken;
import pfg.kraken.SearchParameters;
import pfg.kraken.obstacles.CircularObstacle;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.log.Log;
import senpai.Senpai.ErrorCode;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.CommProtocol;
import senpai.comm.DataTicket;
import senpai.comm.Ticket;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.scripts.ScriptPriseCube;
import senpai.table.Croix;
import senpai.table.CubeColor;
import senpai.table.CubeFace;

/**
 * Test rapide
 * @author pf
 *
 */

public class Test {

	public static void main(String[] args) throws InterruptedException
	{
		ErrorCode error = ErrorCode.NO_ERROR;
		Senpai senpai = new Senpai();
		try {
			senpai.initialize("match.conf", "default", "graphic", "test", "noLL");
			Log log = senpai.getService(Log.class);
			Config config = senpai.getService(Config.class);
			OutgoingOrderBuffer data = senpai.getService(OutgoingOrderBuffer.class);
			Robot robot = senpai.getService(Robot.class);
			Kraken kraken = senpai.getService(Kraken.class);
			GraphicDisplay buffer = senpai.getService(GraphicDisplay.class);
			
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
				couleur = RobotColor.VERT;
			
			robot.updateColorAndSendPosition(couleur);
			//XYO destination = new XYO(0, 1000, Math.PI);
//			XYO destination = new ScriptPriseCube(0, ElementColor.BLEU, ScriptPriseCube.Face.BAS, false).getPointEntree();
			XYO destination = new ScriptPriseCube(Croix.CROIX_HAUT_DROITE, CubeColor.BLEU, CubeFace.GAUCHE, true).getPointEntree();
			
//			robot.setCinematique(new Cinematique(new XYO(0,1000, 0)));
			buffer.addPrintable(new Cinematique(destination), Color.BLUE, Layer.FOREGROUND.layer);
			
			kraken.initializeNewSearch(new SearchParameters(new XYO(robot.getCinematique().getPosition().clone(), robot.getCinematique().orientationReelle), destination));
			long avant = System.currentTimeMillis();
			LinkedList<ItineraryPoint> path = kraken.search();
			System.out.println("Durée de la recherche : "+(System.currentTimeMillis() - avant));
			Cinematique c = robot.getCinematique().clone();
		
			for(ItineraryPoint p : path)
				buffer.addPrintable(p, p.stop ? Color.BLUE : Color.BLACK, Layer.FOREGROUND.layer);

			for(ItineraryPoint p : path)
			{
				System.out.println(p);
				c.updateReel(p.x, p.y, p.orientation, p.goingForward, p.curvature);
				robot.setCinematique(c);
				buffer.refresh();
				if(p.stop)
					Thread.sleep(150);
				else
					Thread.sleep(Math.round(50./p.possibleSpeed));
			}
			
			Thread.sleep(5000);
		}
		catch(Exception e)
		{
//			Thread.sleep(5000);
			error = ErrorCode.EXCEPTION;
			error.setException(e);
		}
		finally
		{
			senpai.destructor(error);
		}
	}
	
}
