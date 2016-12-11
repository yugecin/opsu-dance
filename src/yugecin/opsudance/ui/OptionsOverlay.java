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
import itdelatrisu.opsu.Options.GameOption.OptionType;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import org.newdawn.slick.*;

@SuppressWarnings("unused")
public class OptionsOverlay {

	private Parent parent;
	private GameContainer container;

	private final Image sliderBallImg;
	private final Image checkOnImg;
	private final Image checkOffImg;

	private OptionTab[] tabs;
	private OptionTab hoverTab;
	private int selectedTab;
	private GameOption hoverOption;
	private GameOption selectedOption;
	private int sliderOptionStartX;
	private int sliderOptionLength;
	private boolean isAdjustingSlider;
	private boolean isListOptionOpen;

	private int width;
	private int height;

	private int optionWidth;
	private int optionStartX;
	private int optionStartY;
	private int optionHeight;

	private int scrollOffset;
	private final int maxScrollOffset;

	private int mousePressY;

	public OptionsOverlay(Parent parent, OptionTab[] tabs, int defaultSelectedTabIndex, GameContainer container) {
		this.parent = parent;
		this.container = container;

		this.tabs = tabs;
		selectedTab = defaultSelectedTabIndex;

		sliderBallImg = GameImage.CONTROL_SLIDER_BALL.getImage().getScaledCopy(20, 20);
		checkOnImg = GameImage.CONTROL_CHECK_ON.getImage().getScaledCopy(20, 20);
		checkOffImg = GameImage.CONTROL_CHECK_OFF.getImage().getScaledCopy(20, 20);

		width = container.getWidth();
		height = container.getHeight();

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
		this.maxScrollOffset = maxScrollOffset - optionStartY - optionHeight;

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

		// UI
		UI.getBackButton().draw();
		UI.draw(g);

		// tooltip
		renderTooltip(g, mouseX, mouseY);
	}

	private void renderTooltip(Graphics g, int mouseX, int mouseY) {
		if (hoverOption != null) {
			String optionDescription = hoverOption.getDescription();
			float textWidth = Fonts.SMALL.getWidth(optionDescription);
			Color.black.a = 0.7f;
			g.setColor(Color.black);
			g.fillRoundRect(mouseX + 10, mouseY + 10, 10 + textWidth, 10 + Fonts.SMALL.getLineHeight(), 4);
			Fonts.SMALL.drawString(mouseX + 15, mouseY + 15, optionDescription, Color.white);
			Color.black.a = 1f;
		}
	}

