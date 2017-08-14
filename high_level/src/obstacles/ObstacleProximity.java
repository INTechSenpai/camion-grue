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

package obstacles;

import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.utils.XY;
import senpai.CouleurSenpai;
import capteurs.CapteursRobot;
import capteurs.SensorsData;

/**
 * Obstacles détectés par capteurs de proximité (ultrasons et infrarouges)
 * 
 * @author pf
 */
public class ObstacleProximity extends RectangularObstacle
{
	private static final long serialVersionUID = -3518004359091355796L;
	private long death_date;
	public final SensorsData mesureOrigine;
	public final int nbCapteurOrigine;

	public ObstacleProximity(XY position, int sizeX, int sizeY, double angle, CouleurSenpai c, long death_date, SensorsData mesureOrigine, int nbCapteurOrigine)
	{
		super(position, sizeX, sizeY, angle, c.couleur, c.l);
		this.mesureOrigine = mesureOrigine;
		this.nbCapteurOrigine = nbCapteurOrigine;
		this.death_date = death_date;
	}

	@Override
	public String toString()
	{
		return "Obstacle de proximité en " + position + ", né à " + mesureOrigine.dateCreation + "ms, meurt à " + death_date + "ms, origine : "+CapteursRobot.values[nbCapteurOrigine]+" (mesure : "+mesureOrigine.mesures[nbCapteurOrigine]+")";
	}

	public boolean isDestructionNecessary(long date)
	{
		return death_date < date;
	}

	public long getDeathDate()
	{
		return death_date;
	}

	/**
	 * Renvoi "vrai" si position est à moins de distance du centre de l'obstacle
	 * 
	 * @param position
	 * @param distance
	 * @return
	 */
	public boolean isProcheCentre(XY position, int distance)
	{
		return squaredDistance(position) < distance * distance;
	}
	
	public boolean isHorsTable()
	{
		return isHorsTable(coinBasGaucheRotate) ||
				isHorsTable(coinBasDroiteRotate) || 
				isHorsTable(coinHautGaucheRotate) || 
				isHorsTable(coinHautDroiteRotate);
	}
	
	private boolean isHorsTable(XY point)
	{
		return point.getX() < -1500 || point.getX() > 1500 || point.getY() < 0 || point.getY() > 2000;
	}
}
