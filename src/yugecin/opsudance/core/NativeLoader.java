// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.jar.JarFile;

import org.lwjgl.LWJGLUtil;
import org.newdawn.slick.util.Log;

/**
 * originally {@link itdelatrisu.opsu.NativeLoader}
 */
class NativeLoader
{
	static void loadNatives() throws Exception
	{
		final File nativedir = new File(Entrypoint.workingdir, "Natives");
		if (!nativedir.exists()) {
			Files.createDirectory(nativedir.toPath());
		}
		if (!nativedir.isDirectory()) {
			throw new IOException(
				"Native path '" + nativedir.getAbsolutePath()
				+ "' is not a directory"
			);
		}

		final String[] files = new String[][] {
			{ "liblwjgl.so", "liblwjgl64.so", "libopenal.so", "libopenal64.so" },
			{ "liblwjgl.dylib", "openal.dylib" },
			{ "OpenAL32.dll", "OpenAL64.dll", "lwjgl.dll", "lwjgl64.dll" }
		}[LWJGLUtil.getPlatform() - 1];

		for (int i = 0; i < files.length; i++) {
			String name = files[i];
			File unpackedFile = new File(nativedir, name);
			if (unpackedFile.exists()) {
				continue;
			}
			try (JarFile jar = new JarFile(findNativesJar())) {
				o: for (;;) {
					unpack(jar, unpackedFile, name);
					do {
						if (++i >= files.length) {
							break o;
						}
						name = files[i];
						unpackedFile = new File(nativedir, name);
					} while (unpackedFile.exists());
				}
			}
		}

		setNativePath(nativedir.getAbsolutePath());
	}

	private static File findNativesJar() throws IOException
	{
		if (Entrypoint.isJarRunning) {
			return Entrypoint.jarfile;
		}

		final String target = new String[] {
			"natives-linux.jar",
			"natives-osx.jar",
			"natives-windows.jar",
		}[LWJGLUtil.getPlatform() - 1];

		final String classpath = System.getProperty("java.class.path");
		for (String entry : classpath.split(System.getProperty("path.separator"))) {
			if (!entry.endsWith(target)) {
				continue;
			}
			File jarfile = new File(entry);
			if (jarfile.exists()) {
				return jarfile;
			}
			jarfile = new File(Entrypoint.workingdir, entry);
			if (jarfile.exists()) {
				return jarfile;
			}
			throw new IOException(
				"Cannot find jar file referred in classpath: "
				+ entry + "."
				+ " Tried absolute and relative to workingdir "
				+ Entrypoint.workingdir
			);
		}

		throw new IOException(
			"Cannot find jar file containing natives (searching for '*" + target + ")'."
			+" Is the classpath correct?"
		);
	}

	private static void unpack(JarFile jarfile, File destination, String filename)
		throws IOException
	{
		try (
			InputStream in = jarfile.getInputStream(jarfile.getEntry(filename));
			OutputStream out = new FileOutputStream(destination))
		{
			byte[] buffer = new byte[65536];
			int bufferSize;
			while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1) {
				out.write(buffer, 0, bufferSize);
			}
		}
	}

	private static void setNativePath(String path)
	{
		System.setProperty("org.lwjgl.librarypath", path);
		System.setProperty("java.library.path", path);

		try {
			// reset variable so it will re-read the properties that were just set
			// http://stackoverflow.com/a/24988095
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (Exception e) {
			Log.warn("Failed to reset 'sys_paths' field.", e);
		}
	}
}
