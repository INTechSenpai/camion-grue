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

package serie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import config.Config;
import config.ConfigInfo;
import container.Service;
import container.dependances.SerialClass;
import exceptions.serie.ClosedSerialException;
import exceptions.serie.IncorrectChecksumException;
import exceptions.serie.MissingCharacterException;
import exceptions.serie.ProtocolException;
import serie.trame.Conversation;
import serie.trame.EndOrderFrame;
import serie.trame.Frame.IncomingCode;
import serie.trame.IncomingFrame;
import serie.trame.Order;
import serie.trame.Paquet;
import utils.Log;
import utils.Log.Verbose;

/**
 * Implémentation du protocole série couche trame
 * 
 * @author pf
 *
 */

public class SerieCoucheTrame implements Service, SerialClass
{
	/**
	 * Toutes les conversations
	 */
	private Conversation[] conversations = new Conversation[256];

	/**
	 * Liste des trames dont on attend un acquittement
	 * Les trames de cette liste sont toujours triées par date de mort (de la
	 * plus proche à la plus éloignée)
	 */
	private LinkedList<Integer> waitingFrames = new LinkedList<Integer>();

	/**
	 * Liste des trames d'ordre long acquittées dont on attend la fin
	 */
	private List<Integer> pendingLongFrames = new ArrayList<Integer>();

	/**
	 * Liste des trames d'ordre dont on a reçu la fin (EXECUTION_END ou
	 * REQUEST_ANSWER)
	 */
	private LinkedList<Integer> closedFrames = new LinkedList<Integer>();

	private int timeout;
	private int dernierIDutilise = 0xFF; // dernier ID utilisé

	// Afin d'éviter de la créer à chaque fois
	private EndOrderFrame endOrderFrame = new EndOrderFrame();

	private Log log;
	private BufferOutgoingBytes serieOutput;
	private BufferIncomingBytes serieInput;

	/**
	 * Constructeur classique
	 * 
	 * @param log
	 * @param serie
	 */
	public SerieCoucheTrame(Log log, BufferOutgoingBytes serieOutput, BufferIncomingBytes serieInput, Config config)
	{
		this.log = log;
		this.serieInput = serieInput;
		this.serieOutput = serieOutput;
		timeout = config.getInt(ConfigInfo.SERIAL_TIMEOUT);
		for(int i = 0; i < 256; i++)
			conversations[i] = new Conversation(i, config);
	}

	/**
	 * GESTION DE LA CRÉATION ET DE L'ENVOI DE TRAMES
	 */

	/**
	 * Renvoie la prochaine conversation disponible, basée sur l'ID.
	 * Cette méthode vérifie les ID actuellement utilisés et donne le prochain
	 * qui est libre.
	 * Si tous les ID sont occupés, attend 1ms et re-cherche.
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	private synchronized Conversation getNextAvailableConversation() throws InterruptedException
	{
		int initialID = dernierIDutilise;
		dernierIDutilise++;
		dernierIDutilise &= 0xFF;
		while(true)
		{
			if(initialID == dernierIDutilise) // on a fait un tour complet…
			{
				log.critical("Aucun ID disponible : attente");
				Thread.sleep(1);
			}

			if(!conversations[dernierIDutilise].libre)
			{
				dernierIDutilise++;
				dernierIDutilise &= 0xFF;
			}
			else
				break;
		}
		conversations[dernierIDutilise].libre = false;
		waitingFrames.add(dernierIDutilise);
		return conversations[dernierIDutilise];
	}

	/**
	 * Demande l'envoi d'un ordre
	 * 
	 * @param o
	 * @throws InterruptedException
	 * @throws ClosedSerialException 
	 */
	public void sendOrder(Order o) throws InterruptedException, ClosedSerialException
	{
		Conversation f = getNextAvailableConversation();
		f.update(o);

		log.debug("Envoi d'une nouvelle trame : " + f.getFirstTrame(), Verbose.SERIE.masque);

		serieOutput.add(f, f.getFirstTrame().trame, f.getFirstTrame().tailleTrame);
		// f.updateResendDate();
	}

	/**
	 * GESTION DE LA RÉCEPTION DES TRAMES
	 */

	/**
	 * Renvoi les données de la couche ordre (haut niveau)
	 * C'est cette méthode qui s'occupe de commander la signalisation.
	 * 
	 * @return
	 * @throws ShutdownRequestException 
	 * @throws ClosedSerialException 
	 */
	public Paquet readData() throws InterruptedException, ClosedSerialException
	{
		IncomingFrame f = null;
		Paquet p = null;
		boolean restart;
		do
		{
			restart = false;
			try
			{
				f = readFrame();
				p = processFrame(f);
				if(p == null) // c'est une trame de signalisation
					restart = true;
			}
			catch(ProtocolException | IncorrectChecksumException | MissingCharacterException | IllegalArgumentException e)
			{
				log.warning("Exception ignorée : " + e);
				restart = true;
			}
		} while(restart);
		return p;
	}

