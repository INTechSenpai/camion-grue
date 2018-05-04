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

package senpai.robot;

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

import pfg.config.Config;
import pfg.kraken.SearchParameters;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.ItineraryPoint;
import pfg.log.Log;
import senpai.utils.ConfigInfoSenpai;
import senpai.utils.Subject;

/**
 * Gestionnaire de trajectoires déjà calculées
 * @author pf
 *
 */

public class KnownPathManager {
			
	private Map<String, SavedPath> paths = new HashMap<String, SavedPath>();
	private SearchParameters currentSp;
	private Log log;
	
	public KnownPathManager(Log log, Config config)
	{
		this.log = log;
		if(config != null && config.getBoolean(ConfigInfoSenpai.ALLOW_PRECOMPUTED_PATH))
			loadAllPaths();
	}
	
	public void savePath(SavedPath path)
	{
		
//		log.write("Sauvegarde d'une trajectoire : "+filename, Subject.DUMMY);
		FileOutputStream fichier = null;
		ObjectOutputStream oos;
		
		try
		{
			fichier = new FileOutputStream("paths/" + path.name);
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
				fichier = new FileOutputStream("paths/" + path.name);
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
		SavedPath s = new SavedPath(newPath, path.sp, path.name+"-limited");
		s.sp.maxSpeed = maxSpeed;
		return s;
	}
	
	private void loadAllPaths()
	{
		File[] files = new File("paths/").listFiles();
		int nb = 0;
		int nbErreurs = 0;
		if(files != null)
			for(File f : files)
			{
				ObjectInputStream ois = null;
				try
				{
					ois = new ObjectInputStream(new FileInputStream(f));
					paths.put(f.getName(), (SavedPath) ois.readObject());
					ois.close();
					nb++;
				}
				catch(IOException | ClassNotFoundException | ClassCastException e)
				{
					nbErreurs++;
					System.out.println("Erreur avec le fichier : "+f.getName());
				}
			}
		log.write(nb+" chemins chargés"+(nbErreurs == 0 ? "." : ", "+nbErreurs+" erreurs."), Subject.STATUS);
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
	
	private SearchParameters symetrise(SearchParameters sp)
	{
		SearchParameters out = new SearchParameters(
				symetrise(sp.start),
				symetrise(sp.arrival),
				sp.mode);
		out.directionstrategy = sp.directionstrategy;
		out.maxSpeed = sp.maxSpeed;
		return out;
	}
	
	private Cinematique symetrise(Cinematique c)
	{
		return new Cinematique(
				-c.getPosition().getX(),
				c.getPosition().getY(),
				Math.PI - c.orientationGeometrique,
				c.enMarcheAvant,
				-c.courbureGeometrique,
				c.stop);
	}
	
	private ItineraryPoint symetrise(ItineraryPoint ip)
	{
		return new ItineraryPoint(
				-ip.x,
				ip.y,
				Math.PI - ip.orientation,
				-ip.curvature,
				ip.goingForward,
				ip.maxSpeed,
				ip.possibleSpeed,
				ip.stop);
	}
	
	private List<ItineraryPoint> symetrise(List<ItineraryPoint> diff)
	{
		List<ItineraryPoint> out = new ArrayList<ItineraryPoint>();
		for(ItineraryPoint ip : diff)
			out.add(symetrise(ip));
		return out;
	}
	
	public PriorityQueue<SavedPath> loadCompatiblePath(SearchParameters sp)
	{
		String prefix = getTrajectoryNamePrefix(sp);
		PriorityQueue<SavedPath> out = new PriorityQueue<SavedPath>(new SavedPathComparator());
		for(String k : paths.keySet())
			if(k.startsWith(prefix))
			{
				SavedPath saved = paths.get(k);
				if(saved.sp.start.getPosition().squaredDistance(sp.start.getPosition()) < 40*40)
					out.add(saved);
			}
		return out;
	}

	public void currentSearchParameters(SearchParameters sp)
	{
		this.currentSp = sp;
	}
	
	public void addPath(List<ItineraryPoint> diff)
	{
		addPathNoSym(diff, currentSp);
		addPathNoSym(symetrise(diff), symetrise(currentSp));
	}

	private void addPathNoSym(List<ItineraryPoint> diff, SearchParameters sp)
	{
		String name = getTrajectoryNamePrefix(sp);
		SavedPath s = new SavedPath(diff, sp, name);
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
		s.name = name;
		paths.put(name, s);
		savePath(s);
		log.write("Nouveau chemin enregistré : "+name, Subject.STATUS);
	}

	private String getTrajectoryNamePrefix(SearchParameters sp)
	{
		currentSp = sp;
		return Math.abs(sp.start.hashCode())+"-"+Math.abs(sp.arrival.hashCode())+"-";
	}
	
}
