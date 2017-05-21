/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu;

import org.newdawn.slick.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.jar.JarEntry;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * Native loader, based on the JarSplice launcher.
 *
 * @author http://ninjacave.com
 */
public class NativeLoader {

	public static void loadNatives() {
		try {
			unpackNatives();
		} catch (IOException e) {
			String msg = String.format("Could not unpack native(s): %s", e.getMessage());
			throw new RuntimeException(msg, e);
		}

		String nativepath = config.NATIVE_DIR.getAbsolutePath();
		System.setProperty("org.lwjgl.librarypath", nativepath);
		System.setProperty("java.library.path", nativepath);

		try {
			// Workaround for "java.library.path" property being read-only.
			// http://stackoverflow.com/a/24988095
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (Exception e) {
			Log.warn("Failed to set 'sys_paths' field.", e);
		}
	}

	/**
	 * Unpacks natives for the current operating system to the natives directory.
	 * @throws IOException if an I/O exception occurs
	 */
	public static void unpackNatives() throws IOException {
		if (env.jarfile == null) {
			return;
		}

		if (!config.NATIVE_DIR.exists() && !config.NATIVE_DIR.mkdir()) {
			String msg = String.format("Could not create folder '%s'",
				config.NATIVE_DIR.getAbsolutePath());
			throw new RuntimeException(msg);
		}


		Enumeration<JarEntry> entries = env.jarfile.entries();
		while (entries.hasMoreElements()) {
			JarEntry e = entries.nextElement();
			if (e == null)
				break;

			File f = new File(config.NATIVE_DIR, e.getName());
			if (isNativeFile(e.getName()) && !e.isDirectory() && e.getName().indexOf('/') == -1 && !f.exists()) {
				InputStream in = env.jarfile.getInputStream(env.jarfile.getEntry(e.getName()));
				OutputStream out = new FileOutputStream(f);

				byte[] buffer = new byte[65536];
				int bufferSize;
				while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1)
					out.write(buffer, 0, bufferSize);

				in.close();
				out.close();
			}
		}

		env.jarfile.close();
	}

	/**
	 * Returns whether the given file name is a native file for the current operating system.
	 * @param entryName the file name
	 * @return true if the file is a native that should be loaded, false otherwise
	 */
	private static boolean isNativeFile(String entryName) {
		String osName = System.getProperty("os.name");
		String name = entryName.toLowerCase();

		if (osName.startsWith("Win")) {
			if (name.endsWith(".dll"))
				return true;
		} else if (osName.startsWith("Linux")) {
			if (name.endsWith(".so"))
				return true;
		} else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
			if (name.endsWith(".dylib") || name.endsWith(".jnilib"))
				return true;
		}
		return false;
	}

}