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

package senpai.scripts;

import pfg.kraken.utils.XYO;
import pfg.log.Log;
import senpai.capteurs.CapteursProcess;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.ScriptException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Table;
import senpai.utils.Severity;
import senpai.utils.Subject;

/**
 * Script abstrait
 * 
 * @author pf
 *
 */

public abstract class Script
{
	protected Log log;
	protected Robot robot;
	protected Table table;
	protected CapteursProcess cp;
	
	public Script(Log log, Robot robot, Table table, CapteursProcess cp)
	{
		this.log = log;
		this.robot = robot;
		this.table = table;
		this.cp = cp;
	}
	
	public abstract boolean faisable();
	
	public abstract XYO getPointEntree();

	protected abstract void run() throws InterruptedException, UnableToMoveException, ActionneurException, ScriptException;

	public void execute() throws ScriptException, InterruptedException
	{
		log.write("Début de l'exécution de "+this, Subject.SCRIPT);
		robot.beginScript();
		try
		{
			run();
			log.write("Fin de l'exécution de " + getClass().getSimpleName(), Subject.SCRIPT);
		}
		catch(ScriptException | UnableToMoveException | ActionneurException e)
		{
			log.write("Erreur lors de l'exécution du script " + this + " : " + e, Severity.CRITICAL, Subject.SCRIPT);
			try {
				// lâche tout (s'il y a) et rentre le bras
				robot.rangeBras();
			} catch (ActionneurException e1) {
				log.write("Erreur lors de l'exécution du script " + this + " : " + e, Severity.CRITICAL, Subject.SCRIPT);
			}
			throw new ScriptException(e.getMessage());
		}
		finally
		{
			robot.endScript();
		}
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o != null && o.toString().equals(toString());
	}

}
