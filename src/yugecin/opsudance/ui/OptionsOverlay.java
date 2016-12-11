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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Options.GameOption;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;

@SuppressWarnings("unused")
public class OptionsOverlay {

	private Parent parent;

	private OptionTab[] tabs;
	private OptionTab hoverTab;
	private int selectedTab;
	private GameOption hoverOption;
	private GameOption selectedOption;

	private int width;
	private int height;

	private int optionWidth;
	private int optionStartX;
	private int optionStartY;
	private int optionHeight;

	private int scrollOffset;
	private final int maxScrollOffset;

	public OptionsOverlay(Parent parent, OptionTab[] tabs, int defaultSelectedTabIndex, int containerWidth, int containerHeight) {
		this.parent = parent;

		this.tabs = tabs;
		selectedTab = defaultSelectedTabIndex;

		width = containerWidth;
		height = containerHeight;

		// calculate positions
		optionWidth = width / 2;
		optionHeight = (int) ((Fonts.MEDIUM.getLineHeight()) * 1.1f);
		optionStartX = optionWidth / 2;

		// initialize tabs
		Image tabImage = GameImage.MENU_TAB.getImage();
		float tabX = width * 0.032f + (tabImage.getWidth() / 3);
		float tabY = Fonts.XLARGE.getLineHeight() + Fonts.DEFAULT.getLineHeight() + height * 0.015f - (tabImage.getHeight() / 2f);
		int tabOffset = Math.min(tabImage.getWidth(), width / tabs.length);
		int maxScrollOffset = Fonts.MEDIUM.getLineHeight() * 2 * tabs.length;
		for (int i = 0; i < tabs.length; i++) {
			maxScrollOffset += tabs[i].options.length * optionHeight;
			tabs[i].tabIndex = i;
			tabs[i].button = new MenuButton(tabImage, tabX, tabY);
			tabX += tabOffset;
			if (tabX + tabOffset > width) {
				tabX = 0;
				tabY += GameImage.MENU_TAB.getImage().getHeight() / 2f;
			}
		}
		this.maxScrollOffset = maxScrollOffset;

		// calculate other positions
		optionStartY = (int) (tabY + tabImage.getHeight() / 2 + 2); // +2 for the separator line
	}

	public void render(Graphics g, int mouseX, int mouseY) {
		// bg
		g.setColor(Colors.BLACK_ALPHA_75);
		g.fillRect(0, 0, width, height);

		// title
		renderTitle();

		// option tabs
		renderTabs(mouseX, mouseY);

		// line separator
		g.setColor(Color.white);
		g.setLineWidth(2f);
		g.drawLine(0, optionStartY - 1, width, optionStartY - 1);
		g.resetLineWidth();

		// options
		renderOptions(g);

		UI.getBackButton().draw();
		UI.draw(g);
	}

	private void renderOptions(Graphics g) {
		g.setClip(0, optionStartY, width, height - optionStartY);
		int y = -scrollOffset + optionStartY;
		for (int tabIndex = 0; tabIndex < tabs.length; tabIndex++) {
			OptionTab tab = tabs[tabIndex];
			if (y > 0) {
				Fonts.LARGE.drawString(optionStartX, y + Fonts.LARGE.getLineHeight() * 0.6f, tab.name, Color.cyan);
			}
			y += Fonts.MEDIUM.getLineHeight() * 2;
			for (int optionIndex = 0; optionIndex < tab.options.length; optionIndex++) {
				GameOption option = tab.options[optionIndex];
				if (!option.showCondition()) {
					continue;
				}
				if (y > 0) {
					renderOption(g, option, y, option == hoverOption);
				}
				y += optionHeight;
				if (y > height) {
					tabIndex = tabs.length;
					break;
				}
			}
		}
		g.clearClip();
	}

	private void renderOption(Graphics g, GameOption option, int y, boolean focus) {
		Color col = focus ? Colors.GREEN : Colors.WHITE_FADE;
		Fonts.MEDIUM.drawString(optionStartX, y, option.getName(), col);
		Fonts.MEDIUM.drawString(optionStartX + optionWidth / 2, y, option.getValueString(), col);
	}

	public void renderTabs(int mouseX, int mouseY) {
		for (int i = 0; i < tabs.length; i++) {
			OptionTab tab = tabs[i];
			boolean hovering = tab.button.contains(mouseX, mouseY);
			UI.drawTab(tab.button.getX(), tab.button.getY(), tab.name, i == selectedTab, hovering);
		}
	}

	private void renderTitle() {
		float marginX = width * 0.015f;
		float marginY = height * 0.01f;
		Fonts.XLARGE.drawString(marginX, marginY, "Options", Color.white);
		marginX += Fonts.XLARGE.getWidth("Options") * 1.2f;
		marginY += Fonts.XLARGE.getLineHeight() * 0.9f - Fonts.DEFAULT.getLineHeight();
		Fonts.DEFAULT.drawString(marginX, marginY, "Change the way opsu! behaves", Color.white);
	}

	public void update(int delta, int mouseX, int mouseY) {
		updateHoverOption(mouseX, mouseY);
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
	}

	public void mousePressed(int button, int x, int y) {
		selectedOption = hoverOption;

		if (UI.getBackButton().contains(x, y)) {
			parent.onLeave();
			return;
		}
	}

	public void mouseReleased(int button, int x, int y) {
		selectedOption = null;
	}

	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
	}

	public void mouseWheelMoved(int delta) {
		scrollOffset = Utils.clamp(scrollOffset - delta, 0, maxScrollOffset - optionStartY);
	}

	public void keyPressed(int key, char c) {
		switch (key) {
			case Input.KEY_ESCAPE:
				parent.onLeave();
				break;
		}
	}

	private void updateHoverOption(int mouseX, int mouseY) {
		hoverOption = null;
		if (mouseY < optionStartY || mouseX < optionStartX || mouseX > optionStartX + optionWidth) {
			return;
		}

		int mouseVirtualY = scrollOffset + mouseY - optionStartY;
		for (OptionTab tab : tabs) {
			mouseVirtualY -= Fonts.MEDIUM.getLineHeight() * 2;
			for (int optionIndex = 0; optionIndex < tab.options.length; optionIndex++) {
				GameOption option = tab.options[optionIndex];
				if (!option.showCondition()) {
					continue;
				}
				if (mouseVirtualY <= optionHeight) {
					if (mouseVirtualY >= 0) {
						hoverOption = option;
					}
					return;
				}
				mouseVirtualY -= optionHeight;
			}
		}
	}

	public static class OptionTab {

		public final String name;
		public final GameOption[] options;
		private MenuButton button;
		private int tabIndex;

		public OptionTab(String name, GameOption[] options) {
			this.name = name;
			this.options = options;
		}

	}

	public interface Parent {

		void onLeave();
		void onSaveOption(GameOption option);

	}

}
