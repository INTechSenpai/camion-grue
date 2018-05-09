package senpai.scripts;

import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.capteurs.CapteursCorrection;
import senpai.capteurs.CapteursProcess;
import senpai.comm.CommProtocol.LLCote;
import senpai.exceptions.ActionneurException;
import senpai.exceptions.UnableToMoveException;
import senpai.robot.Robot;
import senpai.table.Table;

/**
 * Script de dépose
 * @author pf
 *
 */

public class ScriptDeposeCube extends Script
{
	private int taillePile;
	private XY positionPile;
	private double angleDepose;
	private boolean coteDroit;
	private double longueurGrue;
	private int distanceToScript;
	private double angleGrue;
	
	public ScriptDeposeCube(Log log, Robot robot, Table table, CapteursProcess cp, int taillePile, XY positionPile, double angleDepose, boolean coteDroit, double longueurGrue, int distanceToScript)
	{
		super(log, robot, table, cp);
		this.distanceToScript = distanceToScript;
		this.taillePile = taillePile;
		this.positionPile = positionPile;
		this.angleDepose = angleDepose;
		this.coteDroit = coteDroit;
		this.longueurGrue = longueurGrue;
		if(taillePile >= 3)
			angleGrue = Math.PI / 2;
		else
			angleGrue = 15. * Math.PI / 180.;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName()+" dans la pile positionnée en "+positionPile+", taille "+taillePile;
	}

	
	@Override
	public XYO getPointEntree()
	{
		if(coteDroit)
		{
			XY_RW position = new XY_RW(longueurGrue, angleDepose, true).plus(positionPile);
			double angle = angleDepose - Math.PI / 2 - angleGrue;
			position.plus(new XY(50+distanceToScript, angle, true));
			return new XYO(position, angle);
		}
		else
		{
			XY_RW position = new XY_RW(longueurGrue, angleDepose, true).plus(positionPile);
			double angle = angleDepose + Math.PI / 2 + angleGrue;
			position.plus(new XY(50+distanceToScript, angle, true));
			return new XYO(position, angle);
		}

	}

	@Override
	protected void run() throws InterruptedException, UnableToMoveException, ActionneurException
	{
		if(distanceToScript != 0)
			robot.avance(-distanceToScript, 0.2);
		try {
			cp.startStaticCorrection(CapteursCorrection.AVANT, CapteursCorrection.ARRIERE);
			robot.poseCubes(coteDroit ? - Math.PI / 2 + angleGrue : Math.PI / 2 - angleGrue);
			table.enableObstaclePile();
			robot.rangeBras(LLCote.AU_PLUS_VITE);
			cp.endStaticCorrection();
		} finally {
			if(distanceToScript != 0)
				robot.avance(distanceToScript, 0.2);
		}
	}
	
	@Override
	public boolean faisable()
	{
		return robot.canDropCube() && !robot.isPileFull();
	}

}
