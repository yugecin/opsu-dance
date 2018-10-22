/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2018 yugecin
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
package yugecin.opsudance.utils;

import itdelatrisu.opsu.Utils;
import yugecin.opsudance.core.NotNull;
import yugecin.opsudance.core.Nullable;

public class SimpleVersion implements Comparable<SimpleVersion> {

	@Nullable
	public static SimpleVersion parse(@NotNull String version) {
		int dashpos = version.indexOf('-');
		if (dashpos != -1) {
			version = version.substring(0, dashpos);
		}
		final String[] parts = version.split("\\.");
		if (parts.length < 3) {
			return null;
		}
		try {
			return new SimpleVersion(
				Integer.parseInt(parts[0]),
				Integer.parseInt(parts[1]),
				Integer.parseInt(parts[2])
			);
		} catch (Exception e) {
			return null;
		}
	}

	final int major, minor, incremental;

	public SimpleVersion(int major, int minor, int incremental) {
		this.major = major;
		this.minor = minor;
		this.incremental = incremental;
	}

	@Override
	public int compareTo(@NotNull SimpleVersion o) {
		return 
			Utils.clamp(Integer.compare(major, o.major), -1, 1) * 100 + 
			Utils.clamp(Integer.compare(minor, o.minor), -1, 1) * 10 + 
			Utils.clamp(Integer.compare(incremental, o.incremental), -1, 1);
	}

	@Override
	public String toString() {
		return "" + major + '.' + minor + '.' + incremental;
	}
}
