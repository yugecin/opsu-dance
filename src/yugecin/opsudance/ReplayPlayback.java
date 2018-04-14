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
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;
import itdelatrisu.opsu.ui.Cursor;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.Entrypoint;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;

public class ReplayPlayback {

	private static final boolean HIDEMOUSEBTNS = true;

	private final DisplayContainer container;
	private final HitData hitdata;
	public final Replay replay;
	public ReplayFrame currentFrame;
	public ReplayFrame nextFrame;
	private int frameIndex;
	public Color color;
	public Cursor cursor;
	private int keydelay[];
	public static final int SQSIZE = 15;
	public static final int UNITHEIGHT = SQSIZE + 5;
	private boolean hr;
	private String player;
	private String mods;
	private int playerwidth;
	private int modwidth;
	private String currentAcc;
	private int currentAccWidth;
	private final int ACCMAXWIDTH;

	private int c300, c100, c50;

	private Image hitImage;
	private int hitImageTimer = 0;
	private boolean missed;

	private Image gradeImage;

	private static final Color missedColor = new Color(0.4f, 0.4f, 0.4f, 1f);

	public ReplayPlayback(DisplayContainer container, Replay replay, HitData hitdata, Color color) {
		this.container = container;
		this.replay = replay;
		this.hitdata = hitdata;
		resetFrameIndex();
		this.color = color;
		Color cursorcolor = new Color(color);
		//cursorcolor.a = 0.5f;
		cursor = new Cursor(cursorcolor);
		keydelay = new int[4];
		this.player = replay.playerName;
		this.playerwidth = Fonts.SMALLBOLD.getWidth(this.player);
		this.mods = "";
		this.currentAcc = "100,00%";
		this.currentAccWidth = Fonts.SMALLBOLD.getWidth(currentAcc);
		this.ACCMAXWIDTH = currentAccWidth + 10;
		if ((replay.mods & 0x1) > 0) {
			this.mods += "NF";
		}
		if ((replay.mods & 0x2) > 0) {
			this.mods += "EZ";
		}
		if ((replay.mods & 0x8) > 0 && (replay.mods & 0x200) == 0) {
			this.mods += "HD";
		}
		if ((replay.mods & 0x10) > 0) {
			this.mods += "HR";
			hr = true;
		}
		if ((replay.mods & 0x20) > 0) {
			this.mods += "SD";
		}
		if ((replay.mods & 0x40) > 0) {
			this.mods += "DT";
		}
		if ((replay.mods & 0x80) > 0) {
			this.mods += "RL";
		}
		if ((replay.mods & 0x100) > 0) {
			this.mods += "HT";
		}
		if ((replay.mods & 0x200) > 0) {
			this.mods += "NC";
		}
		if ((replay.mods & 0x400) > 0) {
			this.mods += "FL";
		}
		if ((replay.mods & 0x4000) > 0) {
			this.mods += "PF";
		}
		if (this.mods.length() > 0) {
			this.mods = " +" + this.mods;
			this.modwidth = Fonts.SMALLBOLD.getWidth(this.mods);
		}
		updateGradeImage();
	}

	public void resetFrameIndex() {
		frameIndex = 0;
		currentFrame = replay.frames[frameIndex++];
		nextFrame = replay.frames[frameIndex];
	}

	private void updateGradeImage() {
		if (missed) {
			gradeImage = null;
			return;
		}

		boolean silver = (replay.mods & 0x408) > 0 && (replay.mods & 0x200) == 0;
		GameData.Grade grade = GameData.getGrade(c300, c100, c50, 0, silver);

		if (grade == GameData.Grade.NULL) {
			gradeImage = null;
			return;
		}
		gradeImage = grade.getSmallImage().getScaledCopy(SQSIZE + 5, SQSIZE + 5);
	}

