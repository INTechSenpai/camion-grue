import capteurs.SensorMode;
import config.Config;
import config.ConfigInfo;
import container.Container;
import exceptions.ContainerException;
import exceptions.MemoryManagerException;
import exceptions.PathfindingException;
import exceptions.UnableToMoveException;
import pathfinding.KeyPathCache;
import pathfinding.PathCache;
import pathfinding.RealGameState;
import robot.Cinematique;
import robot.RobotColor;
import robot.RobotReal;
import robot.Speed;
import scripts.ScriptsSymetrises;
import serie.BufferOutgoingOrder;
import serie.SerialProtocol;
import serie.SerialProtocol.State;
import serie.Ticket;
import utils.Log;

public class MatchUtilitary {

	private Container container;
	private Log log;
	private PathCache path;
	private RealGameState state;
	private boolean sym;
	private boolean simuleSerie;
	private KeyPathCache first;
	
	public void setUp(RobotColor couleurSimule, ScriptsSymetrises firstScript) throws PathfindingException, MemoryManagerException, InterruptedException, ContainerException
	{
		container = new Container();
		log = container.getService(Log.class);
		Config config = container.getService(Config.class);
		BufferOutgoingOrder data = container.getService(BufferOutgoingOrder.class);
		RobotReal robot = container.getService(RobotReal.class);
		path = container.getService(PathCache.class);
		state = container.getService(RealGameState.class);
		simuleSerie = config.getBoolean(ConfigInfo.SIMULE_SERIE);

		Ticket t = data.waitForJumper();

		log.debug("Initialisation des actionneurs…");

		/*
		 * Initialise les actionneurs
		 */
		robot.initActionneurs();

		log.debug("Actionneurs initialisés");

		robot.setSensorMode(SensorMode.ALL);

		log.debug("Attente de la couleur…");

		/*
		 * Demande de la couleur
		 */
		if(!simuleSerie)
		{
			State etat;
			do
			{
				Ticket tc = data.demandeCouleur();
				etat = tc.attendStatus().etat;
				Thread.sleep(100);
			} while(etat != SerialProtocol.State.OK);
		}
		else
			config.set(ConfigInfo.COULEUR, couleurSimule);
		log.debug("Couleur récupérée");
							
		/*
		 * La couleur est connue : on commence le stream de position
		 */
		data.startStream();

		log.debug("Stream des positions et des capteurs lancé");

		/*
		 * On attend d'avoir l'info de position
		 */
		if(simuleSerie)
			robot.setCinematique(new Cinematique(couleurSimule.symmetry ? -550 : 550, 1905, -Math.PI / 2, true, 0));
		else
		{
			synchronized(robot)
			{
				if(!robot.isCinematiqueInitialised())
					robot.wait();
			}
		}

		log.debug("Cinématique initialisée : " + robot.getCinematique());

		Thread.sleep(100);
		sym = config.getSymmetry();
		
		first = new KeyPathCache(state);
		first.shoot = false;
		first.s = firstScript.getScript(sym);
		path.prepareNewPath(first);
		
		log.debug("Attente du jumper…");

		/*
		 * Attente du jumper
		 */
		if(!simuleSerie)
			t.attendStatus();

		log.debug("LE MATCH COMMENCE !");

		/*
		 * Le match a commencé !
		 */
		data.startMatchChrono();
		log.debug("Chrono démarré");
	}
	
	public void doTheFirstBarrelRoll(Speed vitessePF) throws PathfindingException, InterruptedException, UnableToMoveException, MemoryManagerException
	{
		path.follow(first, vitessePF);
		first.s.s.execute(state);
	}
	
	public void doABarrelRoll(ScriptsSymetrises s, Speed vitessePF) throws PathfindingException, InterruptedException, UnableToMoveException, MemoryManagerException
	{
		KeyPathCache k = new KeyPathCache(state);
		k.shoot = false;
		k.s = s.getScript(sym);
		path.computeAndFollow(k, vitessePF);
		k.s.s.execute(state);
	}

	public boolean getSimuleSerie()
	{
		return simuleSerie;
	}

	public Log getLog()
	{
		return log;
	}

	public void stop()
	{
		try {
			System.exit(container.destructor().code);
		} catch (ContainerException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
