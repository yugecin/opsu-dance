/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance;

import itdelatrisu.opsu.NativeLoader;
import itdelatrisu.opsu.Options;
import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class PreStartupInitializer {

	public PreStartupInitializer() {
		loadNatives();
		setResourcePath();
	}

	private void loadNatives() {
		File nativeDir = loadNativesUsingOptionsPath();

		System.setProperty("org.lwjgl.librarypath", nativeDir.getAbsolutePath());
		System.setProperty("java.library.path", nativeDir.getAbsolutePath());

		try {
			// Workaround for "java.library.path" property being read-only.
			// http://stackoverflow.com/a/24988095
			Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			Log.warn("Failed to set 'sys_paths' field.", e);
		}
	}

	private File loadNativesUsingOptionsPath() {
		File nativeDir = Options.NATIVE_DIR;
		try {
			new NativeLoader(nativeDir).loadNatives();
		} catch (IOException e) {
			Log.error("Error loading natives.", e);
		}
		return nativeDir;
	}

	private void setResourcePath() {
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));
	}

}