	private int HITIMAGETIMEREXPAND = 200;
	private int HITIMAGETIMERFADESTART = 500;
	private int HITIMAGETIMERFADEEND = 700;
	private float HITIMAGETIMERFADEDELTA = HITIMAGETIMERFADEEND - HITIMAGETIMERFADESTART;
	private int HITIMAGEDEADFADE = 4000;
	private float SHRINKTIME = 500f;
	private void showHitImage(int renderdelta, int xpos, float ypos) {
		if (hitImage == null) {
			return;
		}

		hitImageTimer += renderdelta;
		if (!missed && hitImageTimer > HITIMAGETIMERFADEEND) {
			hitImage = null;
			return;
		}

		Color color = new Color(1f, 1f, 1f, 1f);
		if (!missed && hitImageTimer > HITIMAGETIMERFADESTART) {
			color.a = (HITIMAGETIMERFADEEND - hitImageTimer) / HITIMAGETIMERFADEDELTA;
		}
		if (missed) {
			if (hitImageTimer > HITIMAGEDEADFADE) {
				this.color.a = color.a = 0f;
			} else {
				this.color.a = color.a = 1f - AnimationEquation.IN_CIRC.calc((float) hitImageTimer / HITIMAGEDEADFADE);
			}
		}
		float scale = 1f;
		float offset = 0f;
		if (hitImageTimer < HITIMAGETIMEREXPAND) {
			scale = AnimationEquation.OUT_EXPO.calc((float) hitImageTimer / HITIMAGETIMEREXPAND);
			offset = UNITHEIGHT / 2f * (1f - scale);
		}
		hitImage.draw(xpos + offset, 2f + ypos + offset, scale, color);
	}

	public float getHeight() {
		if (hitImageTimer < HITIMAGEDEADFADE) {
			return UNITHEIGHT;
		}
		if (hitImageTimer >= HITIMAGEDEADFADE + SHRINKTIME) {
			return 0f;
		}
		return UNITHEIGHT * (1f - AnimationEquation.OUT_QUART.calc((hitImageTimer - HITIMAGEDEADFADE) / SHRINKTIME));
	}

