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

package senpai.scripts;

import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.capteurs.CapteursCorrection;
import senpai.capteurs.CapteursProcess;
import senpai.capteurs.CapteursRobot;
import senpai.comm.CommProtocol.Id;
import senpai.comm.CommProtocol.LLCote;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.ScriptException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Table;
import senpai.utils.Subject;

/**
 * Script du panneau domotique v2
 * @author pf
 *
 */

public class ScriptDomotiqueV2 extends Script
{
	private XY_RW positionEntree = new XY_RW(370,1920-260);
	private boolean symetrie;
	private boolean interrupteurRehausse;
	
	public ScriptDomotiqueV2(Log log, Robot robot, Table table, boolean interrupteurRehausse, CapteursProcess cp, boolean symetrie)
	{
		super(log, robot, table, cp);
		this.interrupteurRehausse = interrupteurRehausse;
		this.symetrie = symetrie;
		if(symetrie)
			positionEntree.setX(- positionEntree.getX());
	}

	@Override
	public XYO getPointEntree()
	{
		return new XYO(positionEntree, Math.PI / 2);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

	@Override
	protected void run() throws InterruptedException, UnableToMoveException, ActionneurException, ScriptException
	{		
		try
		{
			Integer distanceBrute = cp.getLast()[CapteursRobot.ToF_AVANT.ordinal()];
			if(distanceBrute == null)
				throw new ScriptException("Pas de mesure du capteur avant !");
			
			double distance = distanceBrute * Math.cos(robot.getCinematique().orientationReelle - Math.PI / 2);
			log.write("Distance à l'avant : "+distance, Subject.SCRIPT);
			if(distance > 100)
			{
				robot.avance(distance - 80);
				distanceBrute = cp.getLast()[CapteursRobot.ToF_AVANT.ordinal()];
				if(distanceBrute == null)
					throw new ScriptException("Pas de mesure du capteur avant !");
				
				distance = distanceBrute * Math.cos(robot.getCinematique().orientationReelle - Math.PI / 2);
			}
			else if(distance < 60)
			{
				robot.avance(distance - 80);
				distanceBrute = cp.getLast()[CapteursRobot.ToF_AVANT.ordinal()];
				if(distanceBrute == null)
					throw new ScriptException("Pas de mesure du capteur avant !");
				
				distance = distanceBrute * Math.cos(robot.getCinematique().orientationReelle - Math.PI / 2);
			}
			if(distance > 100 || distance < 60)
				throw new ScriptException("Mauvaise distance pour panneau domotique : "+distance);
		
			
			double angle;
			if(interrupteurRehausse)
				angle = -0.0025*distance+0.3;
			else
				angle = -0.0025*distance+0.25;
			
			log.write("Angle domotique : "+angle, Subject.SCRIPT);
			cp.startStaticCorrection(CapteursCorrection.AVANT);
			if(symetrie)
				robot.execute(Id.ARM_PUSH_BUTTON, angle, LLCote.PAR_LA_GAUCHE);
			else
				robot.execute(Id.ARM_PUSH_BUTTON, angle, LLCote.PAR_LA_DROITE);
			robot.setDomotiqueDone();
			if(symetrie)
				robot.rangeBras(LLCote.PAR_LA_GAUCHE);
			else
				robot.rangeBras(LLCote.PAR_LA_DROITE);
			cp.endStaticCorrection();
		} finally {
			robot.avance(-100);
		}
	}

	@Override
	public boolean faisable()
	{
		return !robot.isDomotiqueDone() && !robot.isThereCubeTop();
	}
}