	/**
	 * S'occupe du protocole : répond si besoin est, vérifie la cohérence, etc.
	 * Renvoie le ticket associé à la conversation
	 * 
	 * @param f
	 * @throws InterruptedException
	 * @throws ClosedSerialException 
	 */
	public synchronized Paquet processFrame(IncomingFrame f) throws ProtocolException, InterruptedException, ClosedSerialException
	{
		Iterator<Integer> it = waitingFrames.iterator();
		while(it.hasNext())
		{
			Integer id = it.next();
			Conversation waiting = conversations[id];
			if(id == f.id)
			{
				// On a le EXECUTION_BEGIN d'une frame qui l'attendait
				if(f.code == IncomingCode.EXECUTION_BEGIN)
				{
					if(waiting.origine.type == Order.Type.LONG)
					{
						log.debug("EXECUTION_BEGIN reçu : " + f, Verbose.SERIE.masque);
						it.remove();
						pendingLongFrames.add(id);
						return null;
					}

					throw new ProtocolException(f.code + " reçu pour un ordre " + waiting.origine.type + ". " + f);
				}
				else if(f.code == IncomingCode.VALUE_ANSWER)
				{
					if(waiting.origine.type == Order.Type.SHORT)
					{
						log.debug("VALUE_ANSWER reçu : " + f, Verbose.SERIE.masque);

						// L'ordre court a reçu un acquittement et ne passe pas
						// par la case "pending"
						it.remove();
						waiting.setDeathDate(); // tes jours sont comptés…
						closedFrames.add(id);
						return new Paquet(f.message, waiting.ticket, waiting.origine, f.code);
					}

					throw new ProtocolException(f.code + " reçu pour un ordre " + waiting.origine.type + ". " + f);
				}
				else
					throw new ProtocolException(f.code + " reçu à la place de EXECUTION_BEGIN ou VALUE_ANSWER ! " + f);
			}
		}

		// Cette valeur n'a pas été trouvée dans les trames en attente
		// On va donc chercher dans les trames en cours

		it = pendingLongFrames.iterator();
		while(it.hasNext())
		{
			Integer id = it.next();
			Conversation pending = conversations[id];
			if(id == f.id)
			{
				// On a le EXECUTION_END d'une frame
				if(f.code == IncomingCode.EXECUTION_END)
				{
					log.debug("EXECUTION_END reçu : " + f + ". On répond par un END_ORDER.", Verbose.SERIE.masque);

					pending.setDeathDate(); // tes jours sont comptés…
					// on envoie un END_ORDER
					endOrderFrame.updateId(f.id);
					serieOutput.add(null, endOrderFrame.trame, endOrderFrame.tailleTrame);
					// et on retire la trame des trames en cours
					it.remove();
					closedFrames.add(id);
					return new Paquet(f.message, pending.ticket, pending.origine, f.code);
				}
				else if(f.code == IncomingCode.STATUS_UPDATE)
				{
					log.debug("STATUS_UPDATE reçu : " + f, Verbose.SERIE.masque);

					return new Paquet(f.message, pending.ticket, pending.origine, f.code);
				}
				else
					throw new ProtocolException(f.code + " reçu à la place de EXECUTION_END ou STATUS_UPDATE ! " + f);
			}
		}

		// On cherche parmi les trames récemment fermées

		it = closedFrames.iterator();
		while(it.hasNext())
		{
			Integer id = it.next();
			Conversation closed = conversations[id];
			if(id == f.id)
			{
				// On avait déjà reçu l'EXECUTION_END. On renvoie un END_ORDER
				if(f.code == IncomingCode.EXECUTION_END && closed.origine.type == Order.Type.LONG)
				{
					log.warning("EXECUTION_END déjà reçu : " + f, Verbose.SERIE.masque);
					endOrderFrame.updateId(f.id);
					serieOutput.add(null, endOrderFrame.trame, endOrderFrame.tailleTrame);
					return null;
				}

				if(f.code == IncomingCode.VALUE_ANSWER && closed.origine.type == Order.Type.SHORT)
				{
					log.warning("VALUE_ANSWER déjà reçu : " + f, Verbose.SERIE.masque);
					return null;
				}

				throw new ProtocolException(f.code + " reçu pour une trame " + closed.origine.type + " finie ! " + f);
			}
		}

		throw new ProtocolException("ID conversation inconnu : " + f.id + ". " + f);
	}

