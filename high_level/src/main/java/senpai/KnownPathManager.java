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

package senpai;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import pfg.kraken.robot.ItineraryPoint;

/**
 * Gestionnaire de trajectoires déjà calculées
 * @author pf
 *
 */

public class KnownPathManager {
/*
	private Log log;
	
	public KnownPathManager(Log log)
	{
		this.log = log;
	}
	*/
	
	
	public static void savePath(String filename, List<ItineraryPoint> path)
	{
//		log.write("Sauvegarde d'une trajectoire : "+filename, Subject.DUMMY);
		try
		{
			FileOutputStream fichier;
			ObjectOutputStream oos;

			fichier = new FileOutputStream("paths/" + filename);
			oos = new ObjectOutputStream(fichier);
			oos.writeObject(path);
			oos.flush();
			oos.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
//			log.write("Erreur lors de la sauvegarde de la trajectoire ! " + e, Severity.CRITICAL, Subject.DUMMY);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<ItineraryPoint> loadPath(String filename)
	{
//		log.debug("Chargement d'une trajectoire : "+nom);
		ObjectInputStream ois = null;
		try
		{
			FileInputStream fichier = new FileInputStream(filename);
			ois = new ObjectInputStream(fichier);
			return (LinkedList<ItineraryPoint>) ois.readObject();
		}
		catch(IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
//			log.warning(e);
		}
		finally
		{
			if(ois != null)
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return null;
	}
	
}
