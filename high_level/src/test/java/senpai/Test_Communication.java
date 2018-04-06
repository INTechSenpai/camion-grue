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

import org.junit.Before;
import org.junit.Test;

import senpai.buffer.OutgoingOrderBuffer;
import senpai.comm.Ticket;
import senpai.comm.CommProtocol.Id;
import senpai.comm.CommProtocol.State;
import pfg.kraken.utils.XY;

/**
 * Tests unitaires de la série.
 * 
 * @author pf
 *
 */

public class Test_Communication extends JUnit_Test
{

	private OutgoingOrderBuffer data;

	@Before
	public void setUp() throws Exception
	{
		setUp("default");
		data = container.getService(OutgoingOrderBuffer.class);
		
		// il est nécessaire que les communications ne soient pas simulées
//		assert !config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);
	}
	
	/**
	 * Un test pour vérifie la connexion
	 * Le programme s'arrête automatiquement au bout de 3s
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_ping2() throws Exception
	{
		Thread.sleep(3000);
	}
	
	@Test
	public void test_deco() throws Exception
	{
		while(true)
//		for(int i = 0; i < 2; i++)
		{
			data.ping().attendStatus();
			System.out.println("Ping !");
			Thread.sleep(3000);
		}
	}

	/**
	 * Un test d'ordre long
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_stream() throws Exception
	{
		data.ping();
		data.startStream(Id.ODO_AND_SENSORS);
		Thread.sleep(5000);
		data.stopStream(Id.ODO_AND_SENSORS);
		data.ping();
	}

	@Test
	public void test_latence() throws Exception
	{
		data.checkLatence();
	}
	
	/**
	 * Un test d'ordre court.
	 * On redemande la couleur jusqu'à avoir un autre code que "couleur
	 * inconnue"
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_ask_color() throws Exception
	{
		State etat;
		do
		{
			Ticket t = data.demandeCouleur();
			etat = t.attendStatus().status;
			Thread.sleep(500);
		} while(etat != State.OK);
	}

	/**
	 * Test de correction de xyo
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_edit_position() throws Exception
	{
		data.startStream(Id.ODO_AND_SENSORS);
		Thread.sleep(1000);
		data.correctPosition(new XY(42, 24), 1);
		Thread.sleep(1000);
	}

	/**
	 * Test du jumper (le HL s'arrête dès que le jumper est retiré)
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_jumper() throws Exception
	{
		Ticket t = data.waitForJumper();
		t.attendStatus();
	}

	/**
	 * Test du compte à rebours de 90s (le test relou, quoi)
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_start_match() throws Exception
	{
		data.startMatchChrono();
		Thread.sleep(100000);
	}

}
