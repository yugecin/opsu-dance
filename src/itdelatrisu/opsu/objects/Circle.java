/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.objects;

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameData.HitObjectType;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.objects.curves.Vec2f;
import itdelatrisu.opsu.ui.Colors;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.Dancer;
import yugecin.opsudance.ObjectColorOverrides;

import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * Data type representing a circle object.
 */
public class Circle extends GameObject {

	/** The associated HitObject. */
	private HitObject hitObject;

	/** The scaled starting x, y coordinates. */
	private float x, y;

	/** The associated GameData object. */
	private GameData data;

	/** The color of this circle. */
	private Color color;
	private Color mirrorColor;

	/** Whether or not the circle result ends the combo streak. */
	private boolean comboEnd;

	private int comboColorIndex;

	/**
	 * Constructor.
	 * @param hitObject the associated HitObject
	 * @param data the associated GameData object
	 * @param comboColorIndex index of the combo color of this circle
	 * @param comboEnd true if this is the last hit object in the combo
	 */
	public Circle(HitObject hitObject, GameData data, int comboColorIndex, boolean comboEnd) {
		this.hitObject = hitObject;
		this.data = data;
		this.comboEnd = comboEnd;
		this.comboColorIndex = comboColorIndex;
		updateColor();
		updatePosition();
	}

	public Circle(float x, float y, int time) {
		hitObject = new HitObject(x, y, time);
		super.updateStartEndPositions(time);
	}

	@Override
	public void draw(Graphics g, int trackPosition, boolean mirror) {
		Color orig = color;
		if (mirror) {
			color = mirrorColor;
		}

		int timeDiff = hitObject.getTime() - trackPosition;
		final int approachTime = gameState.getApproachTime();
		final int fadeInTime = gameState.getFadeInTime();
		float scale = timeDiff / (float) approachTime;
		float approachScale = 1 + scale * 3;
		float fadeinScale = (timeDiff - approachTime + fadeInTime) / (float) fadeInTime;
		float alpha = Utils.clamp(1 - fadeinScale, 0, 1);

		g.pushTransform();
		if (mirror) {
			g.rotate(x, y, -180f);
		}

		if (GameMod.HIDDEN.isActive()) {
			final int hiddenDecayTime = gameState.getHiddenDecayTime();
			final int hiddenTimeDiff = gameState.getHiddenTimeDiff();
			if (fadeinScale <= 0f && timeDiff < hiddenTimeDiff + hiddenDecayTime) {
				float hiddenAlpha = (timeDiff < hiddenTimeDiff) ? 0f : (timeDiff - hiddenTimeDiff) / (float) hiddenDecayTime;
				alpha = Math.min(alpha, hiddenAlpha);
			}
		}

		float oldAlpha = Colors.WHITE_FADE.a;
		Colors.WHITE_FADE.a = color.a = alpha;

		if (timeDiff >= 0) {
			gameObjectRenderer.renderApproachCircle(x, y, color, approachScale);
		}
		gameObjectRenderer.renderHitCircle(x, y, color, hitObject.getComboNumber(), alpha);

		Colors.WHITE_FADE.a = oldAlpha;

		g.popTransform();
		color = orig;
	}

	/**
	 * Calculates the circle hit result.
	 * @param time the hit object time (difference between track time)
	 * @return the hit result (GameData.HIT_* constants)
	 */
	private int hitResult(int time) {
		int timeDiff = Math.abs(time);

		int[] hitResultOffset = gameState.getHitResultOffsets();
		int result = -1;
		if (timeDiff <= hitResultOffset[GameData.HIT_300])
			result = GameData.HIT_300;
		else if (timeDiff <= hitResultOffset[GameData.HIT_100])
			result = GameData.HIT_100;
		else if (timeDiff <= hitResultOffset[GameData.HIT_50])
			result = GameData.HIT_50;
		else if (timeDiff <= hitResultOffset[GameData.HIT_MISS])
			result = GameData.HIT_MISS;
		//else not a hit

		return result;
	}

	@Override
	public boolean mousePressed(int x, int y, int trackPosition) {
		double distance = Math.hypot(this.x - x, this.y - y);
		if (distance < gameObjectRenderer.circleDiameter / 2) {
			int timeDiff = trackPosition - hitObject.getTime();
			int result = hitResult(timeDiff);

			if (result > -1) {
				data.addHitError(hitObject.getTime(), x, y, timeDiff);
				data.sendHitResult(trackPosition, result, this.x, this.y, color, comboEnd, hitObject, HitObjectType.CIRCLE, true, 0, null, false);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean update(boolean overlap, int delta, int mouseX, int mouseY, boolean keyPressed, int trackPosition) {
		int time = hitObject.getTime();

		int[] hitResultOffset = gameState.getHitResultOffsets();
		boolean isAutoMod = GameMod.AUTO.isActive();

		if (trackPosition > time + hitResultOffset[GameData.HIT_50]) {
			if (isAutoMod) {// "auto" mod: catch any missed notes due to lag
				data.sendHitResult(time, GameData.HIT_300, x, y, color, comboEnd, hitObject, HitObjectType.CIRCLE, true, 0, null, false);
				if (OPTION_DANCE_MIRROR.state && GameMod.AUTO.isActive()) {
					float[] m = Utils.mirrorPoint(x, y);
					data.sendHitResult(time, GameData.HIT_300, m[0], m[1], mirrorColor, comboEnd, hitObject, HitObjectType.CIRCLE, true, 0, null, false, false);
				}
			}

			else  // no more points can be scored, so send a miss
				data.sendHitResult(trackPosition, GameData.HIT_MISS, x, y, null, comboEnd, hitObject, HitObjectType.CIRCLE, true, 0, null, false);
			return true;
		}

		// "auto" mod: send a perfect hit result
		else if (isAutoMod) {
			if (Math.abs(trackPosition - time) < hitResultOffset[GameData.HIT_300]) {
				data.sendHitResult(time, GameData.HIT_300, x, y, color, comboEnd, hitObject, HitObjectType.CIRCLE, true, 0, null, false);
				if (OPTION_DANCE_MIRROR.state && GameMod.AUTO.isActive()) {
					float[] m = Utils.mirrorPoint(x, y);
					data.sendHitResult(time, GameData.HIT_300, m[0], m[1], mirrorColor, comboEnd, hitObject, HitObjectType.CIRCLE, true, 0, null, false, false);
				}
				return true;
			}
		}

		// "relax" mod: click automatically
		else if (GameMod.RELAX.isActive() && trackPosition >= time)
			return mousePressed(mouseX, mouseY, trackPosition);

		return false;
	}

	@Override
	public Vec2f getPointAt(int trackPosition) { return new Vec2f(x, y); }

	@Override
	public int getEndTime() { return hitObject.getTime(); }

	@Override
	public void updatePosition() {
		this.x = hitObject.getScaledX();
		this.y = hitObject.getScaledY();
		super.updateStartEndPositions(hitObject.getTime());
	}

	@Override
	public void reset() {}

	@Override
	public boolean isCircle() {
		return true;
	}

	@Override
	public boolean isSlider() {
		return false;
	}

	@Override
	public boolean isSpinner() {
		return false;
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public Color getMirroredColor() {
		return mirrorColor;
	}

	@Override
	public void updateColor()
	{
		super.updateColor();
		ObjectColorOverrides.updateRainbowHue();
		color = Dancer.colorOverride.getColor(comboColorIndex);
		mirrorColor = Dancer.colorMirrorOverride.getColor(comboColorIndex);
	}

}
