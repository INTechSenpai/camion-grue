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

package senpai.capteurs;

import pfg.config.Config;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.utils.XY;
import senpai.robot.Robot;

/**
 * Un capteur immobile
 * 
 * @author pf
 *
 */

public class CapteurImmobile extends Capteur
{
	private static final long serialVersionUID = 1L;

	public CapteurImmobile(Robot robot, Config config, XY positionRelative, double orientationRelative, TypeCapteur type, boolean sureleve)
	{
		super(robot, config, positionRelative, orientationRelative, type, sureleve);
		orientationRelativeRotate = orientationRelative;
		positionRelativeRotate = positionRelative.clone();
	}

	@Override
	public void computePosOrientationRelative(Cinematique c, double angleRoueGauche, double angleRoueDroite)
	{}

}
