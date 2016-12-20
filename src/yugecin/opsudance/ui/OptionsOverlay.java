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
import itdelatrisu.opsu.Options;
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

@SuppressWarnings("UnusedParameters")
public class OptionsOverlay {

	private Parent parent;
	private GameContainer container;

	private final Image sliderBallImg;
	private final Image checkOnImg;
	private final Image checkOffImg;

	private OptionTab[] tabs;
	private int selectedTab;

	private GameOption hoverOption;
	private GameOption selectedOption;

	private int sliderOptionStartX;
	private int sliderOptionLength;
	private boolean isAdjustingSlider;

	private boolean isListOptionOpen;
	private int listItemHeight;
	private int listStartX;
	private int listStartY;
	private int listWidth;
	private int listHeight;
	private int listHoverIndex;

	private int width;
	private int height;

	private int optionWidth;
	private int optionStartX;
	private int optionStartY;
	private int optionHeight;

	private int scrollOffset;
	private int maxScrollOffset;

	private int mousePressY;

	private boolean keyEntryLeft;
	private boolean keyEntryRight;

	private int prevMouseX;
	private int prevMouseY;

	private int sliderSoundDelay;

	public OptionsOverlay(Parent parent, OptionTab[] tabs, int defaultSelectedTabIndex, GameContainer container) {
		this.parent = parent;
		this.container = container;

		this.tabs = tabs;
		selectedTab = defaultSelectedTabIndex;

		listHoverIndex = -1;

		sliderBallImg = GameImage.CONTROL_SLIDER_BALL.getImage().getScaledCopy(20, 20);
		checkOnImg = GameImage.CONTROL_CHECK_ON.getImage().getScaledCopy(20, 20);
		checkOffImg = GameImage.CONTROL_CHECK_OFF.getImage().getScaledCopy(20, 20);

		width = container.getWidth();
		height = container.getHeight();

		// calculate positions
		optionWidth = width / 2;
		optionHeight = (int) ((Fonts.MEDIUM.getLineHeight()) * 1.1f);
		listItemHeight = (int) (optionHeight * 4f / 5f);
		optionStartX = optionWidth / 2;

		// initialize tabs
		Image tabImage = GameImage.MENU_TAB.getImage();
		float tabX = width * 0.032f + (tabImage.getWidth() / 3);
		float tabY = Fonts.XLARGE.getLineHeight() + Fonts.DEFAULT.getLineHeight() + height * 0.015f - (tabImage.getHeight() / 2f);
		int tabOffset = Math.min(tabImage.getWidth(), width / tabs.length);
		maxScrollOffset = Fonts.MEDIUM.getLineHeight() * 2 * tabs.length;
		scrollOffset = 0;
		for (OptionTab tab : tabs) {
			if (defaultSelectedTabIndex-- > 0) {
				scrollOffset += Fonts.MEDIUM.getLineHeight() * 2;
				scrollOffset += tab.options.length * optionHeight;
			}
			maxScrollOffset += tab.options.length * optionHeight;
			tab.button = new MenuButton(tabImage, tabX, tabY);
			tabX += tabOffset;
			if (tabX + tabOffset > width) {
				tabX = 0;
				tabY += GameImage.MENU_TAB.getImage().getHeight() / 2f;
			}
		}
		maxScrollOffset += -optionStartY - optionHeight;

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
		if (isListOptionOpen) {
			renderOpenList(g);
		}

		// scrollbar
		g.setColor(Color.white);
		g.fillRoundRect(optionStartX + optionWidth + 15, optionStartY + ((float) scrollOffset / (maxScrollOffset)) * (height - optionStartY - 45), 10, 45, 2);
		g.clearClip();

		// UI
		UI.getBackButton().draw();

		// tooltip
		renderTooltip(g, mouseX, mouseY);

		// key input options
		if (keyEntryLeft || keyEntryRight) {
			renderKeyEntry(g);
		}
	}

