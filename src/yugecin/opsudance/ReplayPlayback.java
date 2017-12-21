/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance;

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;
import itdelatrisu.opsu.ui.Cursor;
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import yugecin.opsudance.core.DisplayContainer;

public class ReplayPlayback {

	private final DisplayContainer container;
	public final Replay replay;
	public ReplayFrame currentFrame;
	public ReplayFrame nextFrame;
	private int frameIndex;
	public Color color;
	public Cursor cursor;
	private int keydelay[];
	public static final int SQSIZE = 15;
	private boolean hr;
	private String player;

	public GameObject[] gameObjects;
	private int objectIndex = 0;
	private int lastkeys = 0;
	private Image hitImage;
	private int hitImageTimer = 0;
	public GData gdata = new GData();

	public ReplayPlayback(DisplayContainer container, Replay replay, Color color) {
		this.container = container;
		this.replay = replay;
		resetFrameIndex();
		this.color = color;
		Color cursorcolor = new Color(color);
		//cursorcolor.a = 0.5f;
		cursor = new Cursor(cursorcolor);
		keydelay = new int[4];
		this.player = "";
		if ((replay.mods & 0x1) > 0) {
			this.player += "NF";
		}
		if ((replay.mods & 0x2) > 0) {
			this.player += "EZ";
		}
		if ((replay.mods & 0x8) > 0 && (replay.mods & 0x200) == 0) {
			this.player += "HD";
		}
		if ((replay.mods & 0x10) > 0) {
			this.player += "HR";
			hr = true;
		}
		if ((replay.mods & 0x20) > 0) {
			this.player += "SD";
		}
		if ((replay.mods & 0x40) > 0) {
			this.player += "DT";
		}
		if ((replay.mods & 0x80) > 0) {
			this.player += "RL";
		}
		if ((replay.mods & 0x100) > 0) {
			this.player += "HT";
		}
		if ((replay.mods & 0x200) > 0) {
			this.player += "NC";
		}
		if ((replay.mods & 0x400) > 0) {
			this.player += "FL";
		}
		if ((replay.mods & 0x4000) > 0) {
			this.player += "PF";
		}
		if (this.player.length() > 0) {
			this.player = " +" + this.player;
		}
		this.player = replay.playerName + this.player;
	}

	public void resetFrameIndex() {
		frameIndex = 0;
		currentFrame = replay.frames[frameIndex++];
		nextFrame = replay.frames[frameIndex];
	}

	private void sendKeys(Beatmap beatmap, int trackPosition) {
		if (objectIndex >= gameObjects.length)  // nothing to do here
			return;

		HitObject hitObject = beatmap.objects[objectIndex];

		// circles
		if (hitObject.isCircle() && gameObjects[objectIndex].mousePressed(currentFrame.getScaledX(), currentFrame.getScaledY(), trackPosition))
			objectIndex++;  // circle hit

			// sliders
		else if (hitObject.isSlider())
			gameObjects[objectIndex].mousePressed(currentFrame.getScaledX(), currentFrame.getScaledY(), trackPosition);
	}

	private void update(int trackPosition, Beatmap beatmap, int[] hitResultOffset, int delta) {
		boolean keyPressed = currentFrame.getKeys() != ReplayFrame.KEY_NONE;
		while (objectIndex < gameObjects.length && trackPosition > beatmap.objects[objectIndex].getTime()) {
			// check if we've already passed the next object's start time
			boolean overlap = (objectIndex + 1 < gameObjects.length &&
				trackPosition > beatmap.objects[objectIndex + 1].getTime() - hitResultOffset[GameData.HIT_50]);

			// update hit object and check completion status
			if (gameObjects[objectIndex].update(overlap, delta, currentFrame.getScaledX(), currentFrame.getScaledY(), keyPressed, trackPosition)) {
				objectIndex++;  // done, so increment object index
			} else
				break;
		}
	}

	public void render(Beatmap beatmap, int[] hitResultOffset, int renderdelta, Graphics g, int ypos, int time) {
		if (objectIndex >= gameObjects.length) {
			return;
		}

		while (nextFrame != null && nextFrame.getTime() < time) {
			currentFrame = nextFrame;

			int keys = currentFrame.getKeys();
			int deltaKeys = (keys & ~lastkeys);  // keys that turned on
			if (deltaKeys != ReplayFrame.KEY_NONE) { // send a key press
				sendKeys(beatmap, currentFrame.getTime());
			} else if (keys == lastkeys) {
				update(time, beatmap, hitResultOffset, currentFrame.getTimeDiff());
			}
			lastkeys = keys;

			processKeys();
			frameIndex++;
			if (frameIndex >= replay.frames.length) {
				nextFrame = null;
				continue;
			}
			nextFrame = replay.frames[frameIndex];
		}
		g.setColor(color);
		ypos *= (SQSIZE + 5);
		for (int i = 0; i < 4; i++) {
			if (keydelay[i] > 0) {
				g.fillRect(SQSIZE * i, ypos + 5, SQSIZE, SQSIZE);
			}
			keydelay[i] -= renderdelta;
		}
		Fonts.SMALLBOLD.drawString(SQSIZE * 5, ypos, this.player + " " + this.objectIndex, color);
		int namewidth = Fonts.SMALLBOLD.getWidth(this.player);
		if (hitImage != null) {
			hitImage.draw(SQSIZE * 5 + namewidth + SQSIZE * 2, ypos);
		}
		int y = currentFrame.getScaledY();
		if (hr) {
			y = container.height - y;
		}
		cursor.setCursorPosition(renderdelta, currentFrame.getScaledX(), y);
		cursor.draw(false);
	}

	private void processKeys() {
		int keys = currentFrame.getKeys();
		int KEY_DELAY = 10;
		if ((keys & 5) == 5) {
			keydelay[0] = KEY_DELAY;
		}
		if ((keys & 10) == 10) {
			keydelay[1] = KEY_DELAY;
		}
		if ((keys ^ 5) == 4) {
			keydelay[2] = KEY_DELAY;
		}
		if ((keys ^ 10) == 8) {
			keydelay[3] = KEY_DELAY;
		}
	}

	public class GData extends GameData {

		public GData() {
			super();
			this.loadImages();
		}

		@Override
		public void sendSliderRepeatResult(int time, float x, float y, Color color, Curve curve, HitObjectType type) {
			// ?
		}

		@Override
		public void sendSliderStartResult(int time, float x, float y, Color color, Color mirrorColor, boolean expand) {
			// ?
		}

		@Override
		public void sendSliderTickResult(int time, int result, float x, float y, HitObject hitObject, int repeat) {
			if (result == HIT_MISS) {

			}
		}

		@Override
		public void sendHitResult(int time, int result, float x, float y, Color color, boolean end, HitObject hitObject, HitObjectType hitResultType, boolean expand, int repeat, Curve curve, boolean sliderHeldToEnd) {
			sendHitResult(time, result, x, y, color, end, hitObject, hitResultType, expand, repeat, curve, sliderHeldToEnd, true);
		}

		@Override
		public void sendHitResult(int time, int result, float x, float y, Color color, boolean end, HitObject hitObject, HitObjectType hitResultType, boolean expand, int repeat, Curve curve, boolean sliderHeldToEnd, boolean handleResult) {
			if ((result == HIT_300)) {
				//return;
			}

			if (result < hitResults.length) {
				hitImageTimer = 0;
				hitImage = hitResults[result].getScaledCopy(SQSIZE, SQSIZE);
			}
		}

		@Override
		public void addHitError(int time, int x, int y, int timeDiff) {
			//?
		}
	}

}
