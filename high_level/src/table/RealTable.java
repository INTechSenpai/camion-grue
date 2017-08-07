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

package table;

import pfg.config.Config;
import pfg.log.Log;

/**
 * La "vraie" table
 * 
 * @author pf
 *
 */

public class RealTable extends Table
{
	private boolean print;
	private long lastEtatTableDStarLite = 0;
	private boolean lastShoot = true;

	public RealTable(Log log, Config config)
	{
		super(log);

/*		print = config.getBoolean(ConfigInfo.GRAPHIC_GAME_ELEMENTS);
		if(print)
			for(GameElementNames g : GameElementNames.values())
				buffer.addSupprimable(g.obstacle);*/
	}

	/**
	 * Met à jour l'affichage en plus
	 */
	@Override
	public synchronized boolean setDone(GameElementNames id, EtatElement done)
	{
/*		if(done.hash > isDone(id).hash)
			log.debug("Changement état de " + id + " : " + done);
		if(print && done.hash > EtatElement.INDEMNE.hash)
			buffer.removeSupprimable(id.obstacle);*/
		return super.setDone(id, done);
	}
}
