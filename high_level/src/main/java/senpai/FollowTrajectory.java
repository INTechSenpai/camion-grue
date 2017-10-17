package senpai;
import java.util.Arrays;
import java.util.List;

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

/**
 * Permet de lancer facilement un test
 * @author pf
 *
 */

public class FollowTrajectory
{
	public static void main(String[] args) throws InterruptedException
	{
		// TODO
		if(args.length == 0)
		{
			System.out.println("Usage : ./run.sh "+FollowTrajectory.class.getSimpleName()+" test.path [-p config-profile] [-y]");
			System.out.println("-y : ne demande pas la confirmation de l'utilisateur");
			return;
		}
		
		String filename = args[0];
		
		List<String> configProfile = Arrays.asList("default");
		boolean needValidation = true;
		
		for(int i = 1; i < args.length; i++)
		{
			if(args[i].equals("-c"))
				configProfile.add(args[++i]);
			else if(args[i].equals("-y"))
				needValidation = false;
		}
		
		String[] profilesArray = new String[configProfile.size()];
		for(int i = 0; i < profilesArray.length; i++)
			profilesArray[i] = configProfile.get(i);
		
		Senpai senpai = new Senpai(profilesArray);
		// display la trajectoire et attend la validation de l'utilisateur (selon needValidation)
	}
}
