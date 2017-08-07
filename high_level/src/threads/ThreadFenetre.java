/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
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

package threads;

import pfg.config.Config;
import pfg.graphic.ConfigInfoGraphic;
import pfg.graphic.Fenetre;
import pfg.graphic.PrintBuffer;
import pfg.log.Log;
import senpai.Subject;
import senpai.Senpai;

/**
 * S'occupe de la mise à jour graphique
 * 
 * @author pf
 *
 */

public class ThreadFenetre extends Thread
{

	protected Log log;
	private Fenetre fenetre;
	private PrintBuffer buffer;
	private boolean gif, print, deporte;
	private long derniereSauv = 0;
	private String giffile;

	public ThreadFenetre(Log log, Senpai container, PrintBuffer buffer, Config config)
	{
		this.log = log;
		this.buffer = buffer;
/*		print = config.getBoolean(ConfigInfoGraphic.GRAPHIC_ENABLE);
		deporte = config.getBoolean(ConfigInfoGraphic.GRAPHIC_EXTERNAL);
		giffile = config.getString(ConfigInfoGraphic.GIF_FILENAME);
		if(print && !deporte)
			try
			{
				fenetre = container.getService(Fenetre.class);
			}
			catch(ContainerException e)
			{
				e.printStackTrace();
			}*/
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName(getClass().getSimpleName());
		log.write("Démarrage de " + Thread.currentThread().getName(), Subject.DUMMY);
		try
		{
/*			if(!print || deporte)
			{
				log.debug(getClass().getSimpleName() + " annulé (" + ConfigInfo.GRAPHIC_ENABLE + " = " + print + ", " + ConfigInfo.GRAPHIC_EXTERNAL + " = " + deporte + ")");
				while(true)
					Thread.sleep(10000);
			}*/

			while(true)
			{
				synchronized(buffer)
				{
					if(!buffer.needRefresh())
						buffer.wait();
				}
				fenetre.refresh();
				if(gif && System.currentTimeMillis() - derniereSauv > 300)
				{ // on sauvegarde une image toutes les 300ms au plus
					fenetre.saveImage();
					derniereSauv = System.currentTimeMillis();
				}
				Thread.sleep(50); // on ne met pas à jour plus souvent que
									// toutes les 50ms
			}
		}
		catch(InterruptedException e)
		{
			log.write("Arrêt de " + Thread.currentThread().getName(), Subject.DUMMY);
			Thread.currentThread().interrupt();
		}
		catch(Exception e)
		{
			log.write("Arrêt inattendu de " + Thread.currentThread().getName() + " : " + e, Subject.DUMMY);
			e.printStackTrace();
			e.printStackTrace(log.getPrintWriter());
			Thread.currentThread().interrupt();
		}
	}

}