	private void renderKeyEntry(Graphics g) {
		g.setColor(Colors.BLACK_ALPHA_75);
		g.fillRect(0, 0, width, height);
		g.setColor(Color.white);
		String prompt = (keyEntryLeft) ? "Please press the new left-click key." : "Please press the new right-click key.";
		Fonts.LARGE.drawString((width - Fonts.LARGE.getWidth(prompt)) / 2, (height - Fonts.LARGE.getLineHeight()) / 2, prompt);
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
		listStartX = listStartY = listWidth = listHeight = 0; // render out of the screen
		int y = -scrollOffset + optionStartY;
		selectedTab = 0;
		maxScrollOffset = Fonts.MEDIUM.getLineHeight() * 2 * tabs.length;
		boolean render = true;
		for (int tabIndex = 0; tabIndex < tabs.length; tabIndex++) {
			OptionTab tab = tabs[tabIndex];
			if (y > 0) {
				if (render) {
					int x = optionStartX + (optionWidth - Fonts.LARGE.getWidth(tab.name)) / 2;
					Fonts.LARGE.drawString(x, y + Fonts.LARGE.getLineHeight() * 0.6f, tab.name, Color.cyan);
				}
			} else {
				selectedTab++;
			}
			y += Fonts.MEDIUM.getLineHeight() * 2;
			for (int optionIndex = 0; optionIndex < tab.options.length; optionIndex++) {
				GameOption option = tab.options[optionIndex];
				if (!option.showCondition()) {
					continue;
				}
				maxScrollOffset += optionHeight;
				if ((y > 0 && render) || (isListOptionOpen && hoverOption == option)) {
					renderOption(g, option, y, option == hoverOption);
				}
				y += optionHeight;
				if (y > height) {
					render = false;
					tabIndex = tabs.length;
				}
			}
		}
		maxScrollOffset -= optionStartY - optionHeight * 2;
	}

	private void renderOpenList(Graphics g) {
		g.setColor(Colors.BLACK_ALPHA_85);
		g.fillRect(listStartX, listStartY, listWidth, listHeight);
		if (listHoverIndex != -1) {
			g.setColor(Colors.ORANGE_BUTTON);
			g.fillRect(listStartX, listStartY + listHoverIndex * listItemHeight, listWidth, listItemHeight);
		}
		g.setLineWidth(1f);
		g.setColor(Color.white);
		g.drawRect(listStartX, listStartY, listWidth, listHeight);
		Object[] listItems = hoverOption.getListItems();
		int y = listStartY;
		for (Object item : listItems) {
			Fonts.MEDIUM.drawString(listStartX + 20, y - Fonts.MEDIUM.getLineHeight() * 0.05f, item.toString());
			y += listItemHeight;
		}
	}

	private void renderOption(Graphics g, GameOption option, int y, boolean focus) {
		Color col = focus ? Colors.GREEN : Colors.WHITE_FADE;
		OptionType type = option.getType();
		Object[] listItems = option.getListItems();
		if (listItems != null) {
			renderListOption(g, option, y, col, listItems);
		} else if (type == OptionType.BOOLEAN) {
			renderCheckOption(g, option, y, col);
		} else if (type == OptionType.NUMERIC) {
			renderSliderOption(g, option, y, col);
		} else {
			renderGenericOption(g, option, y, col);
		}
	}

	private void renderListOption(Graphics g, GameOption option, int y, Color textColor, Object[] listItems) {
		int nameLen = Fonts.MEDIUM.getWidth(option.getName());
		Fonts.MEDIUM.drawString(optionStartX, y, option.getName(), textColor);
		int padding = (int) (optionHeight / 10f);
		nameLen += 20;
		int itemStart = optionStartX + nameLen;
		int itemWidth = optionWidth - nameLen;
		Color backColor = Colors.BLACK_ALPHA;
		if (hoverOption == option && listHoverIndex == -1) {
			backColor = Colors.ORANGE_BUTTON;
		}
		g.setColor(backColor);
		g.fillRect(itemStart, y + padding, itemWidth, listItemHeight);
		g.setColor(Color.white);
		g.setLineWidth(1f);
		g.drawRect(itemStart, y + padding, itemWidth, listItemHeight);
		Fonts.MEDIUM.drawString(itemStart + 20, y, option.getValueString(), Color.white);
		if (isListOptionOpen && hoverOption == option) {
			listStartX = optionStartX + nameLen;
			listStartY = y + padding + listItemHeight;
			listWidth = itemWidth;
			listHeight = listItems.length * listItemHeight;
		}
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
		Fonts.MEDIUM.drawString(optionStartX + optionWidth - valueLen, y, value, Colors.BLUE_BACKGROUND);
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
		Fonts.MEDIUM.drawString(optionStartX + optionWidth - valueLen, y, value, Colors.BLUE_BACKGROUND);
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
		if (sliderSoundDelay > 0) {
			sliderSoundDelay -= delta;
		}
		if (mouseX - prevMouseX == 0 && mouseY - prevMouseY == 0) {
			return;
		}
		prevMouseX = mouseX;
		prevMouseY = mouseY;
		updateHoverOption(mouseX, mouseY);
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
		if (isAdjustingSlider) {
			int sliderValue = hoverOption.getIntegerValue();
			updateSliderOption(mouseX, mouseY);
			if (hoverOption.getIntegerValue() - sliderValue != 0 && sliderSoundDelay <= 0) {
				sliderSoundDelay = 90;
				SoundController.playSound(SoundEffect.MENUHIT);
			}
		} else if (isListOptionOpen) {
			if (listStartX <= mouseX && mouseX < listStartX + listWidth && listStartY <= mouseY && mouseY < listStartY + listHeight) {
				listHoverIndex = (mouseY - listStartY) / listItemHeight;
			} else {
				listHoverIndex = -1;
			}
		}
	}

