import container.Container;
import exceptions.ActionneurException;
import exceptions.ContainerException;
import pathfinding.RealGameState;
import utils.Log;

public class DemoHeptacle {

	public static void main(String[] args) throws InterruptedException, ContainerException, ActionneurException
	{
		Container container = new Container();
		try {
			Log log = container.getService(Log.class);
			RealGameState state = container.getService(RealGameState.class);
	
			try
			{
				try
				{
					state.robot.rearme();
				}
				catch(ActionneurException e)
				{
					log.warning(e);
				}
	
				try
				{
					state.robot.rearmeAutreCote();
				}
				catch(ActionneurException e)
				{
					log.warning(e);
				}
	
				state.robot.ouvreFilet();
				
				try
				{
					state.robot.baisseFilet();
				}
				catch(ActionneurException e)
				{
					log.warning(e);
					try
					{
						state.robot.leveFilet();
					}
					catch(ActionneurException e1)
					{
						log.warning(e1);
						state.robot.fermeFilet();
						throw e1;
					}
	
					try
					{
						state.robot.baisseFilet();
					}
					catch(ActionneurException e1)
					{
						log.warning(e1);
						try
						{
							state.robot.leveFilet();
						}
						catch(ActionneurException e2)
						{
							log.warning(e2);
						}
						state.robot.fermeFilet();
						throw e1;
					}
				}
	
				state.robot.fermeFilet();
				state.robot.ouvreFilet();
				state.robot.fermeFiletForce();
	
				try
				{
					state.robot.leveFilet();
				}
				catch(ActionneurException e)
				{
					log.warning(e);
				}
			}
			catch(ActionneurException e)
			{
				state.robot.setFiletPlein(false);
				log.warning(e);
				throw e;
			}
		}
		finally
		{
			System.exit(container.destructor().code);			
		}
	}
	
}
