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

import yugecin.opsudance.core.NotNull;
import yugecin.opsudance.core.Nullable;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestWrapper {

	@Nullable
	public final Manifest manifest;

	public ManifestWrapper(@Nullable Manifest manifest) {
		this.manifest = manifest;
	}

	/**
	 * @param attribute attribute in jarfile or null for default attributes
	 */
	public String valueOrDefault(@Nullable String attribute, @NotNull String key, @Nullable String dfault) {
		if (manifest == null) {
			return dfault;
		}
		Attributes attributes =
			attribute == null ? manifest.getMainAttributes() : manifest.getAttributes(attribute);
		if (attributes == null) {
			return dfault;
		}
		String val = attributes.getValue(key);
		if (val == null) {
			return dfault;
		}
		return val;
	}

}
