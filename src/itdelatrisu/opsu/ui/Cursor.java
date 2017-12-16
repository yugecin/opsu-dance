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

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.awt.Point;
import java.util.LinkedList;

import org.newdawn.slick.*;
import yugecin.opsudance.Dancer;
import yugecin.opsudance.skinning.SkinService;

import static yugecin.opsudance.options.Options.*;

/**
 * Updates and draws the cursor.
 */
public class Cursor {

	/** Last cursor coordinates. */
	private Point lastPosition;

	/** Cursor rotation angle. */
	private static float cursorAngle = 0f;

	/** The time in milliseconds when the cursor was last pressed, used for the scaling animation. */
	private long lastCursorPressTime = 0L;

	/** Whether or not the cursor was pressed in the last frame, used for the scaling animation. */
	private boolean lastCursorPressState = false;

	/** The amount the cursor scale increases, if enabled, when pressed. */
	private static final float CURSOR_SCALE_CHANGE = 0.25f;

	/** The time it takes for the cursor to scale, in milliseconds. */
	private static final float CURSOR_SCALE_TIME = 125;

	/** Stores all previous cursor locations to display a trail. */
	private LinkedList<Point> trail = new LinkedList<>();

	private boolean newStyle;

	public static Color lastObjColor = Color.white;
	public static Color lastMirroredObjColor = Color.white;
	public static Color nextObjColor = Color.white;
	public static Color nextMirroredObjColor = Color.white;
	public static Color lastCursorColor = Color.white;

	private boolean isMirrored;

	private Color filter;

	public Cursor() {
		this(false);
	}

	public Cursor(boolean isMirrored) {
		resetLocations(0, 0);
		this.isMirrored = isMirrored;
	}

	public Cursor(Color filter) {
		this(false);
		this.filter = filter;
	}

	/**
	 * Draws the cursor.
	 * @param mousePressed whether or not the mouse button is pressed
	 */
	public void draw(boolean mousePressed) {
		if (OPTION_DISABLE_CURSOR.state) {
			return;
		}

		// determine correct cursor image
		Image cursor, cursorMiddle = null, cursorTrail;
		boolean beatmapSkinned = GameImage.CURSOR.hasBeatmapSkinImage();
		boolean hasMiddle;
		if (beatmapSkinned) {
			newStyle = true;  // osu! currently treats all beatmap cursors as new-style cursors
			hasMiddle = GameImage.CURSOR_MIDDLE.hasBeatmapSkinImage();
		} else {
			newStyle = hasMiddle = OPTION_NEW_CURSOR.state;
		}
		if (beatmapSkinned || newStyle) {
			cursor = GameImage.CURSOR.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL.getImage();
		} else {
			cursor = GameImage.CURSOR.hasGameSkinImage() ? GameImage.CURSOR.getImage() : GameImage.CURSOR_OLD.getImage();
			cursorTrail = GameImage.CURSOR_TRAIL.hasGameSkinImage() ? GameImage.CURSOR_TRAIL.getImage() : GameImage.CURSOR_TRAIL_OLD.getImage();
		}
		if (hasMiddle)
			cursorMiddle = GameImage.CURSOR_MIDDLE.getImage();

		// scale cursor
		float cursorScaleAnimated = 1f;
		if (SkinService.skin.isCursorExpanded()) {
			if (lastCursorPressState != mousePressed) {
				lastCursorPressState = mousePressed;
				lastCursorPressTime = System.currentTimeMillis();
			}

			float cursorScaleChange = CURSOR_SCALE_CHANGE * AnimationEquation.IN_OUT_CUBIC.calc(
					Utils.clamp(System.currentTimeMillis() - lastCursorPressTime, 0, CURSOR_SCALE_TIME) / CURSOR_SCALE_TIME);
			cursorScaleAnimated = 1f + ((mousePressed) ? cursorScaleChange : CURSOR_SCALE_CHANGE - cursorScaleChange);
		}
		float cursorScale = cursorScaleAnimated * OPTION_CURSOR_SIZE.val / 100f;
		if (cursorScale != 1f) {
			cursor = cursor.getScaledCopy(cursorScale);
			cursorTrail = cursorTrail.getScaledCopy(cursorScale);
		}

		Color filter;
		if (isMirrored) {
			filter = Dancer.cursorColorMirrorOverride.getMirrorColor();
		} else {
			lastCursorColor = filter = Dancer.cursorColorOverride.getColor();
		}

		if (this.filter != null) {
			filter = this.filter;
		}

		// draw a fading trail
		float alpha = 0f;
		float t = 2f / trail.size();
		int cursorTrailWidth = cursorTrail.getWidth(), cursorTrailHeight = cursorTrail.getHeight();
		float cursorTrailRotation = (SkinService.skin.isCursorTrailRotated()) ? cursorAngle : 0;
		cursorTrail.startUse();
		for (Point p : trail) {
			alpha += t;
			cursorTrail.setImageColor(filter.r, filter.g, filter.b, alpha * 0.25f);
			cursorTrail.drawEmbedded(
					p.x - (cursorTrailWidth / 2f), p.y - (cursorTrailHeight / 2f),
					cursorTrailWidth, cursorTrailHeight, cursorTrailRotation);
		}
		cursorTrail.drawEmbedded(
				lastPosition.x - (cursorTrailWidth / 2f), lastPosition.y - (cursorTrailHeight / 2f),
				cursorTrailWidth, cursorTrailHeight, cursorTrailRotation);
		cursorTrail.endUse();

		// draw the other components
		if (newStyle && SkinService.skin.isCursorRotated()) {
			cursor.setRotation(cursorAngle);
		}
		cursor.drawCentered(lastPosition.x, lastPosition.y, OPTION_DANCE_CURSOR_ONLY_COLOR_TRAIL.state ? Color.white : filter);
		if (hasMiddle) {
			cursorMiddle.drawCentered(lastPosition.x, lastPosition.y, OPTION_DANCE_CURSOR_ONLY_COLOR_TRAIL.state ? Color.white : filter);
		}
	}

