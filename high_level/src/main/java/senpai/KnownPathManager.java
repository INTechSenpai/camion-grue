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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import pfg.kraken.SearchParameters;
import pfg.kraken.robot.ItineraryPoint;

/**
 * Gestionnaire de trajectoires déjà calculées
 * @author pf
 *
 */

public class KnownPathManager {
			
	private Map<String, SavedPath> paths = new HashMap<String, SavedPath>();
	private SearchParameters currentSp;
	
	public KnownPathManager()
	{
		loadAllPaths();
	}
	
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
		SavedPath s = new SavedPath(newPath, path.sp);
		s.sp.maxSpeed = maxSpeed;
		return s;
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
	
	private class SavedPathComparator implements Comparator<SavedPath>
	{
		@Override
		public int compare(SavedPath o1, SavedPath o2)
		{
			return o1.path.size() - o2.path.size();
		}
	}
	
	public PriorityQueue<SavedPath> loadCompatiblePath(SearchParameters sp)
	{
		String prefix = getTrajectoryNamePrefix(sp);
		PriorityQueue<SavedPath> out = new PriorityQueue<SavedPath>(new SavedPathComparator());
		for(String k : paths.keySet())
			if(k.startsWith(prefix))
				out.add(paths.get(k));
		return out;
	}

	public void currentSearchParameters(SearchParameters sp)
	{
		this.currentSp = sp;
	}
	
	public void addPath(List<ItineraryPoint> diff)
	{
		SavedPath s = new SavedPath(diff, currentSp);
		String name = getTrajectoryNamePrefix(currentSp);
		int biggest = 0;
		for(String k : paths.keySet())
			if(k.startsWith(name))
			{
				SavedPath o = paths.get(k);
				if(o.equals(s))
					return; // chemin déjà connu
				biggest++;
			}
		name += biggest;
		paths.put(name, s);
		savePath(name, s);
	}

	private String getTrajectoryNamePrefix(SearchParameters sp)
	{
		currentSp = sp;
		return Math.abs(sp.start.hashCode())+"-"+Math.abs(sp.arrival.hashCode())+"-";
	}
	
}