	public void render(int renderdelta, Graphics g, float ypos, int time) {

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
		if (!missed) {
			for (int i = 0; i < 4; i++) {
				if (keydelay[i] > 0) {
					g.fillRect(SQSIZE * i, ypos + 5, SQSIZE, SQSIZE);
				}
				keydelay[i] -= renderdelta;
			}

			boolean hitschanged = false;
			while (!hitdata.acc.isEmpty() && hitdata.acc.getFirst().time <= time) {
				currentAcc = String.format("%.2f%%", hitdata.acc.removeFirst().acc).replace('.', ',');
				currentAccWidth = Fonts.SMALLBOLD.getWidth(currentAcc);
			}

			while (!hitdata.time300.isEmpty() && hitdata.time300.getFirst() <= time) {
				hitdata.time300.removeFirst();
				c300++;
				hitschanged = true;
			}

			while (!hitdata.time100.isEmpty() && hitdata.time100.getFirst() <= time) {
				hitdata.time100.removeFirst();
				hitImageTimer = 0;
				hitImage = GameData.hitResults[GameData.HIT_100].getScaledCopy(SQSIZE + 5, SQSIZE + 5);
				c100++;
				hitschanged = true;
			}

			while (!hitdata.time50.isEmpty() && hitdata.time50.getFirst() <= time) {
				hitdata.time50.removeFirst();
				hitImageTimer = 0;
				hitImage = GameData.hitResults[GameData.HIT_100].getScaledCopy(SQSIZE + 5, SQSIZE + 5);
				c50++;
				hitschanged = true;
			}

			if (hitschanged) {
				updateGradeImage();
			}

			if (time >= hitdata.combobreaktime) {
				missed = true;
				color = new Color(missedColor);
				hitImageTimer = 0;
				hitImage = GameData.hitResults[GameData.HIT_MISS].getScaledCopy(SQSIZE + 5, SQSIZE + 5);
			}
		}
		int xpos = SQSIZE * (HIDEMOUSEBTNS ? 3 : 5);
		Fonts.SMALLBOLD.drawString(xpos + ACCMAXWIDTH - currentAccWidth - 10, ypos, currentAcc, new Color(.4f, .4f, .4f, color.a));
		xpos += ACCMAXWIDTH;
		if (!missed && gradeImage != null) {
			gradeImage.draw(xpos, ypos);
		}
		xpos += SQSIZE + 10;
		Fonts.SMALLBOLD.drawString(xpos, ypos, this.player, color);
		xpos += playerwidth;
		if (!this.mods.isEmpty()) {
			Fonts.SMALLBOLD.drawString(xpos, ypos, this.mods, new Color(1f, 1f, 1f, color.a));
			xpos += modwidth;
		}
		xpos += 10;
		showHitImage(renderdelta, xpos, ypos);
		if (missed) {
			return;
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

	public static class HitData {

		int combobreaktime = -1;
		LinkedList<Integer> time300 = new LinkedList();
		LinkedList<Integer> time100 = new LinkedList();
		LinkedList<Integer> time50 = new LinkedList();
		LinkedList<AccData> acc = new LinkedList();
		LinkedList<ComboData> combo = new LinkedList();

		public HitData(File file) {
			try (InputStream in = new FileInputStream(file)) {
				int lasttime = -1;
				int lastcombo = 0;
				int last300 = 0;
				int last100 = 0;
				int last50 = 0;
				while (true) {
					byte[] time = new byte[4];
					int rd = in.read(time);
					if (rd == 0) {
						break;
					}
					if (rd != 4) {
						throw new RuntimeException();
					}
					byte[] _time = { time[3], time[2], time[1], time[0] };
					lasttime = ByteBuffer.wrap(_time).getInt();
					lasttime += 200;
					int type = in.read();
					if (type == -1) {
						throw new RuntimeException();
					}
					if (in.read(time) != 4) {
						throw new RuntimeException();
					}
					_time = new byte[] { time[3], time[2], time[1], time[0] };
					switch (type) {
					case 1:
						int this100 = ByteBuffer.wrap(_time).getInt();
						spread(time100, lasttime, this100 - last100);
						last100 = this100;
						break;
					case 3:
						int this300 = ByteBuffer.wrap(_time).getInt();
						spread(time300, lasttime, this300 - last300);
						last300 = this300;
						break;
					case 5:
						int this50 = ByteBuffer.wrap(_time).getInt();
						spread(time50, lasttime, this50 - last50);
						last50 = this50;
						break;
					case 10:
						acc.add(new AccData(lasttime, ByteBuffer.wrap(_time).getFloat()));
						break;
					case 12:
						int c = ByteBuffer.wrap(_time).getInt();
						combo.add(new ComboData(lasttime, c));
						if (c < lastcombo) {
							combobreaktime = lasttime;
						} else {
							lastcombo = c;
						}
						break;
					default:
						throw new RuntimeException();
					}
					if (combobreaktime != -1) {
						break;
					}
				}
				if (combobreaktime == -1) {
					combobreaktime = lasttime;
				}
				if (combobreaktime == -1) {
					throw new RuntimeException("nodata");
				}
				Entrypoint.sout(String.format(
					"%s combobreak at %d, lastcombo %d lastacc %f",
					file.getName(),
					combobreaktime,
					lastcombo,
					acc.getLast().acc
				));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void spread(LinkedList<Integer> list, int time, int d) {
			if (list.isEmpty() || d <= 1) {
				list.add(time);
				return;
			}

			int dtime = time - list.getLast();
			int inc = dtime / d;
			int ttime = list.getLast();
			for (int i = 0; i < d; i++) {
				ttime += inc;
				if (i == d - 1) {
					ttime = time;
				}
				list.add(ttime);
			}
		}

	}

	public static class AccData {
		public int time;
		public float acc;
		public AccData(int time, float acc) {
			this.time = time;
			this.acc = acc;
		}
	}

	public static class ComboData {
		public int time;
		public int combo;
		public ComboData(int time, int combo) {
			this.time = time;
			this.combo = combo;
		}
	}

}
