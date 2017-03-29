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

import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;
import itdelatrisu.opsu.ui.Cursor;
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.DisplayContainer;

public class ReplayPlayback {

	private final DisplayContainer container;
	public final Replay replay;
	public ReplayFrame currentFrame;
	public ReplayFrame nextFrame;
	private int frameIndex;
	private Color color;
	private Cursor cursor;
	private int keydelay[];
	public static final int SQSIZE = 15;
	private boolean hr;
	private String player;

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

	public void render(int renderdelta, Graphics g, int ypos, int time) {
		while (nextFrame != null && nextFrame.getTime() < time) {
			currentFrame = nextFrame;
			processKeys();
			frameIndex++;
			if (frameIndex >= replay.frames.length) {
				nextFrame = null;
				continue;
			}
			nextFrame = replay.frames[frameIndex];
		}
		processKeys();
		g.setColor(color);
		ypos *= (SQSIZE + 5);
		for (int i = 0; i < 4; i++) {
			if (keydelay[i] > 0) {
				g.fillRect(SQSIZE * i, ypos, SQSIZE, SQSIZE);
			}
			keydelay[i] -= renderdelta;
		}
		Fonts.SMALLBOLD.drawString(SQSIZE * 5, ypos, this.player, color);
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

}
