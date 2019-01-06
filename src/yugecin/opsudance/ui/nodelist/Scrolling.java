// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import static itdelatrisu.opsu.Utils.clamp;
import static itdelatrisu.opsu.ui.KineticScrolling.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;

/**
 * based on {@link itdelatrisu.opsu.ui.KineticScrolling}
 */
public class Scrolling
{
	private float max;

	private boolean mouseDown;
	private boolean lastDirection;
	private long lastOffsetTime;
	private float avgVelocity;
	private int dragOffset;

	float position;
	float positionNorm;
	float scrollProgress;

	private float target, amplitude;
	private int totalDelta;
	private static final int TIME_CONST = 200;

	void setMax(float max)
	{
		this.max = max;
	}

	void addOffset(float offset)
	{
		final long time = System.currentTimeMillis();
		final boolean newDirection = offset > 0;
		if (newDirection ^ this.lastDirection) {
			this.lastDirection = newDirection;
			this.target = this.position + offset;
			this.lastOffsetTime = 0l;
		} else {
			if (time - lastOffsetTime < 75) {
				// boost is only actually intended for mouse wheel invocations,
				// but updates this fast are pretty much only possible when using
				// the mousewheel, soooo...
				final float boost = (1f - ((time - lastOffsetTime) / 75f));
				offset *= 1f + IN_CIRC.calc(boost) * 10f;
			}
			this.target += offset;
			this.lastOffsetTime = time;
		}
		this.totalDelta = 0;
		this.amplitude = this.target - this.position;
	}

	void setPosition(float position)
	{
		this.target = position;
		this.position = target;
		this.amplitude = 0f;
	}

	public void scrollToNorm(float norm)
	{
		this.scrollToPosition(clamp(norm * this.max, 0f, this.max));
	}

	void scrollToPosition(float position)
	{
		this.amplitude = position - this.position;
		this.target = position;
		this.totalDelta = 0;
	}

	void update(int delta)
	{
		if (this.mouseDown) {
			final float avg_not_so_const = .3f * delta / 16f;
			this.avgVelocity =
				(1f - avg_not_so_const) * avgVelocity
				+ avg_not_so_const * (this.dragOffset * 1000f / delta);
			this.target = this.position += this.dragOffset;
			this.dragOffset = 0;
			return;
		}


		final float progress = (float) ((this.totalDelta += delta)
			* Math.log10(Math.abs(this.amplitude)) / 2f / TIME_CONST);
		this.position = clamp(
			this.target + (float) (-this.amplitude * Math.exp(-progress)),
			0f,
			this.max
		);
		this.positionNorm = this.position / this.max;
	}

	public void pressed()
	{
		if (!this.mouseDown) {
			this.mouseDown = true;
			this.avgVelocity = 0;
			this.dragOffset = 0;
		}
	}

	public void released()
	{
		if (this.mouseDown) {
			this.mouseDown = false;
			this.amplitude = AMPLITUDE_CONST * this.avgVelocity;
			this.target += amplitude;
			this.totalDelta = 0;
		}
	}

	public void dragged(int ydistance)
	{
		if (this.mouseDown) {
			this.dragOffset += ydistance;
		}
	}
}
