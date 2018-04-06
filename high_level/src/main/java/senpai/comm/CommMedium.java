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

package senpai.comm;

import java.io.InputStream;
import java.io.OutputStream;
import pfg.config.Config;

/**
 * Une interface de medium de communication : série ou ethernet
 * @author pf
 *
 */

public interface CommMedium
{
	public void initialize(Config config);
	public boolean openIfClosed() throws InterruptedException;
	public void open(int delayBetweenTries) throws InterruptedException;
	public void close();
	public OutputStream getOutput();
	public InputStream getInput();
}
