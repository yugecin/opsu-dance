// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import org.newdawn.slick.util.Log;

public class Environment
{
	/**
	 * a 40-character SHA-1 hash denoting the revision the current branch is on,
	 * or {@code null} if a jar is running or it could not be determined
	 */
	public final String gitHash;

	public Environment()
	{
		if (Entrypoint.isJarRunning) {
			this.gitHash = null;
		} else {
			this.gitHash = this.findGitHash();
			Log.info("rev: " + this.gitHash);
		}
	}

	@Nullable
	private String findGitHash()
	{
		File root = Entrypoint.workingdir;
		File gitdir;
		for (int i = 4;;) {
			gitdir = new File(root, ".git");
			if (gitdir.exists()) {
				break;
			}
			root = root.getParentFile();
			if (root == null || --i < 0) {
				Log.info("no git root detected");
				return null;
			}
		}

		String ref = "refs/heads/master";
		File HEAD = new File(gitdir, "HEAD");
		if (HEAD.exists()) {
			 try (InputStream in = new FileInputStream(HEAD)) {
				 int off = 0;
				 final byte[] buffer = new byte[256];
				 for (;;) {
					 int read = in.read(buffer, off, buffer.length - off);
					 if (read == -1) {
						 break;
					 }
					 off += read;
					 if (buffer.length - off == 0) {
						 throw new Exception("ref too long");
					 }
				 }
				 if (off > 0 &&
					 (buffer[off - 1] == '\n' || buffer[off - 1] == '\r'))
				 {
					--off;
				 }
				 String _ref = new String(buffer, 0, off);
				 if (!_ref.startsWith("ref: ")) {
					 throw new Exception("unexpected content");
				 }
				 ref = _ref.substring(5);
			 } catch (Throwable t) {
				 Log.warn("could not parse HEAD file " + HEAD.getAbsolutePath(), t);
			 }
		}

		File head;
		try {
			head = Paths.get(gitdir.getAbsolutePath(), ref.split("/")).toFile();
		} catch (Throwable t) {
			Log.warn("could not compose head file, ref: " + ref);
			return null;
		}

		if (!head.exists()) {
			Log.warn("could not find revision file " + head.getAbsolutePath());
			return null;
		}

		try (InputStream in = new FileInputStream(head)) {
			int off = 0;
			byte[] sha = new byte[40];
			for (;;) {
				int read = in.read(sha, off, sha.length - off);
				if (read == -1) {
					throw new Exception("unexpected EOF");
				}
				off += read;
				if (off == 40) {
					break;
				}
			}
			for (int i = sha.length; i > 0;) {
				final char c = (char) sha[--i];
				if (
					(c < '0' && '9' < c) &&
					(c < 'a' && 'f' < c) &&
					(c < 'A' && 'F' < c))
				{
					throw new Exception(
						"illegal character at index " + i + ": " + c
					);
				}
			}
			return new String(sha);
		} catch (Throwable t) {
			Log.warn("could not read revision file " + head.getAbsolutePath(), t);
			return null;
		}
	}
}
