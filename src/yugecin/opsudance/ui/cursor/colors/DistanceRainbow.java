// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor.colors;

import java.awt.Color;

import static org.lwjgl.opengl.GL11.glColor3f;
import static yugecin.opsudance.options.Options.*;

class DistanceRainbow extends CursorColor
{
	static float saturation;

	private float hue;

	private float hueBeforeMove, hueIncrease;

	DistanceRainbow(String name)
	{
		super(name);
		this.updateSaturation();
		OPTION_RAINBOWTRAIL_SATURATION.addListener(this::updateSaturation);
	}

	private void updateSaturation()
	{
		saturation = OPTION_RAINBOWTRAIL_SATURATION.val / 100f;
	}

	@Override
	public void onMovement(int x1, int y1, int x2, int y2)
	{
		this.hueIncrease = (float) Math.hypot(x2 - x1, y2 - y1) / 1080f;
		this.hueIncrease *= OPTION_DANCE_RGB_CURSOR_INC.val / 180f;
		this.hueBeforeMove = this.hue;
		this.hue += this.hueIncrease;
		this.hue = this.hue - (float) Math.floor(this.hue);
	}

	@Override
	public int getMovementColor(float movementProgress)
	{
		float hue = this.hueBeforeMove + this.hueIncrease * movementProgress;
		hue = hue - (float) Math.floor(hue);
		return Color.HSBtoRGB(hue, saturation, 1f);
	}

	@Override
	public int getCurrentColor()
	{
		return Color.HSBtoRGB(this.hue, saturation, 1f);
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
