// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.options;

import org.lwjgl.input.Keyboard;

public class KeyOption extends Option
{
	public int keycode;

	public KeyOption(String name, String configurationName, String description, int keycode)
	{
		super(name, configurationName, description);
		this.keycode = keycode;
	}

	@Override
	public String getValueString ()
	{
		return Keyboard.getKeyName(this.keycode);
	}

	@Override
	public String write ()
	{
		return Keyboard.getKeyName(this.keycode);
	}

	@Override
	public void read(String s)
	{
		this.keycode = Keyboard.getKeyIndex(s);
		if (this.keycode == Keyboard.KEY_NONE) {
			this.keycode = Keyboard.KEY_X;
		}
	}

	public void setKeycode(int keycode)
	{
		this.keycode = keycode;
		this.notifyListeners();
	}
}
