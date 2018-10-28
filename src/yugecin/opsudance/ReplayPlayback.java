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
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import yugecin.opsudance.core.Entrypoint;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;

import static itdelatrisu.opsu.GameData.*;
import static itdelatrisu.opsu.Utils.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

public class ReplayPlayback
{
	private final HitData hitdata;
	public final Replay replay;
	public ReplayFrame currentFrame;
	public ReplayFrame nextFrame;
	private int frameIndex;
	private Color color;
	private final Color originalcolor;
	public final ReplayCursor cursor;
	private int keydelay[];
	public final int PADDING = 3;
	public final int sqsize;
	public final int unitHeight;
	public static int lineHeight;
	private boolean hr;
	private String player;
	private String mods;
	private int playerwidth;
	private int modwidth;
	private String currentAcc;
	private int currentAccWidth;
	private final int ACCMAXWIDTH;

	private int c300, c100, c50, fakecmiss;

	private Image hitImage;
	private int hitImageTimer = 0;
	private boolean knockedout;
	private final LinkedList<MissIndicator> missIndicators;

	private Image gradeImage;

	private static final Color missedColor = new Color(0.4f, 0.4f, 0.4f, 1f);

	public ReplayPlayback(Replay replay, HitData hitdata, Color color, ReplayCursor cursor) {
		this.missIndicators = new LinkedList<>();
		this.replay = replay;
		this.hitdata = hitdata;
		resetFrameIndex();
		this.color = new Color(color);
		this.originalcolor = color;
		this.cursor = cursor;
		keydelay = new int[4];
		this.player = replay.playerName;
		this.playerwidth = Fonts.SMALLBOLD.getWidth(this.player);
		this.mods = "";
		this.currentAcc = "100,00%";
		this.currentAccWidth = Fonts.SMALLBOLD.getWidth(currentAcc);
		this.ACCMAXWIDTH = currentAccWidth + 10;
		this.unitHeight = (int) (Fonts.SMALLBOLD.getLineHeight() * 0.9f);
		this.sqsize = unitHeight - PADDING;
		lineHeight = this.unitHeight;
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
		if (knockedout || !OPTION_RP_SHOW_GRADES.state) {
			gradeImage = null;
			return;
		}

		boolean silver = (replay.mods & 0x408) > 0 && (replay.mods & 0x200) == 0;
		GameData.Grade grade = GameData.getGrade(c300, c100, c50, fakecmiss, silver);

		if (grade == GameData.Grade.NULL) {
			if ((replay.mods & 0x8) > 0 && (replay.mods & 0x200) == 0) {
				grade = GameData.Grade.SSH;
			} else {
				grade = GameData.Grade.SS;
			}
		}
		gradeImage = grade.getSmallImage().getScaledCopy(unitHeight, unitHeight);
	}

	private int HITIMAGETIMEREXPAND = 250;
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
		if (!knockedout && hitImageTimer > HITIMAGETIMERFADEEND) {
			hitImage = null;
			return;
		}

