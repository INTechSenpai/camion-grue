package senpai.scripts;

import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Table;

/**
 * Script du panneau domotique
 * @author pf
 *
 */

public class ScriptDomotique extends Script
{
	private XY_RW positionEntree = new XY_RW(370,1780);
	
	public ScriptDomotique(Log log, boolean symetrie)
	{
		super(log);
		if(symetrie)
			positionEntree.setX(- positionEntree.getX());
	}

	@Override
	public XYO getPointEntree()
	{
		return new XYO(positionEntree, -Math.PI / 2);
	}

	@Override
	protected void run(Robot robot, Table table) throws InterruptedException, UnableToMoveException, ActionneurException
	{
		try {
			robot.avance(-100, 200);
		} catch(UnableToMoveException e)
		{
			// OK
		}
		robot.setDomotiqueDone();
		robot.avance(300, 800);
	}

}
