// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.newdawn.slick.util.LogSystem;

import static yugecin.opsudance.core.Entrypoint.runtime;

class LogImpl implements LogSystem
{
	private final PrintStream fileOut;

	LogImpl()
	{
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(Entrypoint.LOGFILE, /*append*/ true);
		} catch (Throwable t) {
			System.err.println("Could not open log file, logging will be console only");
			t.printStackTrace();
		}

		if (fout != null) {
			this.fileOut = new PrintStream(fout, /*autoFlush*/ true);
		} else {
			this.fileOut = new PrintStream(new OutputStream() {
				@Override
				public void write(int b) throws IOException
				{
				}
				
			}, /*autoFlush*/ true);
		}

		this.metalog("\nSession Start: " + new Date().toString());
	}

	void close()
	{
		this.metalog("Session Close: " + new Date().toString());
		this.fileOut.close();
	}

	private void metalog(String entry)
	{
		this.fileOut.println(entry);
		System.out.println(entry);
	}

	@Override
	public void error(String message, Throwable e)
	{
		synchronized (this) {
			final String msg = String.format("[%8d] ERROR: %s", runtime(), message);
			System.err.println(msg);
			this.fileOut.println(msg);
			this.printStackTrace(e, System.err, "[        ] ERROR: ");
		}
	}

	@Override
	public void error(Throwable e)
	{
		this.error("", e);
	}

	@Override
	public void error(String message)
	{
		synchronized (this) {
			final String msg = String.format("[%8d] ERROR: %s", runtime(), message);
			System.err.println(msg);
			this.fileOut.println(msg);
		}
	}

	@Override
	public void warn(String message)
	{
		synchronized(this) {
			final String msg = String.format("[%8d] WARN: %s", runtime(), message);
			System.out.println(msg);
			this.fileOut.println(msg);
		}
	}

	@Override
	public void warn(String message, Throwable e)
	{
		synchronized (this) {
			final String msg = String.format("[%8d] WARN: %s", runtime(), message);
			System.out.println(msg);
			this.fileOut.println(msg);
			this.printStackTrace(e, System.out, "[        ] WARN: ");
		}
	}

	@Override
	public void info(String message)
	{
		synchronized (this) {
			final String msg = String.format("[%8d] INFO: %s", runtime(), message);
			System.out.println(msg);
			this.fileOut.println(msg);
		}
	}

	@Override
	public void debug(String message)
	{
		synchronized (this) {
			final String msg = String.format("[%8d] DEBUG: %s", runtime(), message);
			System.out.println(msg);
			this.fileOut.println(msg);
		}
	}

	private void printStackTrace(Throwable t, PrintStream primaryOutput, String prefix)
	{
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		final StringBuffer buf = sw.getBuffer();
		final char[] chars = new char[buf.length()];
		buf.getChars(0, chars.length, chars, 0);
		primaryOutput.print(prefix);
		this.fileOut.print(prefix);
		for (int i = 0; i < chars.length;) {
			char c = chars[i++];
			if (c == '\r') {
				if (i < chars.length && chars[i] == '\n') {
					continue;
				}
				c = '\n';
			}
			if (c == '\n') {
				primaryOutput.print('\n');
				this.fileOut.print('\n');
				if (i < chars.length) {
					primaryOutput.print(prefix);
					this.fileOut.print(prefix);
					if (chars[i] == '\t') {
						chars[i] = ' ';
					}
				}
				continue;
			}
			primaryOutput.print(c);
			this.fileOut.print(c);
		}
		this.fileOut.flush();
	}
}
