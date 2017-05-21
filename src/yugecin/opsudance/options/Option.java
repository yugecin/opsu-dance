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
package yugecin.opsudance.options;

import static yugecin.opsudance.core.InstanceContainer.*;

public class Option {

	public final String name;
	public final String configurationName;
	public final String description;

	/**
	 * If this option should not be shown in the optionsmenu because it does
	 * not match the search string.
	 */
	private boolean filtered;

	/**
	 * Constructor for internal options (not displayed in-game).
	 */
	public Option(String configurationName) {
		this(null, configurationName, null);
	}

	public Option(String name, String configurationName, String description) {
		this.name = name;
		this.configurationName = configurationName;
		this.description = description;
		optionservice.registerOption(this);
	}

	/**
	 * should the option be shown
	 * @return true if the option should be shown
	 */
	public boolean showCondition() {
		return true;
	}

	public String getValueString() {
		return "";
	}

	public String write() {
		return getValueString();
	}

	public void read(String s) {
	}

	/**
	 * Update the filtered flag for this option based on the given searchString.
	 * @param searchString the searched string or null to reset the filtered flag
	 * @return true if this option does need to be filtered
	 */
	public boolean filter(String searchString) {
		if (searchString == null || searchString.length() == 0) {
			filtered = false;
			return false;
		}
		filtered = !name.toLowerCase().contains(searchString) && !description.toLowerCase().contains(searchString);
		return filtered;
	}

	/**
	 * Check if this option should be filtered (= not shown) because it does not
	 * match the search string.
	 * @return true if the option shouldn't be shown.
	 */
	public boolean isFiltered() {
		return filtered;
	}

}
