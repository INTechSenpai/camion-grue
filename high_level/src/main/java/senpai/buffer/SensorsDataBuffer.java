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

package senpai.buffer;

import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.capteurs.SensorsData;

/**
 * Buffer qui contient les infos provenant des capteurs du LL
 * 
 * @author pf
 *
 */

public class SensorsDataBuffer extends IncomingBuffer<SensorsData>
{
	private static final long serialVersionUID = 1L;

	public SensorsDataBuffer(Log log, Config config, GraphicDisplay print)
	{
		super(log, "Buffer des capteurs");
		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_CAPTEURS_CHART))
			print.addPlottable(this);
	}
}
