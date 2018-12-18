// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.options;

import itdelatrisu.opsu.Utils;

import static itdelatrisu.opsu.ui.Colors.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class NumericOption extends Option
{
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
	
	public float percentage()
	{
		return (float) (val - min) / (max - min);
	}
}
