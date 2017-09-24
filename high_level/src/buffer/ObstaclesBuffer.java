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

package buffer;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import pfg.config.Config;
import pfg.graphic.Chart;
import pfg.graphic.GraphicPanel;
import pfg.graphic.PrintBuffer;
import pfg.graphic.printable.Printable;
import pfg.kraken.obstacles.Obstacle;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;

/**
 * Buffer qui contient les obstacles nouveaux / périmés
 * 
 * @author pf
 *
 */

public class ObstaclesBuffer implements Printable
{
	private static final long serialVersionUID = 1L;
	protected Log log;
	private boolean needToWait = true;
	
	public ObstaclesBuffer(Log log, Config config, PrintBuffer print)
	{
		this.log = log;
		if(config.getBoolean(ConfigInfoSenpai.GRAPHIC_COMM_CHART))
			print.add(this, Color.BLACK, 0);
	}

	public synchronized void clear()
	{
		notify();
		needToWait = false;
	}
	
	public synchronized boolean isNewObstaclesEmpty()
	{
		return bufferNewObstacles.isEmpty();
	}
	
	public synchronized boolean isOldObstaclesEmpty()
	{
		return bufferOldObstacles.isEmpty();
	}

	private Queue<Obstacle> bufferNewObstacles = new ConcurrentLinkedQueue<Obstacle>();
	private Queue<Obstacle> bufferOldObstacles = new ConcurrentLinkedQueue<Obstacle>();

	/**
	 * Ajout d'un élément dans le buffer et provoque un "notify"
	 * 
	 * @param elem
	 */
	public synchronized void addNewObstacle(Obstacle elem)
	{
		bufferNewObstacles.add(elem);
	}

	/**
	 * Retire un élément du buffer
	 * 
	 * @return
	 */
	public synchronized Obstacle pollNewObstacle()
	{
		needToWait = true;
		return bufferNewObstacles.poll();
	}
	
	/**
	 * Ajout d'un élément dans le buffer et provoque un "notify"
	 * 
	 * @param elem
	 */
	public synchronized void addAllOldObstacle(List<Obstacle> elem)
	{
		bufferOldObstacles.addAll(elem);
	}

	/**
	 * Retire un élément du buffer
	 * 
	 * @return
	 */
	public synchronized Obstacle pollOldObstacle()
	{
		needToWait = true;
		return bufferOldObstacles.poll();
	}

	public boolean needToWait()
	{
		return needToWait;
	}

	@Override
	public void print(Graphics g, GraphicPanel f, Chart a)
	{
		a.addData("Buffer d'obstacles nouveaux", (double) (bufferNewObstacles.size()));
		a.addData("Buffer d'obstacles anciens", (double) (bufferOldObstacles.size()));
	}
}
