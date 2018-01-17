package senpai.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import pfg.config.Config;
import pfg.graphic.log.Log;
import senpai.ConfigInfoSenpai;
import senpai.Severity;
import senpai.Subject;

/**
 * Communication série
 * @author pf
 *
 */

public class Serie implements CommMedium, SerialPortEventListener
{
	private SerialPort serialPort;
	protected Log log;

	protected volatile boolean isClosed;
	private int baudrate;

	private String portName;

	/** The output stream to the port */
	private OutputStream output;
	
	private InputStream input;

	// Permet d'ouvrir le port à la première utilisation de la série
	protected volatile boolean portOuvert = false;

	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;

	public Serie(Log log)
	{
		this.log = log;
	}
	
	/**
	 * Constructeur pour la série de test
	 * 
	 * @param log
	 */
	public void initialize(Config config)
	{
		portName = config.getString(ConfigInfoSenpai.SERIAL_PORT);
		baudrate = config.getInt(ConfigInfoSenpai.BAUDRATE);

//		if(simuleSerie)
//			log.critical("SÉRIE SIMULÉE !");
	}

	@Override
	public synchronized void serialEvent(SerialPortEvent oEvent)
	{
/*		if(oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE)
			bufferIn.dataAvailable();
		else
			assert false : oEvent;*/
	}
	
	public synchronized void open(int delayBetweenTries) throws InterruptedException
	{
		if(!portOuvert && !searchPort())
		{
			/**
			 * Suppression des verrous qui empêchent parfois la connexion
			 */
			String OS = System.getProperty("os.name");
			if(!OS.toLowerCase().contains("win"))
			{
				// sur unix, on peut tester ça
				try
				{
					log.write("Port série non trouvé, suppression des verrous", Severity.WARNING, Subject.COMM);
					Runtime.getRuntime().exec("sudo rm -f /var/lock/LCK..tty*");
				}
				catch(IOException e)
				{
					log.write(e, Severity.WARNING, Subject.COMM);
				}
			}
			while(!searchPort())
			{
				log.write("Port série non trouvé, réessaie dans "+delayBetweenTries+" ms", Severity.CRITICAL, Subject.COMM);
				Thread.sleep(delayBetweenTries);
			}
		}
	}

	/**
	 * Recherche du port
	 * 
	 * @return
	 */
	protected synchronized boolean searchPort()
	{
		portOuvert = false;
		CommPortIdentifier port;
		try
		{
			port = CommPortIdentifier.getPortIdentifier(portName);
			log.write("Port " + port.getName() + " trouvé !", Subject.COMM);

			if(port.isCurrentlyOwned())
			{
				log.write("Port déjà utilisé par " + port.getCurrentOwner(), Severity.WARNING, Subject.COMM);
				return false;
			}

			if(!initialize(port, baudrate))
				return false;

			portOuvert = true;
			return true;

		}
		catch(NoSuchPortException e)
		{
			log.write("Port " + portName + " introuvable : " + e, Severity.WARNING, Subject.COMM);
			String out = "Les ports disponibles sont : ";

			Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();

			while(ports.hasMoreElements())
			{
				out += ((CommPortIdentifier) ports.nextElement()).getName();
				if(ports.hasMoreElements())
					out += ", ";
			}

			log.write(out, Subject.COMM);
			return false;
		}
	}

	/**
	 * Il donne à la série tout ce qu'il faut pour fonctionner
	 * 
	 * @param port_name
	 * Le port où est connecté la carte
	 * @param baudrate
	 * Le baudrate que la carte utilise
	 */
	private boolean initialize(CommPortIdentifier portId, int baudrate)
	{
		try
		{
			serialPort = (SerialPort) portId.open("INTech-Senpai", TIME_OUT);
			// set port parameters
			serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			// serialPort.setInputBufferSize(100);
			// serialPort.setOutputBufferSize(100);
			// serialPort.enableReceiveTimeout(100);
			// serialPort.enableReceiveThreshold(1);

			// open the streams
//			buffer.setInput(serialPort.getInputStream());
			output = serialPort.getOutputStream();
			input = serialPort.getInputStream();

			// Configuration du Listener
			serialPort.addEventListener(this);

			serialPort.notifyOnDataAvailable(true); // activation du listener
													// pour vérifier qu'on a des
													// données disponible

			isClosed = false;
			return true;
		}
		catch(TooManyListenersException | PortInUseException | UnsupportedCommOperationException | IOException e2)
		{
			log.write(e2, Severity.CRITICAL, Subject.COMM);
			return false;
		}
	}

	/**
	 * Doit être appelé quand on arrête de se servir de la série
	 */
	public synchronized void close()
	{
		if(!isClosed && portOuvert)
		{
			try
			{
				log.write("Fermeture de la carte", Subject.COMM);
				serialPort.removeEventListener();
				serialPort.close();
				input.close();
				output.close();
			}
			catch(IOException e)
			{
				log.write(e, Subject.COMM);
			}
			isClosed = true;
		}
		else if(isClosed)
			log.write("Carte déjà fermée", Subject.COMM);
		else
			log.write("Carte jamais ouverte", Subject.COMM);
	}

	@Override
	public boolean openIfClosed() throws InterruptedException
	{
		if(isClosed)
		{
			open(10);
			return true;
		}
		return false;
	}

	@Override
	public OutputStream getOutput()
	{
		return output;
	}

	@Override
	public InputStream getInput()
	{
		return input;
	}
}
