package senpai;

import java.util.List;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.utils.XYO;

public class SavedPath
{
	public List<ItineraryPoint> path;
	public XYO depart;
	
	public SavedPath(List<ItineraryPoint> path, XYO depart)
	{
		this.path = path;
		this.depart = depart;
	}
}
