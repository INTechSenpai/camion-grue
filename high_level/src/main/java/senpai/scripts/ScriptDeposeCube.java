package senpai.scripts;

import pfg.kraken.utils.XY;
import pfg.kraken.utils.XYO;
import pfg.kraken.utils.XY_RW;
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
	private double[] longueurGrue = new double[]{300, 300, 290, 365, 365}; // longueur de la grue en fonction du nombre de cube déjà posés
	
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
		
/*		XY_RW position = new XY_RW(longueurGrue, face.angleAttaque, true).plus(cubePosition);
		double angle = face.angleAttaque + Math.PI / 2 + 15. * Math.PI / 180.;
		position.plus(new XY(50, angle, true));
		return new XYO(position, angle);*/
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void run(Robot robot, Table table) throws InterruptedException, UnableToMoveException, ActionneurException
	{
		// TODO Auto-generated method stub
		
	}

}