		Color color = new Color(1f, 1f, 1f, 1f);
		if (!knockedout && hitImageTimer > HITIMAGETIMERFADESTART) {
			color.a = (HITIMAGETIMERFADEEND - hitImageTimer) / HITIMAGETIMERFADEDELTA;
		}
		if (knockedout) {
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
			offset = unitHeight / 2f * (1f - scale);
		}
		hitImage.draw(xpos, 2f + ypos + offset, scale, color);
	}

	public float getHeight() {
		if (hitImageTimer < HITIMAGEDEADFADE) {
			return unitHeight;
		}
		if (hitImageTimer >= HITIMAGEDEADFADE + SHRINKTIME) {
			return 0f;
		}
		return unitHeight * (1f - AnimationEquation.OUT_QUART.calc((hitImageTimer - HITIMAGEDEADFADE) / SHRINKTIME));
	}

	public void render(int renderdelta, Graphics g, float ypos, int time)
	{
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
		if (!knockedout) {
			for (int i = 0; i < 4; i++) {
				if (keydelay[i] > 0) {
					g.fillRect(sqsize * i, ypos + PADDING, sqsize, sqsize);
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
				hitImage = GameData.hitResults[GameData.HIT_100];
				c100++;
				hitschanged = true;
			}

			while (!hitdata.time50.isEmpty() && hitdata.time50.getFirst() <= time) {
				hitdata.time50.removeFirst();
				hitImageTimer = 0;
				hitImage = GameData.hitResults[GameData.HIT_50];
				c50++;
				hitschanged = true;
			}

			while (!hitdata.timeCombobreaks.isEmpty() && hitdata.timeCombobreaks.getFirst() <= time) {
				hitdata.timeCombobreaks.removeFirst();
				hitImageTimer = 0;
				hitImage = GameData.hitResults[GameData.HIT_MISS];
				fakecmiss++;
				hitschanged = true;
				if (OPTION_RP_SHOW_MISSES.state) {
					float posx = currentFrame.getScaledX();
					float posy = currentFrame.getScaledY();
					if (hr) {
						posy = height - posy;
					}
					this.missIndicators.add(new MissIndicator(posx, posy));
				}
				if (OPTION_RP_KNOCKOUT.state) {
					knockedout = true;
					color = new Color(missedColor);
				}
			}

			if (hitImage != null) {
				final float h = hitImage.getHeight();
				if (h == 0) {
					hitImage = null;
				} else {
					hitImage = hitImage.getScaledCopy(unitHeight / h);
				}
			}

			if (hitschanged) {
				updateGradeImage();
			}
		}
		int xpos = sqsize * (OPTION_RP_SHOW_MOUSECOLUMN.state ? 5 : 3);
		if (OPTION_RP_SHOW_ACC.state) { 
			Fonts.SMALLBOLD.drawString(xpos + ACCMAXWIDTH - currentAccWidth - 10, ypos, currentAcc, new Color(.4f, .4f, .4f, color.a));
			xpos += ACCMAXWIDTH;
		}
		if (gradeImage != null) {
			gradeImage.draw(xpos, ypos);
			xpos += sqsize + 10;
		}
		Fonts.SMALLBOLD.drawString(xpos, ypos, this.player, color);
		xpos += playerwidth;
		if (!this.mods.isEmpty()) {
			Fonts.SMALLBOLD.drawString(xpos, ypos, this.mods, new Color(1f, 1f, 1f, color.a));
			xpos += modwidth;
		}
		xpos += 10;
		if (OPTION_RP_SHOW_HITS.state) {
			showHitImage(renderdelta, xpos, ypos);
		}
		if (OPTION_RP_SHOW_MISSES.state) { 
			final Iterator<MissIndicator> iter = this.missIndicators.iterator();
			while (iter.hasNext()) {
				final MissIndicator mi = iter.next();
				if (mi.timer >= HITIMAGEDEADFADE) {
					iter.remove();
					continue;
				}
				float progress = (float) mi.timer / HITIMAGEDEADFADE;
				float failposy = mi.posy + 50f * OUT_QUART.calc(progress);
				Color col = new Color(originalcolor);
				col.a = 1f - IN_QUAD.calc(clamp(progress * 2f, 0f, 1f));
				Fonts.SMALLBOLD.drawString(mi.posx - playerwidth / 2, failposy, player, col);
				Color failimgcol = new Color(1f, 1f, 1f, col.a);
				Image failimg = hitResults[HIT_MISS].getScaledCopy(unitHeight, unitHeight);
				failimg.draw(mi.posx + playerwidth / 2 + 5, failposy + 2f, failimgcol);
				mi.timer += renderdelta;
			}
		}
		if (knockedout) {
			return;
		}
		int y = currentFrame.getScaledY();
		if (hr) {
			y = height - y;
		}
		cursor.setCursorPosition(renderdelta, currentFrame.getScaledX(), y);
	}

	public boolean shouldDrawCursor()
	{
		return !knockedout;
	}

	private void processKeys() {
		int keys = currentFrame.getKeys();
		if ((keys & 5) == 5) {
			keydelay[0] = OPTION_RP_KEYPRESS_DELAY.val;
		}
		if ((keys & 10) == 10) {
			keydelay[1] = OPTION_RP_KEYPRESS_DELAY.val;
		}
		if ((keys ^ 5) == 4) {
			keydelay[2] = OPTION_RP_KEYPRESS_DELAY.val;
		}
		if ((keys ^ 10) == 8) {
			keydelay[3] = OPTION_RP_KEYPRESS_DELAY.val;
		}
	}
	
	private static class MissIndicator
	{
		private float posx, posy;
		private int timer;
		
		private MissIndicator(float posx, float posy)
		{
			this.posx = posx;
			this.posy = posy;
			this.timer = 0;
		}
	}

	public static class HitData
	{
		LinkedList<Integer> time300 = new LinkedList<>();
		LinkedList<Integer> time100 = new LinkedList<>();
		LinkedList<Integer> time50 = new LinkedList<>();
		LinkedList<Integer> timeCombobreaks = new LinkedList<>();
		LinkedList<AccData> acc = new LinkedList<>();
		LinkedList<ComboData> combo = new LinkedList<>();

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
					if (rd <= 0) {
						break;
					}
					if (rd != 4) {
						throw new RuntimeException("expected 4 bytes, got " + rd);
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
							timeCombobreaks.add(lasttime);
						}
						lastcombo = c;
						break;
					default:
						throw new RuntimeException("unexpected data");
					}
				}
				if (lasttime == -1) {
					throw new RuntimeException("nodata");
				}
				Entrypoint.sout(String.format(
					"%s lastcombo %d lasttime %d",
					file.getName(),
					lastcombo,
					lasttime
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
