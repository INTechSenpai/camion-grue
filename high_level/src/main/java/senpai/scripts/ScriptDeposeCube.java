package senpai.scripts;

import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
import pfg.log.Log;
import senpai.capteurs.CapteursCorrection;
import senpai.capteurs.CapteursProcess;
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
	private int nbPile;
	private int distanceToScript = 80;
	private double angleGrue;
	
	public ScriptDeposeCube(Log log, Robot robot, Table table, CapteursProcess cp, int taillePile, XY positionPile, double angleDepose, boolean coteDroit, double longueurGrue, int nbPile)
	{
		super(log, robot, table, cp);
		this.taillePile = taillePile;
		this.positionPile = positionPile;
		this.angleDepose = angleDepose;
		this.coteDroit = coteDroit;
		this.longueurGrue = longueurGrue;
		this.nbPile = nbPile;
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
		/*
		 * Cas où on connait le pattern : 2 piles de 3 cubes
		 * Cas ou on ne connaît pas le pattern : 1 pile de 6 cubes
		 */
		
		/*
		 * En étant bien placé (au-dessus du bloc noir), on peut prendre et poser le cube noir sans se déplacer
		 * Pour la stratégie sans pattern, on peut tenter une grande pile en exploitant ça
		 */
		
		/*
		 * Attention ! Plus on met un cube haut, plus la projection verticale de la grue sera courte. Il faut donc prendre en compte cet effet.
		 */
		
		/*
		 * Pour déposer le 4e et le 5e cube, l'angle de la grue avec le robot est nul (remplacer le +15 par un +90
		 */
		
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
		cp.startStaticCorrection(CapteursCorrection.AVANT, CapteursCorrection.ARRIERE);
		robot.avance(-distanceToScript, 0.2);
		robot.poseCubes(coteDroit ? - Math.PI / 2 + angleGrue : Math.PI / 2 - angleGrue, nbPile);
		table.enableObstaclePile(nbPile);
		robot.rangeBras();
		cp.endStaticCorrection();
	}
	
	@Override
	public boolean faisable()
	{
		return robot.canDropCube();
	}

}
