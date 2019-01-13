// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor.colors;

import org.newdawn.slick.Color;

class Fixed extends CursorColor
{
	final Color col;
	final int color;

	Fixed(String name, int color)
	{
		super(name);
		this.col = new Color(color);
		this.color = color;
	}

	@Override
	public int getMovementColor(float movementProgress)
	{
		return this.color;
	}

	@Override
	public int getCurrentColor()
	{
		return this.color;
	}

	@Override
	public void bindCurrentColor()
	{
		this.col.bind();
	}
}
