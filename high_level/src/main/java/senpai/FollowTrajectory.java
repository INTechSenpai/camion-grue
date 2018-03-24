package senpai;

import java.util.List;
import pfg.injector.InjectorException;
import pfg.kraken.robot.ItineraryPoint;
import pfg.log.Log;
import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.CommProtocol.InOrder;
import senpai.comm.Ticket;

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

/**
 * Permet de lancer facilement un test
 * @author pf
 *
 */

public class FollowTrajectory
{
	public static void main(String[] args) throws InterruptedException
	{
		if(args.length != 1)
		{
			System.out.println("Usage : ./run.sh "+FollowTrajectory.class.getSimpleName()+" chemin");
			return;
		}
		
		String configfile = "senpai-trajectory.conf";
		
		String filename = args[0];
		Senpai senpai = new Senpai(configfile, "default");
		Log log = new Log(Severity.INFO, configfile, "log");
		
		List<ItineraryPoint> path = KnownPathManager.loadPath(filename);
		OutgoingOrderBuffer data;
		try
		{
			data = senpai.getService(OutgoingOrderBuffer.class);

			data.ajoutePointsTrajectoire(path);
			Ticket t = data.followTrajectory();
			InOrder state = t.attendStatus();
			log.write("Code de retour reçu : "+state, Subject.TRAJECTORY);
		}
		catch(InjectorException e)
		{
			e.printStackTrace();
		}
	}
}
