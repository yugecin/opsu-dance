// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

public class MouseDragEvent extends Event
{
	public final int dx, dy;

	MouseDragEvent(int dx, int dy)
	{
		this.dx = dx;
		this.dy = dy;
	}
}
