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

package senpai;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import pfg.config.Config;
import pfg.graphic.WindowFrame;
import pfg.kraken.robot.Cinematique;
import pfg.log.Log;
import org.junit.After;

import senpai.robot.Robot;
import senpai.ConfigInfoSenpai;
import senpai.Senpai;
import senpai.Senpai.ErrorCode;
import senpai.Subject;

/**
 * Classe mère de tous les tests.
 * Prépare container, log et config. Crée l'interface graphique si besoin est.
 * Détruit le tout à la fin.
 * 
 * @author pf
 *
 */

public abstract class JUnit_Test
{
	protected Senpai container;
	protected Config config;
	protected Log log;
//	private long timeoutAffichage;

	@Rule
	public TestName testName = new TestName();

	public void setUp(String... profiles) throws Exception
	{
		System.out.println("----- DÉBUT DU TEST " + testName.getMethodName() + " -----");

		container = new Senpai();
		container.initialize("senpai-test.conf", profiles);
		config = container.getService(Config.class);
//		timeoutAffichage = config.getLong(ConfigInfoSenpai.AFFICHAGE_TIMEOUT);
		log = container.getService(Log.class);		
		log.write("Test unitaire : " + testName.getMethodName(), Subject.STATUS);
/*		synchronized(config)
		{
			config.set(ConfigInfo.MATCH_DEMARRE, true);
			config.set(ConfigInfo.DATE_DEBUT_MATCH, System.currentTimeMillis());
			ThreadSerialInputCoucheOrdre.capteursOn = true;
		}*/
		/*
		 * La position initiale du robot
		 */
		Robot r = container.getService(Robot.class);
		r.setCinematique(new Cinematique(0, 1800, -Math.PI / 3, true, 0, false));
	}

	@After
	public void tearDown() throws Exception
	{
		// s'il y a eu un problème lors de la création de Senpai, par exemple si la connexion avec le LL a échoué
		if(container != null)
			container.destructor(ErrorCode.NO_ERROR);
	}

	/**
	 * Lanceur d'une seule méthode de test
	 * 
	 * @param args
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws ClassNotFoundException
	{
		assert args.length > 0;
		String[] classAndMethod = args[0].split("#");
		Request request = Request.method(Class.forName(classAndMethod[0]), classAndMethod[1]);

		Result result = new JUnitCore().run(request);
		System.exit(result.wasSuccessful() ? 0 : 1);
	}

}
