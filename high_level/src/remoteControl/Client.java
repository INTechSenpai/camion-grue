package remoteControl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import config.Config;
import config.ConfigInfo;
import utils.Log;

public class Client extends JPanel implements KeyListener
{
	private static final long serialVersionUID = 9171422634457882975L;
	private JFrame frame;
	private Log log;
	private OutputStream out;
	private int sleep = 50;
	private volatile HashSet<Commandes> current = new HashSet<Commandes>();
	private volatile HashSet<Commandes> keepSending = new HashSet<Commandes>();
	private Image image = null;
	private int sizeX, sizeY;

	private class WindowExit extends WindowAdapter
	{
		@Override
		public synchronized void windowClosing(WindowEvent e)
		{
			notify();
			frame.dispose();
		}
	}
	
	public Client(String[] args) throws InterruptedException
	{
		log = new Log();
		Config config = new Config();
		log.useConfig(config);
		
		Commandes.STOP.setCode(32);
		Commandes.RESET_WHEELS.setCode(config.getInt(ConfigInfo.RESET_WHEELS_KEY));
		Commandes.SPEED_DOWN.setCode(config.getInt(ConfigInfo.SPEED_DOWN_KEY));
		Commandes.SPEED_UP.setCode(config.getInt(ConfigInfo.SPEED_UP_KEY));
		Commandes.TURN_LEFT.setCode(config.getInt(ConfigInfo.TURN_LEFT_KEY));
		Commandes.TURN_RIGHT.setCode(config.getInt(ConfigInfo.TURN_RIGHT_KEY));
		Commandes.BAISSE_FILET.setCode(config.getInt(ConfigInfo.BAISSE_FILET_KEY));
		Commandes.LEVE_FILET.setCode(config.getInt(ConfigInfo.LEVE_FILET_KEY));
		Commandes.EJECTE_DROITE.setCode(config.getInt(ConfigInfo.EJECTE_DROITE_KEY));
		Commandes.EJECTE_GAUCHE.setCode(config.getInt(ConfigInfo.EJECTE_GAUCHE_KEY));
		Commandes.REARME_GAUCHE.setCode(config.getInt(ConfigInfo.REARME_GAUCHE_KEY));
		Commandes.REARME_DROITE.setCode(config.getInt(ConfigInfo.REARME_DROITE_KEY));
		Commandes.FERME_FILET.setCode(config.getInt(ConfigInfo.FERME_FILET_KEY));
		Commandes.OUVRE_FILET.setCode(config.getInt(ConfigInfo.OUVRE_FILET_KEY));

		InetAddress rpiAdresse = null;
		boolean loop = true;
		log.debug("Démarrage du client de contrôle à distance");
		try
		{			
			if(args.length != 0)
			{
				for(int i = 0; i < args.length; i++)
				{
					if(!args[i].startsWith("-"))
					{
						String[] s = args[i].split("\\."); // on découpe
															// avec les
															// points
						if(s.length == 4) // une adresse ip,
											// probablement
						{
							log.debug("Recherche du serveur à partir de son adresse ip : " + args[i]);
							byte[] addr = new byte[4];
							for(int j = 0; j < 4; j++)
								addr[j] = Byte.parseByte(s[j]);
							rpiAdresse = InetAddress.getByAddress(addr);
						}
						else // le nom du serveur, probablement
						{
							log.debug("Recherche du serveur à partir de son nom : " + args[i]);
							rpiAdresse = InetAddress.getByName(args[i]);
						}
					}
					else
						log.warning("Paramètre inconnu : " + args[i]);
				}
			}

			if(rpiAdresse == null) // par défaut, la raspi (ip fixe)
			{
				rpiAdresse = InetAddress.getByAddress(new byte[] { (byte) 172, 24, 1, 1 });
				log.debug("Utilisation de l'adresse par défaut : " + rpiAdresse);
			}
		}
		catch(UnknownHostException e)
		{
			log.critical("La recherche du serveur a échoué ! " + e);
			return;
		}

		Socket socket = null;
		do
		{

			boolean ko;
			log.debug("Tentative de connexion…");

			do
			{
				try
				{
					socket = new Socket(rpiAdresse, 13371);
					ko = false;
				}
				catch(IOException e)
				{
					Thread.sleep(500); // on attend un peu avant de
										// réessayer
					ko = true;
				}
			} while(ko);

			log.debug("Connexion réussie !");
			Thread.sleep(1000);
			try
			{
				out = socket.getOutputStream();
			}
			catch(IOException e)
			{
				log.warning("Le serveur a coupé la connexion : " + e);
				continue; // on relance la recherche
			}

			sizeX = sizeY = 100;
			
			try {
				image = ImageIO.read(new File("img/background_remote_control_mini.jpg"));
				sizeX = image.getWidth(this);
				sizeY = image.getHeight(this);
			} catch (IOException e1) {
				log.critical(e1);
			}

			/*
			 * À ne faire qu'une fois !
			 */
			if(frame == null)
			{
				setBackground(Color.GRAY);
				setPreferredSize(new Dimension(sizeX, sizeY));
				frame = new JFrame();
				frame.addKeyListener(this);
				frame.addWindowListener(new WindowExit());
				frame.getContentPane().add(this);
				frame.pack();
				frame.setVisible(true);
				repaint();
			}
			
//			for(Commandes c : Commandes.values())
//				log.debug(c+" "+c.code);
			
			int noSending = 0;
			try
			{
				while(true)
				{
					Thread.sleep(sleep);
					
					synchronized(keepSending)
					{
						if(keepSending.isEmpty() && noSending == 6)
						{
							out.write(Commandes.PING.ordinal());
							out.write(0); // pas de param
							out.flush();
							noSending = 0;
						}
						
						if(!keepSending.isEmpty())
						{
							for(Commandes c : keepSending)
							{
								out.write(c.ordinal());
								out.write(0); // pas de param
							}
							out.flush();
						}
						else
							noSending++;
						
						// pas de spam !
						keepSending.remove(Commandes.RESET_WHEELS);
						keepSending.remove(Commandes.STOP);
					}
				}
			}
			catch(IOException e)
			{
				log.warning(e);
			}
			finally
			{
				try
				{
					out.write(Commandes.SHUTDOWN.ordinal());
					out.write(0);
					out.close();
				}
				catch(IOException e)
				{
					log.warning(e);
				}
			}

		} while(loop);

		try
		{
			socket.close();
		}
		catch(IOException e)
		{
			log.critical(e);
		}
		log.debug("Arrêt du client de contrôle à distance");
	}

	@Override
	public synchronized void keyPressed(KeyEvent arg0)
	{
		Commandes tmp = null;
		int code = arg0.getKeyCode();

		if(out == null)
			return;
		
		
		for(Commandes c : Commandes.values())
			if(code == c.code)
			{
				tmp = c;
				break;
			}
		
		// STOP sur toutes les touches
		if(tmp == null)
			tmp = Commandes.STOP;

		
		if(!current.contains(tmp))
		{
			synchronized(current)
			{
				current.add(tmp);
			}
			synchronized(keepSending)
			{
				keepSending.add(tmp);
			}
		}
	}

	@Override
	public synchronized void keyReleased(KeyEvent arg0)
	{
		int code = arg0.getKeyCode();
		for(Commandes c : Commandes.values())
			if(code == c.code)
			{
				synchronized(current)
				{
					current.remove(c);
					keepSending.remove(c);
				}
				break;
			}
	}

	@Override
	public synchronized void keyTyped(KeyEvent arg0)
	{}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if(image != null)
			g.drawImage(image, 0, 0, this);
	}
}
