/*
 * Copyright (C) 2013-2018 Pierre-François Gimenez
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

package senpai;


import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Scanner;
import pfg.config.Config;
import pfg.config.ConfigInfo;
import pfg.graphic.DebugTool;
import pfg.graphic.GraphicDisplay;
import pfg.graphic.Position;
import pfg.graphic.TimestampedList;
import pfg.graphic.Vec2RO;
import pfg.graphic.Vec2RW;
import pfg.graphic.printable.ColoredPrintable;
import senpai.capteurs.Capteur;
import senpai.robot.RobotPrintable;
import senpai.utils.ConfigInfoSenpai;

/**
 * Un lecteur de vidéo enregistrée sur le rover
 * 
 * @author pf
 *
 */

public class VideoReader
{

	public static void main(String[] args)
	{
		String filename = null, logfile = null;
		double vitesse = 1;
//		boolean debug = false;
		long[] breakPoints = new long[0];
		int indexBP = 0;
		boolean stopOnWarning = false, stopOnCritical = false;
		boolean frameToFrame = false;
		long dateSkip = -1;
		boolean skipdone = false;
		long nextStopFTF = 0;
		RobotPrintable robotprintable = null;
		
		HashMap<ConfigInfo, Object> override = new HashMap<ConfigInfo, Object>();
		
		for(int i = 0; i < args.length; i++)
		{
			if(args[i].equals("-s")) // speed
				vitesse = Double.parseDouble(args[++i]);
			else if(args[i].equals("-S")) // skip
				dateSkip = Long.parseLong(args[++i]);
//			else if(args[i].equals("-d")) // debug
//				debug = true;
			else if(args[i].equals("-v")) // video
				filename = args[++i];
			else if(args[i].equals("-w")) // warning
				stopOnWarning = true;
			else if(args[i].equals("-c")) // critical
				stopOnCritical = true;
			else if(args[i].equals("-l")) // log
				logfile = args[++i];
			else if(args[i].equals("-B")) // break
			{
				int nb = Integer.parseInt(args[++i]);
				breakPoints = new long[nb];
				for(int j = 0; j < nb; j++)
					breakPoints[j] = Long.parseLong(args[++i]);
			}
			else
				System.err.println("Option inconnue ! " + args[i]);
		}

		if(filename == null && logfile == null)
		{
			System.out.println("Utilisation : VideoReader -v videoFile -l logFile [-s speed] [-w] [-c] [-B n ...]");
			System.out.println("-w : autostop on warning ");
			System.out.println("-c : autostop on critical ");
			System.out.println("-S date : start at this date");
			System.out.println("-B n t1 t2 … tn: add n breakpoints at timestamps t1,… tn ");
			System.out.println("-s speed : set reading speed. 2 is twice as fast, 0.5 twice as slow");
			return;
		}

		Scanner sc = new Scanner(System.in);

		try
		{
			TimestampedList listes = null;

			special("Fichier vidéo : " + filename);
			special("Fichier log : " + logfile);
			special("Vitesse : " + vitesse);
			if(dateSkip != -1)
				special("Skip to : " + dateSkip);
//			if(debug)
//				special("Debug activé");

			DebugTool debugTool = null;
			Vec2RW center = null;
			GraphicDisplay buffer = null;
			
			if(filename != null)
			{
				try
				{
					FileInputStream fichier = new FileInputStream(filename);
					ObjectInputStream ois = new ObjectInputStream(fichier);
					listes = (TimestampedList) ois.readObject();
					ois.close();
				}
				catch(IOException | ClassNotFoundException e)
				{
					System.err.println("Chargement échoué ! "+e);
					return;
				}
				
				Position d = listes.getDefaultCenter();
				Vec2RO defaultCenter = new Vec2RO(d.getX(), d.getY());
				center = defaultCenter.clone();
				debugTool = DebugTool.getDebugTool(override, defaultCenter, center, null, "reader.conf", "default");
				buffer = debugTool.getGraphicDisplay();
				robotprintable = new RobotPrintable(new Config(ConfigInfoSenpai.values(), false));
			}
			


			
			long initialDate = System.currentTimeMillis();

			Thread.sleep(500); // le temps que la fenêtre apparaisse

			BufferedReader br = null;
			long nextLog = Long.MAX_VALUE;
			String nextLine = null;

			if(logfile != null)
			{
				br = new BufferedReader(new FileReader(logfile));
				nextLine = br.readLine();
				if(nextLine == null)
					nextLog = Long.MAX_VALUE;
				else
					nextLog = getTimestampLog(nextLine);
			}

			long nextVid;

			if(listes == null)
				nextVid = Long.MAX_VALUE;
			else
				nextVid = listes.getTimestamp(0);
			
			long firstTimestamp = Math.min(nextLog, nextVid);

			int indexListe = 0;
			boolean stop = false;

			special("At any point, type \"stop\" to stop the VideoReader.");
			
			while(nextVid != Long.MAX_VALUE || nextLog != Long.MAX_VALUE)
			{
				if(indexBP < breakPoints.length && breakPoints[indexBP] < Math.min(nextVid, nextLog))
				{
					if(!frameToFrame)
						special("Breakpoint : "+breakPoints[indexBP]);
					indexBP++;
					stop = true;
				}
				
				if(frameToFrame && nextStopFTF < Math.min(nextVid, nextLog))
					stop = true;

				if(stop || System.in.available() > 0)
				{
					if(!frameToFrame)
					{						
						if(stop)
							special("Auto-pause !");
						else
							special("Pause ! Enter \"ftf\" to enter the frame-to-frame mode");
					}
					
					stop = false;
					while(System.in.available() > 0)
						System.in.read();

					long avant = System.currentTimeMillis();
					nextStopFTF = Math.min(nextVid, nextLog) + 5;

					String l = sc.nextLine();
					if(!frameToFrame && l.equals("ftf"))
					{
						frameToFrame = true;
						nextStopFTF = Math.min(nextVid, nextLog) + 5;
						special("Entre \"normal\" to resume the normal (non-frame-to-frame) mode");
					}
					else if(l.equals("stop"))
					{
						if(logfile != null)
							br.close();
						throw new InterruptedException();
					}
					else if(frameToFrame && l.equals("normal"))
					{
						special("Normal mode resumed");
						frameToFrame = false;
					}
					
/*					while(System.in.available() == 0)
						Thread.sleep(10);

					while(System.in.available() > 0)
						System.in.read();
*/
					initialDate += (System.currentTimeMillis() - avant);
					if(!frameToFrame)
						special("Unpause");
				}

				if(!skipdone && Math.min(nextVid, nextLog) > dateSkip)
				{
					stop = true;
					skipdone = true;
					initialDate -= dateSkip;
				}
				
				if(nextVid < nextLog)
				{
					PriorityQueue<ColoredPrintable> tab = listes.getList(indexListe);
					Iterator<ColoredPrintable> iter = tab.iterator();
					boolean trouve = false;
					Color color = null;
					int layer = 0;
					while(iter.hasNext())
					{
						ColoredPrintable c = iter.next();
						if(c.p instanceof RobotPrintable)
						{
							robotprintable.initPositionObject(((RobotPrintable)c.p).getCinematique());
							color = c.c;
							layer = c.l;
							iter.remove();
							trouve = true;
							break;
						}
					}
					if(trouve)
					{
						tab.add(new ColoredPrintable(robotprintable, color, layer, false));

						// mise à jour des capteurs
						for(ColoredPrintable c : tab)
							if(c.p instanceof Capteur)
								((Capteur)c.p).setCinematique(robotprintable.getCinematique());
					}
					
					long deltaT = (long) ((nextVid - firstTimestamp) / vitesse);
					long deltaP = System.currentTimeMillis() - initialDate;
					long delta = deltaT - deltaP;

					if(delta > 0 && dateSkip < nextVid)
						Thread.sleep(delta);

					Position p = listes.getPosition(indexListe);
					center.setX(p.getX());
					center.setY(p.getY());
					buffer.updatePrintable(tab);
					buffer.refresh();
/*					synchronized(buffer)
					{
						buffer.clearTemporaryPrintables();
						int i = 0;
						
						while(i < tab.size())
						{
							ColoredPrintable o = tab.get(i++);*/
							/*if(o instanceof Cinematique)
							{
								if(debug)
									System.out.println("Cinématique robot : " + ((Cinematique) o).getPosition());
								robot.setCinematique((Cinematique) o);
							}
							else if(o instanceof AnglesRoues)
							{
								if(debug)
									System.out.println("Angles des roues du robot : " + ((AnglesRoues) o).angleRoueGauche + ", " + ((AnglesRoues) o).angleRoueDroite);
								robot.setAngleRoues(((AnglesRoues) o).angleRoueGauche, ((AnglesRoues) o).angleRoueDroite);
							}
							else if(o instanceof Vector)
							{
								robot.setVector((Vector) o);
							}
							else */
/*							{
								if(debug)
									System.out.println("Ajout : " + o);
//								Layer l = (Layer) tab.get(i++);
								buffer.addTemporaryPrintable(o.p, o.c, o.l);
							}
						}
					}*/

					indexListe++;
					if(indexListe < listes.size())
						nextVid = listes.getTimestamp(indexListe);
					else
						nextVid = Long.MAX_VALUE;
				}
				else
				{
					long deltaT = (long) ((nextLog - firstTimestamp) / vitesse);
					long deltaP = System.currentTimeMillis() - initialDate;
					long delta = deltaT - deltaP;

					if(delta > 0 && dateSkip < nextLog)
						Thread.sleep(delta);

					if(skipdone && ((stopOnWarning && nextLine.contains("WARNING")) || stopOnCritical && nextLine.contains("CRITICAL")))
						stop = true;

					System.out.println(nextLine);

					nextLine = getNextLine(br);
					if(nextLine == null)
						nextLog = Long.MAX_VALUE;
					else
						nextLog = getTimestampLog(nextLine);
				}
			}
			special("Fin de l'enregistrement");
			
			if(logfile != null)
				br.close();
			
			if(debugTool != null)
				debugTool.destructor();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			sc.close();
		}
	}

	private static void special(Object o)
	{
		System.out.println("	\u001B[34m" + o + "\u001B[0m");
	}

	private static String getNextLine(BufferedReader br) throws IOException
	{
		String line;
		while((line = br.readLine()) != null)
//			if(Verbose.shouldPrint(extractMasque(line)))
				return line.substring(line.indexOf(" ") + 1);

		return null;
	}

/*	private static int extractMasque(String line)
	{
		try
		{
			return Integer.parseInt(line.split(" ")[0]);
		}
		catch(NumberFormatException e)
		{
			return 0;//Verbose.all;
		}
	}*/

	private static long getTimestampLog(String line)
	{
		String time = line.split(" ")[0];
		try
		{
			int first = -1;
			if(time.startsWith("\u001B["))
			{
				first = time.indexOf("m");
				time = time.substring(first + 1);
			}
			return Long.parseLong(time);
		}
		catch(NumberFormatException e)
		{
			return -1;
		}
	}

}
