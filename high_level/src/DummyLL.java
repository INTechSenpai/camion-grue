import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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
 * Test de communication
 * @author pf
 *
 */

public class DummyLL {

	public static void main(String[] args) throws IOException
	{
		Socket socketduserveur;
		ServerSocket socketserver = new ServerSocket(22222);
		try {
			socketduserveur = socketserver.accept();
			BufferedReader input = new BufferedReader(new InputStreamReader(socketduserveur.getInputStream()));
			int read, length = 0;
			OutputStream output = socketduserveur.getOutputStream();
			do {
				length++;
				read = input.read();
				System.out.println(read);
			} while(read != -1 && length < 4);
			output.write(0xFF);
			output.write(0x00);
			output.write(0x01);
			output.write(0x42);
			System.out.println("Envoi terminé");
		} finally {
			socketserver.close();
		}
	}
	
}
