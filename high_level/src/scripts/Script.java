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

package scripts;

import pfg.kraken.robot.Cinematique;
import pfg.log.Log;

/**
 * Script abstrait
 * 
 * @author pf
 *
 */

public abstract class Script
{
	protected Log log;
	
	public abstract Cinematique getPointEntree();

	public abstract void setUpCercleArrivee();
/*
	protected abstract void run(RealGameState state) throws InterruptedException, UnableToMoveException, ActionneurException, MemoryManagerException;

	public void execute(RealGameState state) throws InterruptedException, MemoryManagerException
	{
		log.debug("Début de l'exécution de " + getClass().getSimpleName());
		try
		{
			run(state);
			log.debug("Fin de l'exécution de " + getClass().getSimpleName());
		}
		catch(UnableToMoveException | ActionneurException e)
		{
			log.critical("Erreur lors de l'exécution du script " + getClass().getSimpleName() + " : " + e);
		}
	}*/

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o.toString().equals(toString());
	}

}
