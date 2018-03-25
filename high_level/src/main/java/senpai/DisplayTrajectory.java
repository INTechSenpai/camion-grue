package senpai;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import pfg.config.ConfigInfo;
import pfg.graphic.DebugTool;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.Vec2RO;
import pfg.graphic.printable.Layer;
import pfg.kraken.robot.ItineraryPoint;
import pfg.log.Log;

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
 * Affiche une trajectoire déjà calculée
 * @author pf
 *
 */

public class DisplayTrajectory
{

	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.out.println("Usage : ./run.sh "+DisplayTrajectory.class.getSimpleName()+" chemin");
			return;
		}
		
		String configfile = "senpai-trajectory.conf";
		String filename = args[0];
		List<ItineraryPoint> path = KnownPathManager.loadPath(filename).path;

		Log log = new Log(Severity.INFO, configfile, "log");
		
		DebugTool debug = DebugTool.getDebugTool(new HashMap<ConfigInfo, Object>(), new Vec2RO(0,1000), Severity.INFO, configfile, "default", "graphic");
		GraphicDisplay display = debug.getGraphicDisplay();
		
		for(ItineraryPoint p : path)
		{
			log.write(p, Subject.STATUS);
			display.addPrintable(p, Color.BLACK, Layer.FOREGROUND.layer);
		}
		display.refresh();

	}
	
}
