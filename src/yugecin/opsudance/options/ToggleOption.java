// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.options;

public class ToggleOption extends Option
{
	public boolean state;

	public ToggleOption(String name, String configurationName, String description, boolean state) {
		super(name, configurationName, description);
		this.state = state;
	}

	public void toggle() {
		this.state = !this.state;
		this.notifyListeners();
	}

	@Override
	public String write() {
		return Boolean.toString(state);
	}

	@Override
	public void read(String s) {
		state = Boolean.parseBoolean(s);
	}
}