	/**
	 * Lit une frame depuis la série
	 * Cette méthode est bloquante
	 * 
	 * @return
	 * @throws MissingCharacterException
	 * @throws IncorrectChecksumException
	 * @throws InterruptedException
	 * @throws ShutdownRequestException 
	 * @throws ClosedSerialException 
	 */
	private IncomingFrame readFrame() throws MissingCharacterException, IncorrectChecksumException, IllegalArgumentException, InterruptedException, ClosedSerialException
	{
		int code, id, longueur, checksum;
		int[] message;
		synchronized(serieInput)
		{
			// Attente des données…
			if(!serieInput.available())
				serieInput.wait(); // si on n'a pas encore la communication, on ne met pas de timeout

			code = serieInput.read();

			IncomingFrame.check(code);

			longueur = serieInput.read();

			if(longueur < 4 || longueur > 255)
				throw new IllegalArgumentException("Mauvaise longueur : " + longueur + " (code = " + code + ")");
			else if(longueur > 4 && code == IncomingCode.EXECUTION_BEGIN.code)
				throw new IllegalArgumentException("Trame EXECUTION_BEGIN de longueur incorrecte (" + longueur + ")");

			id = serieInput.read();
			message = new int[longueur - 4];
			for(int i = 0; i < message.length; i++)
				message[i] = serieInput.read();
			checksum = serieInput.read();
		}
		return new IncomingFrame(code, id, checksum, longueur, message);
	}

	/**
	 * Fermeture de la série
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void close() throws InterruptedException
	{
		// On attend de clore les conversations
		int nb = 0;
		while((!waitingFrames.isEmpty() || !pendingLongFrames.isEmpty()) && nb < 5)
		{
			if(nb == 0)
				log.debug("On attend la fin des conversations série…");
			Thread.sleep(100);
			nb++;
		}
		for(Integer id : waitingFrames)
		{
			Conversation c = conversations[id];
			log.warning("Waiting short frame : "+c.origine);
		}
		for(Integer id : pendingLongFrames)
		{
			Conversation c = conversations[id];
			log.warning("Pending long frame : "+c.origine);
		}
		serieOutput.close();
	}

	/**
	 * GESTION DES RENVOIS ET DES DESTRUCTIONS DE TRAMES
	 */

	/**
	 * Renvoie le temps avant qu'une trame doive être renvoyée (timeout sinon)
	 * 
	 * @return
	 */
	public synchronized int timeBeforeResend()
	{
		int out;
		if(!waitingFrames.isEmpty())
			out = conversations[waitingFrames.getFirst()].timeBeforeResend();
		else
			out = timeout;
		return Math.max(out, 0); // il faut envoyer un temps positif
	}

	/**
	 * Renvoie le temps avant qu'une trame fermée soit vraiment détruite
	 * 
	 * @return
	 */
	public synchronized int timeBeforeDeath()
	{
		int out;
		if(!closedFrames.isEmpty())
			out = conversations[closedFrames.getFirst()].timeBeforeDeath();
		else
			out = 2 * timeout;
		return Math.max(out, 0); // il faut envoyer un temps positif
	}

	/**
	 * Renvoie la trame la plus vieille qui en a besoin (possiblement aucune)
	 * 
	 * @throws InterruptedException
	 * @throws ClosedSerialException 
	 */
	public void resend() throws InterruptedException, ClosedSerialException
	{
		Conversation trame = null;

		synchronized(this)
		{
			if(!waitingFrames.isEmpty() && conversations[waitingFrames.getFirst()].needResend())
			{
				int id = waitingFrames.poll();
				trame = conversations[id];
				// On remet à la fin
				waitingFrames.add(id);
			}
		}

		if(trame != null)
		{
			log.debug("Une trame est renvoyée : " + trame.getFirstTrame(), Verbose.SERIE.masque);

			serieOutput.add(trame, trame.getFirstTrame().trame, trame.getFirstTrame().tailleTrame);
			// trame.updateResendDate(); // on remet la date de renvoi à plus
			// tard
		}
	}

	/**
	 * Tue les vieilles trames
	 */
	public synchronized void kill()
	{
		while(!closedFrames.isEmpty() && conversations[closedFrames.getFirst()].needDeath())
		{
			int id = closedFrames.getFirst();
			conversations[id].libre = true; // cet ID est maintenant libre
			closedFrames.removeFirst();
		}
	}

	public void init() throws InterruptedException
	{
		serieOutput.init();
	}
}
