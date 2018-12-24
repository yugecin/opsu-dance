// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

public class KeyEvent extends Event
{
	public final int keyCode;
	public final char chr;

	KeyEvent(int keyCode, char chr)
	{
		this.keyCode = keyCode;
		this.chr = chr;
	}
}
