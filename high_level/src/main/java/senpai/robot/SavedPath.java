package senpai.robot;

import java.io.Serializable;
import java.util.List;
import pfg.kraken.SearchParameters;
import pfg.kraken.robot.ItineraryPoint;

public class SavedPath implements Serializable
{
	private static final long serialVersionUID = 1L;
	public List<ItineraryPoint> path;
	public SearchParameters sp;
	public String name;
	
	public SavedPath(List<ItineraryPoint> path, SearchParameters sp, String name)
	{
		this.path = path;
		this.sp = sp;
		this.name = name;
	}
	
	@Override
	public int hashCode()
	{
		return path.size();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o == null || !(o instanceof SavedPath))
			return false;
		return ((SavedPath)o).path.equals(path);
	}
}
