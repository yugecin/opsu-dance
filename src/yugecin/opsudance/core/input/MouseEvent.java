// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

import static yugecin.opsudance.core.InstanceContainer.*;

public class MouseEvent extends Event
{
	public final int button, x, y;

	MouseEvent(int button, int x, int y)
	{
		this.button = button;
		this.x = x;
		this.y = y;
	}

	public boolean dragDistanceExceeds(int distance)
	{
		final int dx = this.x - mousePressX;
		final int dy = this.y - mousePressY;
		return dx * dx + dy * dy > distance * distance;
	}
}
