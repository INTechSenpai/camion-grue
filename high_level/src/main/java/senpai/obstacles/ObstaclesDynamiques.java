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

package senpai.obstacles;

import java.util.Iterator;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.container.SmartDynamicObstacles;
import senpai.table.Table;

/**
 * Regroupe les obstacles de capteurs et de table
 * @author pf
 *
 */

public class ObstaclesDynamiques extends SmartDynamicObstacles implements Iterator<Obstacle>
{
	private Table table;
	private ObstaclesMemory memory;
	private Iterator<Obstacle> iteratorMemory;
	private Iterator<Obstacle> iteratorTable;
	
	public ObstaclesDynamiques(Table table, ObstaclesMemory memory)
	{
		this.table = table;
		this.memory = memory;
	}

	@Override
	public Iterator<Obstacle> getCurrentDynamicObstacles()
	{
		iteratorMemory = memory.getCurrentDynamicObstacles();
		iteratorTable = table.getCurrentObstaclesIterator();
		return this;
	}

	@Override
	public boolean hasNext()
	{
		if(iteratorMemory.hasNext())
			return true;
		return iteratorTable.hasNext();
	}

	@Override
	public Obstacle next()
	{
		if(iteratorMemory.hasNext())
			return iteratorMemory.next();
		return iteratorTable.next();
	}

	@Override
	protected void addObstacle(Obstacle obs)
	{
		assert obs instanceof ObstacleProximity;
		memory.add((ObstacleProximity)obs);
	}

}
