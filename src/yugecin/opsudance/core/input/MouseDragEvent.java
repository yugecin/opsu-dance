// Copyright 2018-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

public class MouseDragEvent extends Event
{
	public final int button, dx, dy, downX, downY;
	public final float distance;

	MouseDragEvent(MouseEvent downEvent, int button, int dx, int dy)
	{
		this.button = button;
		this.dx = dx;
		this.dy = dy;
		this.distance = (float) Math.hypot(dx, dy);
		this.downX = downEvent.x;
		this.downY = downEvent.y;
		downEvent.dragDistance += this.distance;
	}
}
