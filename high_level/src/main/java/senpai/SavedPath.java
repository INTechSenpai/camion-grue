package senpai;

import java.io.Serializable;
import java.util.List;
import pfg.kraken.SearchParameters;
import pfg.kraken.robot.ItineraryPoint;

public class SavedPath implements Serializable
{
	private static final long serialVersionUID = 1L;
	public List<ItineraryPoint> path;
	public SearchParameters sp;
	
	public SavedPath(List<ItineraryPoint> path, SearchParameters sp)
	{
		this.path = path;
		this.sp = sp;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof SavedPath))
			return false;
		return ((SavedPath)o).path.equals(path);
	}
}
