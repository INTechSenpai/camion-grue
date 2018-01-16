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
