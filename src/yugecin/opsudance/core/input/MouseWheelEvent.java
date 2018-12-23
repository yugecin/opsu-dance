// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

public class MouseWheelEvent extends Event
{
	public final int delta;
	/**
	 * {@code 1} is up, {@code -1} is down
	 */
	public final int direction;

	MouseWheelEvent(int delta)
	{
		this.delta = delta;
		this.direction = (1 - ((delta & 0x80000000) >>> 30));
	}
}
