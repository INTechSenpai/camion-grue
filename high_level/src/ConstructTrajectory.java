import pfg.kraken.Kraken;
import pfg.kraken.utils.XYO;

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

/**
 * Construit une trajectoire et l'enregistre.
 * @author pf
 *
 */

public class ConstructTrajectory
{

	public static void main(String[] args)
	{
		if(args.length != 7)
		{
			System.out.println("Usage : ./run.sh "+ConstructTrajectory.class.getSimpleName()+" x_depart y_depart o_depart x_arrivee y_arrivee o_arrivee output.path");
			return;
		}
		
		double x = Double.parseDouble(args[0]);
		double y = Double.parseDouble(args[1]);
		double o = Double.parseDouble(args[2]);
		XYO depart = new XYO(x,y,o);
		
		x = Double.parseDouble(args[3]);
		y = Double.parseDouble(args[4]);
		o = Double.parseDouble(args[5]);
		XYO arrivee = new XYO(x,y,o);
		
		String output = args[6];
		
		// TODO
		Kraken k = Kraken.getKraken(null);
		
		// calcul trajet par bézier
		
		// affiche trajet
		
		// sauvegarde trajet
		
	}
	
}
