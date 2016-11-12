/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016 yugecin
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
package yugecin.opsudance.ui;

import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;

public class OptionsOverlay {

	private int width;
	private int height;

	private static Options.GameOption[] options = new Options.GameOption[] {
		Options.GameOption.DANCE_MOVER,
		Options.GameOption.DANCE_MOVER_DIRECTION,
		Options.GameOption.DANCE_SLIDER_MOVER_TYPE,
		Options.GameOption.DANCE_SPINNER,
		Options.GameOption.DANCE_SPINNER_DELAY,
		Options.GameOption.DANCE_LAZY_SLIDERS,
		Options.GameOption.DANCE_CIRCLE_STREAMS,
		Options.GameOption.DANCE_ONLY_CIRCLE_STACKS,
		Options.GameOption.DANCE_CIRLCE_IN_SLOW_SLIDERS,
		Options.GameOption.DANCE_CIRLCE_IN_LAZY_SLIDERS,
		Options.GameOption.DANCE_MIRROR,
		Options.GameOption.DANCE_DRAW_APPROACH,
		Options.GameOption.DANCE_OBJECT_COLOR_OVERRIDE,
		Options.GameOption.DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED,
		Options.GameOption.DANCE_RGB_OBJECT_INC,
		Options.GameOption.DANCE_CURSOR_COLOR_OVERRIDE,
		Options.GameOption.DANCE_CURSOR_MIRROR_COLOR_OVERRIDE,
		Options.GameOption.DANCE_CURSOR_ONLY_COLOR_TRAIL,
		Options.GameOption.DANCE_RGB_CURSOR_INC,
		Options.GameOption.DANCE_CURSOR_TRAIL_OVERRIDE,
		Options.GameOption.DANCE_REMOVE_BG,
		Options.GameOption.DANCE_HIDE_OBJECTS,
		Options.GameOption.DANCE_HIDE_UI,
		Options.GameOption.PIPPI_ENABLE,
		Options.GameOption.PIPPI_ANGLE_INC_MUL,
		Options.GameOption.PIPPI_ANGLE_INC_MUL_SLIDER,
		Options.GameOption.PIPPI_SLIDER_FOLLOW_EXPAND,
		Options.GameOption.PIPPI_PREVENT_WOBBLY_STREAMS,
	};

	private int textHeight;
	private Input input;

	public void init(Input input, int width, int height) {
		this.input = input;
		this.width = width;
		this.height = height;
		textHeight = Fonts.SMALL.getLineHeight();
	}

	public void render(Graphics g) {
		int hoverIdx = getOptionIdxAt(input.getMouseY());
		float a = Color.black.a;
		Color.black.a = 0.8f;
		g.setColor(Color.black);
		g.fillRect(0, 0, width, height);
		Color.black.a = a;
		for (int i = 0; i < options.length; i++) {
			drawOption(g, options[i], i, hoverIdx == i);
		}
	}

	// I know... kill me
	private void drawOption(Graphics g, Options.GameOption option, int pos, boolean focus) {
		float y = pos * (textHeight + 5);
		Color color = (focus) ? Color.cyan : Color.white;

		Fonts.MEDIUM.drawString(width / 6 * 2, y, option.getName(), color);
		Fonts.MEDIUM.drawString(width / 3 * 2, y, option.getValueString(), color);
		g.setColor(Colors.WHITE_ALPHA);
		g.drawLine(0, y + textHeight + 3, width, y + textHeight + 3 + 1);
	}

	private int getOptionIdxAt(int y) {
		int index = y / (textHeight + 5);
		if (index >= options.length) {
			return -1;
		}
		return index;
	}

	public void update(int mouseX, int mouseY) {

	}

	public boolean mousePressed(int button, int x, int y) {
		return false;
	}

	public void mouseDragged(int oldx, int oldy, int newx, int newy) {

	}

}
