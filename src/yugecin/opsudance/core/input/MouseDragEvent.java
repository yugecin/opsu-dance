// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

public class MouseDragEvent extends Event
{
	public final int button, dx, dy;

	MouseDragEvent(int button, int dx, int dy)
	{
		this.button = button;
		this.dx = dx;
		this.dy = dy;
	}
}
