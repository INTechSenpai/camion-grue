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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import pfg.config.Config;
import pfg.graphic.AbstractPrintBuffer;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.container.DynamicObstacles;
import pfg.log.Log;
import senpai.ConfigInfoSenpai;

/**
 * Mémorise tous les obstacles mobiles qu'on a rencontré jusque là.
 * Il y a un mécanisme de libération de mémoire transparent.
 * 
 * @author pf
 *
 */

public class ObstaclesMemory implements DynamicObstacles
{
	// Les obstacles mobiles, c'est-à-dire des obstacles de proximité
	private volatile LinkedList<ObstacleProximity> listObstaclesMobiles = new LinkedList<ObstacleProximity>();
	private volatile LinkedList<ObstacleProximity> listObstaclesMortsTot = new LinkedList<ObstacleProximity>();
	private int dureeAvantPeremption;
	private volatile int size = 0;
	private volatile int indicePremierObstacle = 0;
	private volatile int firstNotDeadNow = 0;
	private volatile long nextDeathDate = Long.MAX_VALUE;
	private boolean printProx;
	private boolean printDStarLite;
	private final int tempsAvantSuppression = 2000;

	protected Log log;
	private AbstractPrintBuffer buffer;

	public ObstaclesMemory(Log log, AbstractPrintBuffer buffer, Config config)
	{
		this.log = log;
		this.buffer = buffer;
		dureeAvantPeremption = config.getInt(ConfigInfoSenpai.DUREE_PEREMPTION_OBSTACLES);
//		printProx = config.getBoolean(ConfigInfo.GRAPHIC_PROXIMITY_OBSTACLES);
//		printDStarLite = config.getBoolean(ConfigInfo.GRAPHIC_D_STAR_LITE);
	}

	public synchronized ObstacleProximity add(Obstacle obstacle)
	{
		return add(obstacle, System.currentTimeMillis());
	}

	private synchronized ObstacleProximity add(Obstacle obstacleParam, long date)
	{
		ObstacleProximity obstacle = null;//new ObstacleProximity(obstacleParam, date + dureeAvantPeremption);
		listObstaclesMobiles.add(obstacle);

		if(printProx)
			buffer.addSupprimable(obstacle);

		size++;
		return obstacle;
	}

	public synchronized int size()
	{
		return size;
	}

	public synchronized ObstacleProximity getObstacle(int nbTmp)
	{
		if(nbTmp < indicePremierObstacle)
		{
			// log.critical("Erreur : demande d'un vieil obstacle : "+nbTmp);
			return null;
		}
		return listObstaclesMobiles.get(nbTmp - indicePremierObstacle);
	}

	/**
	 * Supprime cet obstacle
	 * 
	 * @param indice
	 */
	public synchronized void remove(int indice)
	{
		ObstacleProximity o = listObstaclesMobiles.get(indice - indicePremierObstacle);

		if(printProx)
		{
			buffer.removeSupprimable(o);
		}

		listObstaclesMortsTot.add(o);
		listObstaclesMobiles.set(indice - indicePremierObstacle, null);

		/**
		 * Mise à jour de firstNotDeadNow
		 */
		firstNotDeadNow -= indicePremierObstacle;

		// on reprend où en était firstNotDeadNow, et on l'avance tant qu'il y a
		// des null devant lui
		while(firstNotDeadNow < listObstaclesMobiles.size() && listObstaclesMobiles.get(firstNotDeadNow) == null)
			firstNotDeadNow++;

		firstNotDeadNow += indicePremierObstacle;
	}

	/**
	 * Renvoie vrai s'il y a effectivement suppression.
	 * On conserve les obstacles récemment périmés, car le DStarLite en a
	 * besoin.
	 * Une recherche dichotomique ne serait pas plus efficace car on oublie peu
	 * d'obstacles à la fois
	 * 
	 * @return
	 */
	public synchronized List<Obstacle> deleteOldObstacles()
	{
		long dateActuelle = System.currentTimeMillis();
		int firstNotDeadNowSave = firstNotDeadNow;

		ObstacleProximity o = null;

		nextDeathDate = Long.MAX_VALUE;
		// firstNotDeadNow = indicePremierObstacle;
		Iterator<ObstacleProximity> iter = listObstaclesMobiles.iterator();

		int last = -1; // dernier indice assez vieux pour être détruit
		int tmp = 0;

		/**
		 * Suppression de la liste des obstacles très vieux.
		 * On supprime tous les obstacles (null y compris) jusqu'au dernier très
		 * vieux obstacle
		 */
		while(iter.hasNext() && ((o = iter.next()) == null || o.isDestructionNecessary(dateActuelle - tempsAvantSuppression)))
		{
			if(o != null) // s'il n'est pas null, c'est qu'il est très vieux
				last = tmp;
			tmp++;
		}

		iter = listObstaclesMobiles.iterator();
		tmp = 0;
		while(iter.hasNext() && tmp <= last)
		{
			indicePremierObstacle++;
			o = iter.next();
			iter.remove();
			tmp++;
		}

		// Mise à jour de firstNotDeadNow
		iter = listObstaclesMobiles.iterator();
		firstNotDeadNow = indicePremierObstacle;
		while(iter.hasNext() && ((o = iter.next()) == null || o.isDestructionNecessary(dateActuelle)))
		{
			firstNotDeadNow++;
			if(printProx && o != null)
			{
				buffer.removeSupprimable(o);
			}
		}

		if(o != null && o.getDeathDate() > dateActuelle)
			nextDeathDate = o.getDeathDate();

		// TODO
		return null;
//		return firstNotDeadNow != firstNotDeadNowSave;
	}

	public synchronized long getNextDeathDate()
	{
		return nextDeathDate;
	}

	public synchronized int getFirstNotDeadNow()
	{
		return firstNotDeadNow;
	}

	/**
	 * Il s'agit forcément d'une date du futur
	 * 
	 * @param firstNotDead
	 * @param date
	 * @return
	 */
	public boolean isDestructionNecessary(int indice, long date)
	{
		return indice < firstNotDeadNow || listObstaclesMobiles.get(indice - indicePremierObstacle) == null || listObstaclesMobiles.get(indice - indicePremierObstacle).isDestructionNecessary(date);
	}

	/**
	 * Permet de récupérer les obstacles morts prématurément
	 * 
	 * @return
	 */
	public synchronized ObstacleProximity pollMortTot()
	{
		return listObstaclesMortsTot.poll();
	}

	@Override
	public Iterator<Obstacle> getFutureDynamicObstacles(long date)
	{
		ObstaclesIteratorFutur iter = new ObstaclesIteratorFutur(log, this);
		iter.init(date, firstNotDeadNow); // TODO vérifier
		return iter;
	}

	@Override
	public Iterator<Obstacle> getCurrentDynamicObstacles()
	{
		ObstaclesIteratorPresent iter = new ObstaclesIteratorPresent(log, this);
		iter.reinit();
		return iter;
	}

}
