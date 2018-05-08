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

package senpai.table;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import pfg.config.Config;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.GraphicPanel;
import pfg.graphic.printable.Layer;
import pfg.graphic.printable.Printable;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.utils.ConfigInfoSenpai;
import senpai.utils.Subject;

/**
 * Gère les éléments de jeux
 * 
 * @author pf
 *
 */

public class Table implements Printable
{
	private static final long serialVersionUID = 1L;

	// Dépendances
	protected transient Log log;

	private HashMap<Cube, Boolean> etat = new HashMap<Cube, Boolean>();
	private List<Obstacle> currentObstacles = new ArrayList<Obstacle>();
	private double pileOrientation;
	
	private RectangularObstacle obstaclePilesGrossi;
	private RectangularObstacle obstaclePiles;
	private boolean pileActivees;
	private XY_RW pilePosition;
	
	public Table(Log log, Config config, GraphicDisplay buffer)
	{
		this.log = log;
		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ENABLE))
			buffer.addPrintable(this, Color.BLACK, Layer.BACKGROUND.layer);
		for(Cube n : Cube.values())
			etat.put(n, false);

		pileActivees = false;
		pilePosition = new XY_RW(config.getDouble(ConfigInfoSenpai.PILE_1_X),config.getDouble(ConfigInfoSenpai.PILE_1_Y));
		pileOrientation = config.getDouble(ConfigInfoSenpai.PILE_1_O);
		setDone(Cube.GOLDEN_CUBE_1);
		setDone(Cube.GOLDEN_CUBE_2);
	}

	public void updateCote(boolean symetrie)
	{
		for(Cube c : Cube.values())
			if(!isDone(c) && c.position.getX() > 0 == symetrie)
				setDone(c);
		
		if(symetrie)
		{
			pilePosition.setX(-pilePosition.getX());
			pileOrientation = Math.PI - pileOrientation;
		}
		
		obstaclePilesGrossi = new RectangularObstacle(pilePosition, 58+30, 58+30, pileOrientation);
		obstaclePiles = new RectangularObstacle(pilePosition, 58, 58, pileOrientation);
	}
	
	/**
	 * On a pris l'objet, on est passé dessus, le robot ennemi est passé
	 * dessus...
	 * Attention, on ne peut qu'augmenter cette valeur.
	 * 
	 * @param id
	 */
	public void setDone(Cube id)
	{
		log.write("Cube absent de la table : "+id, Subject.TABLE);
		etat.put(id, true);
	}

	/**
	 * Cet objet est-il présent ou non?
	 * 
	 * @param id
	 */
	public boolean isDone(Cube id)
	{
		return etat.get(id);
	}

	public Iterator<Obstacle> getCurrentObstaclesIterator()
	{
		currentObstacles.clear();
		for(Cube n : Cube.values())
			if(!etat.get(n))
				currentObstacles.add(n.obstacleGrossi);
		if(pileActivees)
			currentObstacles.add(obstaclePilesGrossi);
		return currentObstacles.iterator();
	}

	public RectangularObstacle getObstaclePile()
	{
		return obstaclePiles;
	}
	
	public void enableObstaclePile()
	{
		pileActivees = true;
	}
	
	@Override
	public void print(Graphics g, GraphicPanel f)
	{
		for(Cube n : Cube.values())
			if(!etat.get(n))
			{
				g.setColor(n.couleur.color);
				n.obstacle.print(g, f);
			}
	}
}