	private void renderOptions(Graphics g) {
		g.setClip(0, optionStartY, width, height - optionStartY);
		int y = -scrollOffset + optionStartY;
		selectedTab = 0;
		for (int tabIndex = 0; tabIndex < tabs.length; tabIndex++) {
			OptionTab tab = tabs[tabIndex];
			if (y > 0) {
				int x = optionStartX + (optionWidth - Fonts.LARGE.getWidth(tab.name)) / 2;
				Fonts.LARGE.drawString(x, y + Fonts.LARGE.getLineHeight() * 0.6f, tab.name, Color.cyan);
			} else {
				selectedTab++;
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
		// scrollbar
		g.setColor(Color.white);
		g.fillRoundRect(optionStartX + optionWidth + 15, optionStartY + ((float) scrollOffset / (maxScrollOffset)) * (height - optionStartY - 45), 10, 45, 2);
		g.clearClip();
	}

	private void renderOption(Graphics g, GameOption option, int y, boolean focus) {
		Color col = focus ? Colors.GREEN : Colors.WHITE_FADE;
		OptionType type = option.getType();
		Object[] listItems = option.getListItems();
		if (listItems != null) {
			renderListOption(g, option, y, col);
		} else if (type == OptionType.BOOLEAN) {
			renderCheckOption(g, option, y, col);
		} else if (type == OptionType.NUMERIC) {
			renderSliderOption(g, option, y, col);
		} else {
			renderGenericOption(g, option, y, col);
		}
	}

	private void renderListOption(Graphics g, GameOption option, int y, Color textColor) {
		Fonts.MEDIUM.drawString(optionStartX, y, option.getName(), textColor);
		Fonts.MEDIUM.drawString(optionStartX + optionWidth / 2, y, option.getValueString(), textColor);
	}

	private void renderCheckOption(Graphics g, GameOption option, int y, Color textColor) {
		if (option.getBooleanValue()) {
			checkOnImg.draw(optionStartX, y + optionHeight / 4, Color.pink);
		} else {
			checkOffImg.draw(optionStartX, y + optionHeight / 4, Color.pink);
		}
		Fonts.MEDIUM.drawString(optionStartX + 30, y, option.getName(), textColor);
	}

	private void renderSliderOption(Graphics g, GameOption option, int y, Color textColor) {
		String value = option.getValueString();
		int nameLen = Fonts.MEDIUM.getWidth(option.getName());
		int valueLen = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(optionStartX, y, option.getName(), textColor);
		Fonts.MEDIUM.drawString(optionStartX + optionWidth - valueLen, y, value, textColor);
		int sliderLen = optionWidth - nameLen - valueLen - 50;

		if (hoverOption == option) {
			if (!isAdjustingSlider) {
				sliderOptionLength = sliderLen;
				sliderOptionStartX = optionStartX + nameLen + 25;
			} else {
				sliderLen = sliderOptionLength;
			}
		}

		g.setColor(Color.pink);
		g.setLineWidth(3f);
		g.drawLine(optionStartX + nameLen + 25, y + optionHeight / 2, optionStartX + nameLen + 25 + sliderLen, y + optionHeight / 2);
		float sliderValue = (float) (sliderLen + 10) * (option.getIntegerValue() - option.getMinValue()) / (option.getMaxValue() - option.getMinValue());
		sliderBallImg.draw(optionStartX + nameLen + 25 + sliderValue - 10, y + optionHeight / 2 - 10, Color.pink);
	}

	private void renderGenericOption(Graphics g, GameOption option, int y, Color textColor) {
		String value = option.getValueString();
		int valueLen = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(optionStartX, y, option.getName(), textColor);
		Fonts.MEDIUM.drawString(optionStartX + optionWidth - valueLen, y, value, textColor);
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
		if (isAdjustingSlider) {
			int min = selectedOption.getMinValue();
			int max = selectedOption.getMaxValue();
			int value = min + (int) ((float) (max - min) * (mouseX - sliderOptionStartX) / (sliderOptionLength));
			selectedOption.setValue(Utils.clamp(value, min, max));
			selectedOption.drag(container, 0);
		}
	}

	public void mousePressed(int button, int x, int y) {
		mousePressY = y;
		selectedOption = hoverOption;

		if (selectedOption != null && selectedOption.getType() == OptionType.NUMERIC) {
			isAdjustingSlider = sliderOptionStartX <= x && x < sliderOptionStartX + sliderOptionLength;
		}

		if (UI.getBackButton().contains(x, y)) {
			parent.onLeave();
			return;
		}
	}

	public void mouseReleased(int button, int x, int y) {
		selectedOption = null;
		isAdjustingSlider = false;
		sliderOptionLength = 0;

		// check if clicked, not dragged
		if (Math.abs(y - mousePressY) >= 5) {
			return;
		}

		if (hoverOption != null && hoverOption.getType() == OptionType.BOOLEAN) {
			hoverOption.click(container);
			SoundController.playSound(SoundEffect.MENUHIT);
			return;
		}

		int tScrollOffset = 0;
		for (int tabIndex = 0; tabIndex < tabs.length; tabIndex++) {
			if (tabs[tabIndex].button.contains(x, y)) {
				if (selectedTab != tabIndex) {
					selectedTab = tabIndex;
					scrollOffset = tScrollOffset;
					SoundController.playSound(SoundEffect.MENUCLICK);
				}
				return;
			}
			tScrollOffset += Fonts.MEDIUM.getLineHeight() * 2;
			tScrollOffset += tabs[tabIndex].options.length * optionHeight;
		}
	}

	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (!isAdjustingSlider) {
			scrollOffset = Utils.clamp(scrollOffset + oldy - newy, 0, maxScrollOffset);
		}
	}

	public void mouseWheelMoved(int delta) {
		if (!isAdjustingSlider) {
			scrollOffset = Utils.clamp(scrollOffset - delta, 0, maxScrollOffset - optionStartY);
		}
	}

	public void keyPressed(int key, char c) {
		switch (key) {
			case Input.KEY_ESCAPE:
				parent.onLeave();
				break;
		}
	}

	private void updateHoverOption(int mouseX, int mouseY) {
		if (selectedOption != null) {
			hoverOption = selectedOption;
			return;
		}
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