	public void mousePressed(int button, int x, int y) {
		if (keyEntryLeft || keyEntryRight) {
			keyEntryLeft = keyEntryRight = false;
			return;
		}

		if (isListOptionOpen) {
			if (y > optionStartY && listStartX <= x && x < listStartX + listWidth && listStartY <= y && y < listStartY + listHeight) {
				hoverOption.clickListItem(listHoverIndex);
				parent.onSaveOption(hoverOption);
				SoundController.playSound(SoundEffect.MENUCLICK);
			}
			isListOptionOpen = false;
			listHoverIndex = -1;
			updateHoverOption(x, y);
			return;
		}

		mousePressY = y;
		selectedOption = hoverOption;

		if (hoverOption != null) {
			if (hoverOption.getListItems() != null) {
				isListOptionOpen = true;
			} else if (hoverOption.getType() == OptionType.NUMERIC) {
				isAdjustingSlider = sliderOptionStartX <= x && x < sliderOptionStartX + sliderOptionLength;
				if (isAdjustingSlider) {
					updateSliderOption(x, y);
				}
			}
		}

		if (UI.getBackButton().contains(x, y)) {
			parent.onLeave();
		}
	}

	public void mouseReleased(int button, int x, int y) {
		selectedOption = null;
		if (isAdjustingSlider) {
			parent.onSaveOption(hoverOption);
		}
		isAdjustingSlider = false;
		sliderOptionLength = 0;

		// check if clicked, not dragged
		if (Math.abs(y - mousePressY) >= 5) {
			return;
		}

		if (hoverOption != null) {
			if (hoverOption.getType() == OptionType.BOOLEAN) {
				hoverOption.click(container);
				parent.onSaveOption(hoverOption);
				SoundController.playSound(SoundEffect.MENUHIT);
				return;
			} else if (hoverOption == GameOption.KEY_LEFT) {
				keyEntryLeft = true;
			} else if (hoverOption == GameOption.KEY_RIGHT) {
				keyEntryLeft = true;
			}
		}

		// check if tab was clicked
		int tScrollOffset = 0;
		for (OptionTab tab : tabs) {
			if (tab.button.contains(x, y)) {
				scrollOffset = tScrollOffset;
				SoundController.playSound(SoundEffect.MENUCLICK);
				return;
			}
			tScrollOffset += Fonts.MEDIUM.getLineHeight() * 2;
			tScrollOffset += tab.options.length * optionHeight;
		}
	}

	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (!isAdjustingSlider) {
			scrollOffset = Utils.clamp(scrollOffset + oldy - newy, 0, maxScrollOffset);
		}
	}

	public void mouseWheelMoved(int delta) {
		if (!isAdjustingSlider) {
			scrollOffset = Utils.clamp(scrollOffset - delta, 0, maxScrollOffset);
		}
		updateHoverOption(prevMouseX, prevMouseY);
	}

	public boolean keyPressed(int key, char c) {
		if (keyEntryRight) {
			Options.setGameKeyRight(key);
			keyEntryRight = false;
			return true;
		}

		if (keyEntryLeft) {
			Options.setGameKeyLeft(key);
			keyEntryLeft = false;
			return true;
		}

		switch (key) {
			case Input.KEY_ESCAPE:
				if (isListOptionOpen) {
					isListOptionOpen = false;
					listHoverIndex = -1;
					return true;
				}
				parent.onLeave();
				return true;
		}
		return false;
	}

	private void updateSliderOption(int mouseX, int mouseY) {
		int min = hoverOption.getMinValue();
		int max = hoverOption.getMaxValue();
		int value = min + Math.round((float) (max - min) * (mouseX - sliderOptionStartX) / (sliderOptionLength));
		hoverOption.setValue(Utils.clamp(value, min, max));
	}

	private void updateHoverOption(int mouseX, int mouseY) {
		if (isListOptionOpen || keyEntryLeft || keyEntryRight) {
			return;
		}
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
