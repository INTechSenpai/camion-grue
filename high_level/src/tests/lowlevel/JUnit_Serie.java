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

package tests.lowlevel;

import org.junit.Before;
import org.junit.Test;

import buffer.OutgoingOrderBuffer;
import comm.Ticket;
import comm.CommProtocol.Id;
import comm.CommProtocol.State;
import pfg.kraken.utils.XY;
import senpai.ConfigInfoSenpai;
import tests.JUnit_Test;

/**
 * Tests unitaires de la série.
 * 
 * @author pf
 *
 */

public class JUnit_Serie extends JUnit_Test
{

	private OutgoingOrderBuffer data;

	@Override
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		data = container.getService(OutgoingOrderBuffer.class);
		
		// il est nécessaire que les communications ne soient pas simulées
		assert !config.getBoolean(ConfigInfoSenpai.SIMULE_COMM);
	}
	
	/**
	 * Un test pour vérifie la connexion
	 * Le programme s'arrête automatiquement au bout de 3s
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_ping() throws Exception
	{
		Thread.sleep(3000);
	}

	/**
	 * Un test d'ordre long
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_stream() throws Exception
	{
		data.startStream(Id.SENSORS_CHANNEL);
		Thread.sleep(10000);
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
			etat = t.attendStatus().etat;
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
		data.startStream(Id.SENSORS_CHANNEL);
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
		synchronized(t)
		{
			if(t.isEmpty())
				t.wait();
		}
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
