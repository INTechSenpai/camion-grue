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

package senpai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pfg.kraken.robot.ItineraryPoint;

/**
 * Gestionnaire de trajectoires déjà calculées
 * @author pf
 *
 */

public class KnownPathManager {
	
	public KnownPathManager()
	{
		loadAllPaths();
	}
	
	
	private Map<String, SavedPath> paths = new HashMap<String, SavedPath>();
	
	
	public void savePath(String filename, SavedPath path)
	{
//		log.write("Sauvegarde d'une trajectoire : "+filename, Subject.DUMMY);
		FileOutputStream fichier = null;
		ObjectOutputStream oos;
		
		try
		{
			fichier = new FileOutputStream("paths/" + filename);
		}
		catch(IOException e)
		{
			try
			{
				Runtime.getRuntime().exec("mkdir paths");
				try
				{
					Thread.sleep(50);
				}
				catch(InterruptedException e1)
				{
					e1.printStackTrace();
				}
			}
			catch(IOException e1)
			{
				e1.printStackTrace();
			}
			try
			{
				fichier = new FileOutputStream("paths/" + filename);
			}
			catch(FileNotFoundException e1)
			{
				e1.printStackTrace();
			}
		}
		
		try
		{
			oos = new ObjectOutputStream(fichier);
			oos.writeObject(path);
			oos.flush();
			oos.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public SavedPath limitMaxSpeed(SavedPath path, double maxSpeed)
	{
		List<ItineraryPoint> newPath = new ArrayList<ItineraryPoint>();
		for(ItineraryPoint it : path.path)
			newPath.add(new ItineraryPoint(it.x, it.y, it.orientation, it.curvature, it.goingForward, Math.min(maxSpeed, it.maxSpeed), Math.min(maxSpeed, it.possibleSpeed), it.stop));

		return new SavedPath(newPath, path.depart);
	}
	
	private void loadAllPaths()
	{
		File[] files = new File("paths/").listFiles();
		for(File f : files)
		{
			ObjectInputStream ois = null;
			try
			{
				ois = new ObjectInputStream(new FileInputStream(f));
				paths.put(f.getName(), (SavedPath) ois.readObject());
				ois.close();
			}
			catch(IOException | ClassNotFoundException | ClassCastException e)
			{
				System.out.println("Erreur avec le fichier : "+f.getName());
				e.printStackTrace();
			}
		}
	}
	
	public SavedPath loadPath(String filename)
	{
		return paths.get(filename);
	}
	
	public List<SavedPath> loadPathStartingWith(String filename)
	{
		List<SavedPath> out = new ArrayList<SavedPath>();
		for(String k : paths.keySet())
			if(k.startsWith(filename))
				out.add(paths.get(k));
		return out;
	}
	
/*	public SavedPath loadPath(String filename)
	{
//		log.debug("Chargement d'une trajectoire : "+nom);
		ObjectInputStream ois = null;
		try
		{
			FileInputStream fichier = new FileInputStream("paths/" + filename);
			ois = new ObjectInputStream(fichier);
			return (SavedPath) ois.readObject();
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
	}*/
	
}
