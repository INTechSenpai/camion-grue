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
import java.util.List;

import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.printable.Layer;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import senpai.Senpai.ErrorCode;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.capteurs.CapteursRobot;
import senpai.comm.CommProtocol;
import senpai.comm.DataTicket;
import senpai.comm.Ticket;
import senpai.obstacles.ObstaclesDynamiques;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.scripts.Script;
import senpai.scripts.ScriptManager;
import senpai.table.Table;
import senpai.utils.ConfigInfoSenpai;

/**
 * Test rapide
 * @author pf
 *
 */

public class TestDeplacement {

	public static void main(String[] args) throws InterruptedException
	{
		ErrorCode error = ErrorCode.NO_ERROR;
		Senpai senpai = new Senpai();
		try {
			senpai.initialize("match.conf", "default", "graphic", "test", "noLL");
//			Log log = senpai.getService(Log.class);
			Config config = senpai.getService(Config.class);
			config.override(ConfigInfoSenpai.SIMULE_COMM, true);
			Thread.sleep(config.getInt(ConfigInfoSenpai.WARM_UP_DURATION));
			OutgoingOrderBuffer data = senpai.getService(OutgoingOrderBuffer.class);
			Robot robot = senpai.getService(Robot.class);
			Table table = senpai.getService(Table.class);
			GraphicDisplay buffer = senpai.getService(GraphicDisplay.class);
			ScriptManager scripts = senpai.getService(ScriptManager.class);
			ObstaclesDynamiques od = senpai.getService(ObstaclesDynamiques.class);
			
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
			
			robot.updateColorAndSendPosition(couleur, true);
			table.updateCote(couleur.symmetry);
			scripts.setCouleur(couleur);
			//XYO destination = new XYO(0, 1000, Math.PI);
//			Script script = new ScriptPriseCube(Croix.CROIX_HAUT_DROITE, CubeColor.ORANGE, CubeFace.GAUCHE, false);
//			PriorityQueue<ScriptPriseCube> all = scripts.getAllPossible(CubeColor.ORANGE, false);
//			XYO destination = new ScriptPriseCube(0, ElementColor.BLEU, ScriptPriseCube.Face.BAS, false).getPointEntree();

			CapteursRobot.ToF_COIN_AVANT_DROIT.updateObstacle(new XY(-150.84,1543.50), 0);
			od.update(CapteursRobot.ToF_COIN_AVANT_DROIT);
			
//			ObstacleProximity obs = new ObstacleProximity(new XY(-150.84,1543.50), 100, 100, 0, 0, null, 0);
//			buffer.addPrintable(CapteursRobot.ToF_COIN_AVANT_DROIT.current, Color.RED, Layer.FOREGROUND.layer);
//			mem.add(obs);

			Cinematique init = robot.getCinematique().clone();
			
			Script script = scripts.getDeposeUnCubeScript(1);
			XYO destination = script.getPointEntree();
			buffer.addPrintable(new Cinematique(destination), Color.BLUE, Layer.FOREGROUND.layer);
			init.copy(robot.getCinematique());
			DataTicket dt = robot.goTo(destination);
				
			/*Cinematique c = */robot.getCinematique();//.clone();
		
			// Ceci ne fonctionne qu'avec la simulation du LL !
			@SuppressWarnings("unchecked")
			List<ItineraryPoint> path = (List<ItineraryPoint>) dt.data;
			for(ItineraryPoint p : path)
				buffer.addPrintable(p, p.stop ? Color.BLUE : Color.BLACK, Layer.FOREGROUND.layer);
			buffer.refresh();
/*	
				for(ItineraryPoint p : path)
				{
					System.out.println(p);
					c.enMarcheAvant = p.goingForward;
					c.updateReel(p.x, p.y, p.orientation, p.curvature);
	//				robot.setCinematique(c);
					buffer.refresh();
					if(p.stop)
						Thread.sleep(150);
					else
						Thread.sleep(Math.min(150, Math.round(50./p.possibleSpeed)));
				}
				
				Thread.sleep(1000);
				script.execute(robot, table);
//				if(i == 0)
//					robot.setDegrade();*/
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
