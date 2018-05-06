package senpai;

import pfg.config.Config;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.utils.XYO;
import pfg.log.Log;
import senpai.Senpai.ErrorCode;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.CommProtocol;
import senpai.comm.DataTicket;
import senpai.comm.Ticket;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.robot.RobotColor;
import senpai.scripts.Script;
import senpai.scripts.ScriptManager;
import senpai.scripts.ScriptRecalage;
import senpai.table.CubeColor;
import senpai.table.Table;
import senpai.threads.comm.ThreadCommProcess;
import senpai.utils.ConfigInfoSenpai;
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
 * Match !
 * @author pf
 *
 */

public class Match
{
	private static Senpai senpai = new Senpai();
	private OutgoingOrderBuffer ll;
	private Robot robot;
	private Table table;
	private ScriptManager scripts;
	private Log log;
	private Config config;

	public static void main(String[] args)
	{
		ErrorCode error = ErrorCode.NO_ERROR;
		try {
			CubeColor[] pattern = null;
			if(args.length == 3)
			{
				pattern = CubeColor.parsePattern(args);
				if(pattern != null)
					System.out.println("Pattern lu : "+pattern[0]+" "+pattern[1]+" "+pattern[2]);
			}
			new Match().exec(pattern);
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
	
	public void exec(CubeColor[] pattern) throws InterruptedException
	{
		String configfile = "match.conf";
		
		senpai = new Senpai();
		senpai.initialize(configfile, "default", "graphic");
		config = senpai.getService(Config.class);
		ll = senpai.getService(OutgoingOrderBuffer.class);
		robot = senpai.getService(Robot.class);
		table = senpai.getService(Table.class);
		scripts = senpai.getService(ScriptManager.class);
		log = senpai.getService(Log.class);
		if(pattern != null)
			scripts.setPattern(pattern);
		
		RobotColor couleur;

		/*
		 * Attente de la couleur
		 */

		if(config.getBoolean(ConfigInfoSenpai.DISABLE_JUMPER))
		{
			couleur = RobotColor.ORANGE;
			robot.setDateDebutMatch();
		}
		else
		{
			DataTicket etat;
			do
			{
				// Demande la couleur toute les 100ms et s'arrête dès qu'elle est connue
				Ticket tc = ll.demandeCouleur();
				etat = tc.attendStatus();
				Thread.sleep(100);
			} while(etat.status != CommProtocol.State.OK);
			couleur = (RobotColor) etat.data;
		}

		robot.updateColorAndSendPosition(couleur);
		table.updateCote(couleur.symmetry);
		scripts.setCouleur(couleur);
		
		/*
		 * Allumage des capteurs
		 */
		senpai.getService(ThreadCommProcess.class).capteursOn = true;
		
		
		/*
		 * Recalage initial
		 */
		ScriptRecalage rec = scripts.getScriptRecalage(2000);
		try {
			rec.execute();
		} catch (UnableToMoveException | ActionneurException e) {
			// IMPOSSIBRU
			e.printStackTrace();
		}
		
		XYO initialCorrection = rec.getCorrection();
		System.out.println(initialCorrection);
		double deltaX = Math.round(initialCorrection.position.getX())/10.;
		double deltaY = Math.round(initialCorrection.position.getY())/10.;
		double orientation = initialCorrection.orientation * 180. / Math.PI;
		log.write("Je suis "+Math.abs(deltaX)+"cm sur la "+(deltaX < 0 ? "droite" : "gauche"), Subject.STATUS);
		log.write("Je suis "+Math.abs(deltaY)+"cm vers l'"+(deltaY < 0 ? "avant" : "arrière"), Subject.STATUS);
		log.write("Je suis orienté vers la "+(orientation < 0 ? "droite" : "gauche")+" de "+Math.abs(orientation)+"°", Subject.STATUS);
		
		try {
			doScript(scripts.getScriptAbeille(), 5);
		} catch (PathfindingException | UnableToMoveException | ActionneurException e) {
			log.write("Erreur : "+e, Subject.SCRIPT);
		}
		
		robot.printTemps();

		try {
			doScript(scripts.getScriptDomotique(), 5);
		} catch (PathfindingException | UnableToMoveException | ActionneurException e) {
			log.write("Erreur : "+e, Subject.SCRIPT);
		}
		
		robot.printTemps();
		
		try {
			doScript(scripts.getAllPossible(false).poll(), 5);
		} catch (PathfindingException | UnableToMoveException | ActionneurException e) {
			log.write("Erreur : "+e, Subject.SCRIPT);
		}

		robot.printTemps();

		try {
			doScript(scripts.getAllPossible(false).poll(), 5);
		} catch (PathfindingException | UnableToMoveException | ActionneurException e) {
			log.write("Erreur : "+e, Subject.SCRIPT);
		}

	}
	
	private void doScript(Script s, int nbEssaiChemin) throws PathfindingException, InterruptedException, UnableToMoveException, ActionneurException
	{
		boolean restart;
		do {
			try {
				restart = false;
				robot.goTo(s.getPointEntree());
			}
			catch(UnableToMoveException e)
			{
				restart = true;
				nbEssaiChemin--;
			}
		} while(restart && nbEssaiChemin > 0);
		
		if(!restart)
			s.execute();
		else
			log.write("On annule l'exécution du script "+s, Subject.SCRIPT);
	}
}
