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
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.state.StateBasedGame;

import java.util.Observable;
import java.util.Observer;

public class OptionsOverlay {

	private int width;
	private int height;

	private static Options.GameOption[] options = new Options.GameOption[] {
		Options.GameOption.DANCE_MOVER,
		Options.GameOption.DANCE_QUAD_BEZ_AGGRESSIVENESS,
		Options.GameOption.DANCE_QUAD_BEZ_SLIDER_AGGRESSIVENESS_FACTOR,
		Options.GameOption.DANCE_QUAD_BEZ_USE_CUBIC_ON_SLIDERS,
		Options.GameOption.DANCE_QUAD_BEZ_CUBIC_AGGRESSIVENESS_FACTOR,
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
		Options.GameOption.PIPPI_RADIUS_PERCENT,
		Options.GameOption.PIPPI_ANGLE_INC_MUL,
		Options.GameOption.PIPPI_ANGLE_INC_MUL_SLIDER,
		Options.GameOption.PIPPI_SLIDER_FOLLOW_EXPAND,
		Options.GameOption.PIPPI_PREVENT_WOBBLY_STREAMS,
		Options.GameOption.SHOW_HIT_LIGHTING,
	};

	private int textHeight;
	private Input input;
	private final ItemList list;
	private GameContainer container;
	private Options.GameOption selectedOption;
	private final SBOverlay overlay;

	public OptionsOverlay(SBOverlay overlay) {
		this.overlay = overlay;
		list = new ItemList();
	}

	public Options.GameOption[] getSavedOptionList() {
		return options;
	}

	public void init(GameContainer container, Input input, int width, int height) {
		list.init(container);
		this.input = input;
		this.width = width;
		this.height = height;
		this.container = container;
		textHeight = Fonts.SMALL.getLineHeight();
	}

	public void render(GameContainer container, StateBasedGame game, Graphics g) {
		int hoverIdx = getOptionIdxAt(input.getMouseY());
		float a = Color.black.a;
		Color.black.a = 0.8f;
		g.setColor(Color.black);
		g.fillRect(0, 0, width, height);
		Color.black.a = a;
		for (int i = 0, j = 0; i < options.length; i++) {
			if (!options[i].showCondition()) {
				continue;
			}
			drawOption(g, options[i], j++, selectedOption == null ? hoverIdx == i : selectedOption == options[i]);
		}
		if (list.isVisible()) {
			list.render(container, game, g);
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
		int i = index;
		while (i >= 0) {
			if (!options[i--].showCondition()) {
				if (++index >= options.length) {
					return -1;
				}
			}
		}
		return index;
	}

	public void update(int mouseX, int mouseY) {

	}

	public boolean mousePressed(int button, int x, int y) {
		if (list.isVisible()) {
			list.mousePressed(button, x, y);
			return true;
		}
		int idx = getOptionIdxAt(y);
		if (idx >= 0 && idx < options.length) {
			final Options.GameOption option = options[idx];
			selectedOption = option;
			Object[] listItems = option.getListItems();
			if (listItems == null) {
				option.click(container);
			} else {
				list.setItems(listItems);
				list.setClickListener(new Observer() {
					@Override
					public void update(Observable o, Object arg) {
						option.clickListItem((int) arg);
						overlay.saveOption(option);
					}
				});
				list.show();
			}
		}
		return true;
	}


	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (list.isVisible()) {
			list.mouseDragged(oldx, oldy, newx, newy);
			return;
		}

		int multiplier;
		if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON))
			multiplier = 4;
		else if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			multiplier = 1;
		else
			return;

		// get direction
		int diff = newx - oldx;
		if (diff == 0)
			return;
		diff = ((diff > 0) ? 1 : -1) * multiplier;

		// options (drag only)
		if (selectedOption != null) {
			selectedOption.drag(container, diff);
		}
	}

	public boolean mouseReleased(int button, int x, int y) {
		if (selectedOption != null) {
			overlay.saveOption(selectedOption);
		}
		selectedOption = null;
		if (list.isVisible()) {
			list.mouseReleased(button, x, y);
			return true;
		}
		return true;
	}

	public boolean mouseWheenMoved(int newValue) {
		if (list.isVisible()) {
			list.mouseWheelMoved(newValue);
			return true;
		}
		return true;
	}

	public boolean keyPressed(int key, char c) {
		if (list.isVisible()) {
			list.keyPressed(key, c);
			return true;
		}
		return false;
	}
}
