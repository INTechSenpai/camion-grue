package senpai;

import java.io.Serializable;
import java.util.List;
import pfg.kraken.robot.ItineraryPoint;
import pfg.kraken.utils.XYO;

public class SavedPath implements Serializable
{
	private static final long serialVersionUID = 1L;
	public List<ItineraryPoint> path;
	public XYO depart;
	
	public SavedPath(List<ItineraryPoint> path, XYO depart)
	{
		this.path = path;
		this.depart = depart;
	}
}
