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

import config.ConfigInfo;
import container.Container;
import exceptions.ContainerException;
import utils.Log;

/**
 * Utilisation par contrôle à distance
 * 
 * @author pf
 *
 */

public class RemoteControl
{
	public static void main(String[] args)
	{
		ConfigInfo.REMOTE_CONTROL.setDefaultValue(true);		
		ConfigInfo.GRAPHIC_ENABLE.setDefaultValue(false);		
		Container container = null;
		try
		{
			container = new Container();
			Log log = container.getService(Log.class);
			log.warning("Serveur de contrôle à distance prêt !");
			while(true)
				Thread.sleep(5000);
		} catch (ContainerException | InterruptedException e) {
			e.printStackTrace();
		}
		finally
		{
			try {
				System.exit(container.destructor().code);
			} catch (ContainerException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
