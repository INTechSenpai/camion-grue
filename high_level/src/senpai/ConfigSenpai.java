package senpai;

import java.net.InetAddress;
import java.net.UnknownHostException;
import pfg.config.Config;

public class ConfigSenpai extends Config
{
	public ConfigSenpai(ConfigInfoSenpai[] allConfigInfo, String configfile, boolean verbose)
	{
		super(allConfigInfo, configfile, verbose);
	}

	public InetAddress getAdresse()
	{
		try
		{
			String[] s = getString(ConfigInfoSenpai.HOSTNAME_SERVEUR).split("\\."); // on découpe
			// avec les
			// points
			if(s.length == 4) // une adresse ip,
			// probablement
			{
	//			log.debug("Recherche du serveur à partir de son adresse ip : " + args[i]);
				byte[] addr = new byte[4];
				for(int j = 0; j < 4; j++)
				addr[j] = Byte.parseByte(s[j]);
					return InetAddress.getByAddress(addr);
			}
			else // le nom du serveur, probablement
			{
	//			log.debug("Recherche du serveur à partir de son nom : " + args[i]);
				return InetAddress.getByName(getString(ConfigInfoSenpai.HOSTNAME_SERVEUR));
			}
		}
		catch(UnknownHostException e)
		{
			assert false;
			e.printStackTrace();
			return null;
		}
	}
}
