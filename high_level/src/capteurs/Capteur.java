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

package capteurs;

import java.awt.Graphics;
import config.Config;
import config.ConfigInfo;
import graphic.Fenetre;
import graphic.printable.Layer;
import graphic.printable.Printable;
import robot.Cinematique;
import robot.RobotReal;
import utils.Vec2RO;
import utils.Vec2RW;

/**
 * Un capteur de proximité du robot
 * 
 * @author pf
 *
 */

public abstract class Capteur implements Printable
{
	public boolean sureleve;
	protected final Vec2RO positionRelative;
	protected final double orientationRelative;
	public final double angleCone; // angle du cône (en radians)
	public final int portee;
	public final int distanceMin;
	protected int L, d;
	protected Vec2RO centreRotationGauche, centreRotationDroite;
	protected double orientationRelativeRotate;
	protected Vec2RW positionRelativeRotate;
	private TypeCapteur type;

	public Capteur(Config config, Vec2RO positionRelative, double orientationRelative, TypeCapteur type, boolean sureleve)
	{
		this.type = type;
		this.positionRelative = positionRelative;
		this.orientationRelative = orientationRelative;
		positionRelativeRotate = new Vec2RW();
		this.angleCone = type.angleCone;
		this.distanceMin = type.distanceMin;
		this.portee = type.portee;
		this.sureleve = sureleve;

		L = config.getInt(ConfigInfo.CENTRE_ROTATION_ROUE_X);
		d = config.getInt(ConfigInfo.CENTRE_ROTATION_ROUE_Y);
		centreRotationGauche = new Vec2RO(L, d);
		centreRotationDroite = new Vec2RO(L, -d);
	}

	/**
	 * Orientation donnée par le bas niveau
	 * 
	 * @param c
	 * @param angleRoueGauche
	 * @param angleRoueDroite
	 */
	public abstract void computePosOrientationRelative(Cinematique c, double angleRoueGauche, double angleRoueDroite);

	@Override
	public void print(Graphics g, Fenetre f, RobotReal robot)
	{
		if(robot.isCinematiqueInitialised())
		{
			double orientation = robot.getCinematique().orientationReelle;
			computePosOrientationRelative(robot.getCinematique(), robot.getAngleRoueGauche(), robot.getAngleRoueDroite());
			Vec2RW p1 = positionRelativeRotate.clone();
			p1.rotate(orientation);
			p1.plus(robot.getCinematique().getPosition());
			Vec2RW p2 = positionRelativeRotate.clone();
			p2.plus(new Vec2RO(portee, angleCone + orientationRelativeRotate, false));
			p2.rotate(orientation);
			p2.plus(robot.getCinematique().getPosition());
			Vec2RW p3 = positionRelativeRotate.clone();
			p3.plus(new Vec2RO(portee, -angleCone + orientationRelativeRotate, false));
			p3.rotate(orientation);
			p3.plus(robot.getCinematique().getPosition());
			int[] x = new int[3];
			x[0] = f.XtoWindow(p1.getX());
			x[1] = f.XtoWindow(p2.getX());
			x[2] = f.XtoWindow(p3.getX());
			int[] y = new int[3];
			y[0] = f.YtoWindow(p1.getY());
			y[1] = f.YtoWindow(p2.getY());
			y[2] = f.YtoWindow(p3.getY());
			g.setColor(type.couleurTransparente);
			g.fillPolygon(x, y, 3);
			g.setColor(type.couleur);
			g.drawPolygon(x, y, 3);
		}
	}

	@Override
	public Layer getLayer()
	{
		return Layer.FOREGROUND;
	}

}
