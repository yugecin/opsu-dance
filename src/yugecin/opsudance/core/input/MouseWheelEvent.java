// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

public class MouseWheelEvent extends Event
{
	public final int delta;

	MouseWheelEvent(int delta)
	{
		this.delta = delta;
	}
}
