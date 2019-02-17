// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor.colors;

import java.awt.Color;

import static org.lwjgl.opengl.GL11.glColor3f;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.ui.cursor.colors.DistanceRainbow.saturation;

class TimeRainbow extends CursorColor
{
	private float hue;

	TimeRainbow(String name)
	{
		super(name);
	}

	@Override
	public void update()
	{
		this.hue += OPTION_DANCE_RGB_CURSOR_INC.val / 360f / 1000f * displayContainer.delta;
		this.hue = this.hue - (float) Math.floor(this.hue);
	}

	@Override
	public int getMovementColor(float movementProgress)
	{
		return Color.HSBtoRGB(this.hue, saturation, 1.0f);
	}

	@Override
	public int getCurrentColor()
	{
		return Color.HSBtoRGB(this.hue, saturation, 1.0f);
	}

	@Override
	public void bindCurrentColor()
	{
		final int val = Color.HSBtoRGB(this.hue, saturation, 1.0f);
		glColor3f(
			((val >> 16) & 0xFF) / 255f,
			((val >> 8) & 0xFF) / 255f,
			(val & 0xFF) / 255f
		);
	}

	@Override
	void reset(int seed)
	{
		this.hue = (seed % 360) / 360f;
	}
}
