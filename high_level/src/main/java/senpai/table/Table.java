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
import pfg.kraken.obstacles.CircularObstacle;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.utils.ConfigInfoSenpai;

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

	private Obstacle[] obstaclePiles;
	private boolean[] pileActivees;
	private XY_RW[] pilePosition;
	
	public Table(Log log, Config config, GraphicDisplay buffer)
	{
		this.log = log;
		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_ENABLE))
			buffer.addPrintable(this, Color.BLACK, Layer.BACKGROUND.layer);
		for(Cube n : Cube.values())
			etat.put(n, false);

		pileActivees = new boolean[] {false, false};
		pilePosition = new XY_RW[] {
				new XY_RW(config.getDouble(ConfigInfoSenpai.PILE_1_X),config.getDouble(ConfigInfoSenpai.PILE_1_Y)),
				new XY_RW(config.getDouble(ConfigInfoSenpai.PILE_2_X),config.getDouble(ConfigInfoSenpai.PILE_2_Y))};
	}

	public void updateCote(boolean symetrie)
	{
		for(Cube c : Cube.values())
			if(c.position.getX() > 0 == symetrie)
				setDone(c);
		
		if(symetrie)
		{
			pilePosition[0].setX(-pilePosition[0].getX());
			pilePosition[1].setX(-pilePosition[1].getX());
		}
		
		obstaclePiles = new Obstacle[] {
				new CircularObstacle(pilePosition[0], 80),
				new CircularObstacle(pilePosition[1], 80)};
	}
	
	/**
	 * On a pris l'objet, on est passé dessus, le robot ennemi est passé
	 * dessus...
	 * Attention, on ne peut qu'augmenter cette valeur.
	 * Renvoie vrai si l'état de la table a changé pour le futur (?)
	 * 
	 * @param id
	 */
	public void setDone(Cube id)
	{
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
				currentObstacles.add(n.obstacle);
		if(pileActivees[0])
			currentObstacles.add(obstaclePiles[0]);
		if(pileActivees[1])
			currentObstacles.add(obstaclePiles[1]);
		return currentObstacles.iterator();
	}

	public void enableObstaclePile(int nbPile)
	{
		pileActivees[nbPile] = true;
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