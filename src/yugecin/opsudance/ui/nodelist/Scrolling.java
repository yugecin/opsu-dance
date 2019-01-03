// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import static itdelatrisu.opsu.Utils.clamp;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;

class Scrolling
{
	private float max;

	private float positionTo;
	private float positionFrom;
	private int updateTime;
	private static final int UPDATE_TIME = 1500;
	private boolean lastDirection;
	private long lastOffsetTime;

	float position;
	float positionNorm;
	float scrollProgress;

	void setMax(float max)
	{
		this.max = max;
	}

	void addOffset(float offset)
	{
		final long time = System.currentTimeMillis();
		this.positionFrom = this.position;
		final boolean newDirection = offset > 0;
		if (newDirection ^ this.lastDirection) {
			this.lastDirection = newDirection;
			this.positionTo = this.position + offset;
		} else {
			if (time - lastOffsetTime < 100) {
				//offset *= (1f - IN_EXPO.calc((time - lastOffsetTime) / 100)) * this.max * 0.00003f;
			}
			this.positionTo += offset;
		}
		lastOffsetTime = time;
		this.updateTime = UPDATE_TIME;
	}

	void update(int delta)
	{
		if (this.updateTime > 0) {
			if ((this.updateTime -= delta) < 0) {
				this.updateTime = 0;
			}
			final float dpos = (this.positionTo - this.positionFrom);
			this.scrollProgress = OUT_EXPO.calc(
				(float) (UPDATE_TIME - this.updateTime) / UPDATE_TIME
			);
			this.position = clamp(
				this.positionFrom + this.scrollProgress * dpos,
				0f,
				this.max
			);
			this.positionNorm = this.position / this.max;
		}
	}
}
