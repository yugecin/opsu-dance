/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017-2018 yugecin
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

import itdelatrisu.opsu.Utils;

import static itdelatrisu.opsu.ui.Colors.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class NumericOption extends Option {

	public final int min;
	public final int max;
	public int val;

	public NumericOption(String name, String configurationName, String description, int val, int min, int max) {
		super(name, configurationName, description);
		this.min = min;
		this.max = max;
		this.val = val;
	}

	public void setValue(int val) {
		this.val = val;
	}

	@Override
	public String getValueString() {
		return String.format("%d%%", val);
	}

	@Override
	public String write() {
		return Integer.toString(val);
	}

	@Override
	public void read(String s) {
		try {
			val = Utils.clamp(Integer.parseInt(s), min, max);
		} catch (Exception ignored) {
			bubNotifs.send(BUB_RED, "Failed to parse '" + configurationName + "' option");
		}
	}

}