	/**
	 * Sets the cursor position to given point and updates trail.
	 * @param mouseX x coordinate to set position to
	 * @param mouseY y coordinate to set position to
	 */
	public void setCursorPosition(int delta, int mouseX, int mouseY) {
		// TODO: use an image buffer
		int removeCount = 0;
		float FPSmod = Math.max(1000 / Math.max(delta, 1), 1) / 30f; // TODO
		if (newStyle) {
			// new style: add all points between cursor movements
			if ((lastPosition.x == 0 && lastPosition.y == 0) || !addCursorPoints(lastPosition.x, lastPosition.y, mouseX, mouseY)) {
				trail.add(new Point(mouseX, mouseY));
			}
			lastPosition.move(mouseX, mouseY);

			removeCount = (int) (trail.size() / (6 * FPSmod)) + 1;
		} else {
			// old style: sample one point at a time
			trail.add(new Point(mouseX, mouseY));

			int max = (int) (10 * FPSmod);
			if (trail.size() > max)
				removeCount = trail.size() - max;
		}

		int cursortraillength = OPTION_DANCE_CURSOR_TRAIL_OVERRIDE.val;
		if (cursortraillength > 20) {
			removeCount = trail.size() - cursortraillength;
		}

		// remove points from the lists
		for (int i = 0; i < removeCount && !trail.isEmpty(); i++)
			trail.remove();
	}

	/**
	 * Adds all points between (x1, y1) and (x2, y2) to the cursor point lists.
	 * @author http://rosettacode.org/wiki/Bitmap/Bresenham's_line_algorithm#Java
	 */
	private boolean addCursorPoints(int x1, int y1, int x2, int y2) {
		// delta of exact value and rounded value of the dependent variable
		boolean added = false;
		int d = 0;
		int dy = Math.abs(y2 - y1);
		int dx = Math.abs(x2 - x1);

		int dy2 = (dy << 1);  // slope scaling factors to avoid floating
		int dx2 = (dx << 1);  // point
		int ix = x1 < x2 ? 1 : -1;  // increment direction
		int iy = y1 < y2 ? 1 : -1;

		int k = 5;  // sample size
		if (dy <= dx) {
			for (int i = 0; ; i++) {
				if (i == k) {
					trail.add(new Point(x1, y1));
					added = true;
					i = 0;
				}
				if (x1 == x2)
					break;
				x1 += ix;
				d += dy2;
				if (d > dx) {
					y1 += iy;
					d -= dx2;
				}
			}
		} else {
			for (int i = 0; ; i++) {
				if (i == k) {
					trail.add(new Point(x1, y1));
					added = true;
					i = 0;
				}
				if (y1 == y2)
					break;
				y1 += iy;
				d += dx2;
				if (d > dy) {
					x1 += ix;
					d -= dy2;
				}
			}
		}
		return added;
	}

	/**
	 * Rotates the cursor by a degree determined by a delta interval.
	 * If the old style cursor is being used, this will do nothing.
	 * @param delta the delta interval since the last call
	 */
	public void updateAngle(int delta) {
		cursorAngle += delta / 40f;
		cursorAngle %= 360;
	}

	/**
	 * Resets all cursor data and beatmap skins.
	 */
	public void reset(int mouseX, int mouseY) {
		// destroy skin images
		GameImage.CURSOR.destroyBeatmapSkinImage();
		GameImage.CURSOR_MIDDLE.destroyBeatmapSkinImage();
		GameImage.CURSOR_TRAIL.destroyBeatmapSkinImage();

		// reset locations
		resetLocations(mouseX, mouseY);

		// reset angles
		cursorAngle = 0f;
	}

	/**
	 * Resets all cursor location data.
	 */
	public void resetLocations(int mouseX, int mouseY) {
		trail.clear();
		lastPosition = new Point(mouseX, mouseY);
		for (int i = 0; i < 50; i++) {
			trail.add(new Point(lastPosition));
		}
	}

	/**
	 * Returns whether or not the cursor is skinned.
	 */
	public boolean isBeatmapSkinned() {
		return (GameImage.CURSOR.hasBeatmapSkinImage() ||
		        GameImage.CURSOR_MIDDLE.hasBeatmapSkinImage() ||
		        GameImage.CURSOR_TRAIL.hasBeatmapSkinImage());
	}

}
