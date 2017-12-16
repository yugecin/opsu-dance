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
package yugecin.opsudance.core;

import java.io.File;
import java.nio.file.Paths;

import static yugecin.opsudance.core.Constants.PROJECT_NAME;

public class Environment {

	public final boolean isJarRunning;
	public final File workingdir;
	public final File jarfile;

	public Environment() {
		Class thiz = Environment.class;
		String thisClassLocation = thiz.getResource(thiz.getSimpleName() + ".class").toString();
		this.isJarRunning = thisClassLocation.startsWith("jar:");
		if (!isJarRunning) {
			this.workingdir = Paths.get(".").toAbsolutePath().normalize().toFile();
			this.jarfile = null;
		} else {
			String wdir = thisClassLocation.substring(9); // remove jar:file:
			String separator = "!/";
			int separatorIdx = wdir.indexOf(separator);
			int lastSeparatorIdx = wdir.lastIndexOf(separator);
			if (separatorIdx != lastSeparatorIdx) {
				String msg = String.format("%s cannot run from paths containing '!/', please move the file. Current directory: %s",
					PROJECT_NAME, thisClassLocation.substring(0, lastSeparatorIdx));
				throw new RuntimeException(msg);
			}
			this.jarfile = new File(wdir.substring(0, separatorIdx));
			this.workingdir = jarfile.getParentFile();
		}
	}

}
