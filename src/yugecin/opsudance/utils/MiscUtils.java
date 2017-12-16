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
package yugecin.opsudance.utils;

import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;
import yugecin.opsudance.core.Constants;

import java.io.IOException;
import java.util.Properties;

public class MiscUtils {

	public static final CachedVariable<Properties> buildProperties = new CachedVariable<>(new CachedVariable.Getter<Properties>() {
		@Override
		public Properties get() {
			Properties props = new Properties();
			try {
				props.load(ResourceLoader.getResourceAsStream(Constants.VERSION_FILE));
			} catch (IOException e) {
				Log.error("Could not read version file", e);
			}
			return props;
		}
	});

}
