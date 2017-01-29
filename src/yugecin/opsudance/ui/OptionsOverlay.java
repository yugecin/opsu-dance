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
import itdelatrisu.opsu.ui.*;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.*;
import org.newdawn.slick.gui.TextField;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.state.OverlayOpsuState;
import yugecin.opsudance.utils.FontUtil;

import java.util.HashMap;
import java.util.LinkedList;

public class OptionsOverlay extends OverlayOpsuState {

	private final DisplayContainer displayContainer;

	private static final float BG_ALPHA = 0.7f;
	private static final float LINEALPHA = 0.8f;
	private static final Color COL_BG = new Color(Color.black);
	private static final Color COL_WHITE = new Color(1f, 1f, 1f);
	private static final Color COL_PINK = new Color(235, 117, 139);
	private static final Color COL_CYAN = new Color(88, 218, 254);
	private static final Color COL_GREY = new Color(55, 55, 57);
	private static final Color COL_BLUE = new Color(Colors.BLUE_BACKGROUND);
	private static final Color COL_COMBOBOX_HOVER = new Color(185, 19, 121);

	private static final float INDICATOR_ALPHA = 0.8f;
	private static final Color COL_INDICATOR = new Color(Color.black);


	/** Duration, in ms, of the show (slide-in) animation. */
	private static final int SHOWANIMATIONTIME = 1000;
	/** Current value of the animation timer. */
	private int animationtime;
	/** Duration, in ms, of the hide animation. */
	private int hideAnimationTime;
	/** How much the show animation progressed when the hide request was made. */
	private float hideAnimationStartProgress;

	/** Target duration, in ms, of the move animation for the indicator. */
	private static final int INDICATORMOVEANIMATIONTIME = 166;
	/**  Selected option indicator virtual position. */
	private int indicatorPos;
	/** Selected option indicator offset to next position. */
	private int indicatorOffsetToNextPos;
	/** Selected option indicator move to next position animation time past. */
	private int indicatorMoveAnimationTime;
	/** Target duration, in ms, of the fadeout animation for the indicator. */
	private static final int INDICATORHIDEANIMATIONTIME = 500;
	/** Selected option indicator hide animation time past. */
	private int indicatorHideAnimationTime;

	private Listener listener;

	private Image sliderBallImg;
	private Image checkOnImg;
	private Image checkOffImg;
	private Image searchImg;

	private OptionTab[] sections;

	private GameOption hoverOption;
	private GameOption selectedOption;

	private int sliderOptionStartX;
	private int sliderOptionLength;
	private boolean isAdjustingSlider;

	private final HashMap<GameOption, DropdownMenu<Object>> dropdownMenus;
	private final LinkedList<DropdownMenu<Object>> visibleDropdownMenus;
	private int dropdownMenuPaddingY;
	private DropdownMenu<Object> openDropdownMenu;
	private int openDropdownVirtualY;

	private int finalWidth;
	private int width;
	private int height;

	private int optionWidth;
	private int optionStartX;
	private int optionStartY;
	private int optionHeight;
	private int optionTextOffsetY;

	private int controlImageSize;
	private int controlImagePadding;

	private static final int LINEWIDTH = 3;
	private int paddingRight;
	private int paddingLeft;
	private int paddingTextLeft;

	private int textOptionsY;
	private int textChangeY;
	private int posSearchY;
	private int textSearchYOffset;

	private final KineticScrolling scrollHandler;
	private int maxScrollOffset;

	private int mousePressY;

	private boolean keyEntryLeft;
	private boolean keyEntryRight;

	private int prevMouseX;
	private int prevMouseY;

	private int sliderSoundDelay;

	private final TextField searchField;
	private String lastSearchText;

	public OptionsOverlay(DisplayContainer displayContainer, OptionTab[] sections) {
		this.displayContainer = displayContainer;

		this.sections = sections;

		dropdownMenus = new HashMap<>();
		visibleDropdownMenus = new LinkedList<>();

		searchField = new TextField(displayContainer, null, 0, 0, 0, 0);
		searchField.setMaxLength(20);

		scrollHandler = new KineticScrolling();
		scrollHandler.setAllowOverScroll(true);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public void revalidate() {
		super.revalidate();

		finalWidth = Math.max((int) (displayContainer.width * 0.36f), 340); // 0.321f
		height = displayContainer.height;

		// calculate positions
		paddingRight = (int) (displayContainer.width * 0.009375f); // not so accurate
		paddingLeft = (int) (displayContainer.width * 0.0180f); // not so accurate
		paddingTextLeft = paddingLeft + LINEWIDTH + (int) (displayContainer.width * 0.00625f); // not so accurate
		optionStartX = paddingTextLeft;
		textOptionsY = Fonts.LARGE.getLineHeight() * 2;
		textChangeY = textOptionsY + Fonts.LARGE.getLineHeight();
		posSearchY = textChangeY + Fonts.MEDIUM.getLineHeight() * 2;
		textSearchYOffset = Fonts.MEDIUM.getLineHeight() / 2;
		optionStartY = posSearchY + Fonts.MEDIUM.getLineHeight() + Fonts.LARGE.getLineHeight();

		if (active) {
			width = finalWidth;
			optionWidth = width - optionStartX - paddingRight;
		}

		optionHeight = (int) (Fonts.MEDIUM.getLineHeight() * 1.3f);
		optionTextOffsetY = (int) ((optionHeight - Fonts.MEDIUM.getLineHeight()) / 2f);
		controlImageSize = (int) (Fonts.MEDIUM.getLineHeight() * 0.7f);
		controlImagePadding = (optionHeight - controlImageSize) / 2;

		sliderBallImg = GameImage.CONTROL_SLIDER_BALL.getImage().getScaledCopy(controlImageSize, controlImageSize);
		checkOnImg = GameImage.CONTROL_CHECK_ON.getImage().getScaledCopy(controlImageSize, controlImageSize);
		checkOffImg = GameImage.CONTROL_CHECK_OFF.getImage().getScaledCopy(controlImageSize, controlImageSize);

		dropdownMenus.clear();
		for (OptionTab section : sections) {
			if (section.options == null) {
				continue;
			}
			for (final GameOption option : section.options) {
				Object[] items = option.getListItems();
				if (items == null) {
					continue;
				}
				DropdownMenu<Object> menu = new DropdownMenu<Object>(displayContainer, items, 0, 0, 0) {
					@Override
					public void itemSelected(int index, Object item) {
						option.clickListItem(index);
						openDropdownMenu = null;
					}
				};
				// not the best way to determine the selected option AT ALL, but seems like it's the only one right now...
				String selectedValue = option.getValueString();
				int idx = 0;
				for (Object item : items) {
					if (item.toString().equals(selectedValue)) {
						break;
					}
					idx++;
				}
				menu.setSelectedIndex(idx);
				menu.setBackgroundColor(COL_BG);
				menu.setBorderColor(Color.transparent);
				menu.setChevronDownColor(COL_WHITE);
				menu.setChevronRightColor(COL_BG);
				menu.setHighlightColor(COL_COMBOBOX_HOVER);
				menu.setTextColor(COL_WHITE);
				dropdownMenuPaddingY = (optionHeight - menu.getHeight()); // TODO why isn't this /2 ?
				dropdownMenus.put(option, menu);
			}
		}

		int searchImgSize = (int) (Fonts.LARGE.getLineHeight() * 0.75f);
		searchImg = GameImage.SEARCH.getImage().getScaledCopy(searchImgSize, searchImgSize);
	}

	@Override
	public void onRender(Graphics g) {
		g.setClip(0, 0, width, height);

		// bg
		g.setColor(COL_BG);
		g.fillRect(0, 0, width, height);

		// title
		renderTitle();

		renderIndicator(g);

		// options
		renderOptions(g);
		if (openDropdownMenu != null) {
			openDropdownMenu.render(g);
			if (!openDropdownMenu.isOpen()) {
				openDropdownMenu = null;
			}
		}

		renderSearch(g);

		// scrollbar
		g.setColor(COL_WHITE);
		g.fillRect(width - 5, scrollHandler.getPosition() / maxScrollOffset * (height - 45), 5, 45);
		g.clearClip();

		// UI
		UI.getBackButton().draw();

		// tooltip
		renderTooltip(g);

		// key input options
		if (keyEntryLeft || keyEntryRight) {
			renderKeyEntry(g);
		}
	}

	private void renderIndicator(Graphics g) {
		g.setColor(COL_INDICATOR);
		int indicatorPos = this.indicatorPos;
		if (indicatorMoveAnimationTime > 0) {
			indicatorMoveAnimationTime += displayContainer.renderDelta;
			if (indicatorMoveAnimationTime > INDICATORMOVEANIMATIONTIME) {
				indicatorMoveAnimationTime = 0;
				indicatorPos += indicatorOffsetToNextPos;
				indicatorOffsetToNextPos = 0;
				this.indicatorPos = indicatorPos;
			} else {
				indicatorPos += AnimationEquation.OUT_BACK.calc((float) indicatorMoveAnimationTime / INDICATORMOVEANIMATIONTIME) * indicatorOffsetToNextPos;
			}
		}
		g.fillRect(0, indicatorPos - scrollHandler.getPosition(), width, optionHeight);
	}

	private void renderKeyEntry(Graphics g) {
		g.setColor(COL_BG);
		g.fillRect(0, 0, displayContainer.width, height);
		g.setColor(COL_WHITE);
		String prompt = (keyEntryLeft) ? "Please press the new left-click key." : "Please press the new right-click key.";
		int y = (displayContainer.height - Fonts.LARGE.getLineHeight()) / 2;
		FontUtil.drawCentered(Fonts.LARGE, displayContainer.width, 0, y, prompt, COL_WHITE);
	}

	private void renderTooltip(Graphics g) {
		if (hoverOption != null) {
			String tip = hoverOption.getDescription();
			if (hoverOption.getType() == OptionType.NUMERIC) {
				tip = "(" + hoverOption.getValueString() + ") " + tip;
			}
			UI.updateTooltip(displayContainer.renderDelta, tip, true);
			UI.drawTooltip(g);
		}
	}

	private void renderOptions(Graphics g) {
		visibleDropdownMenus.clear();
		int y = -scrollHandler.getIntPosition() + optionStartY;
		maxScrollOffset = optionStartY;
		boolean render = true;
		int sectionIndex = 0;
		for (; sectionIndex < sections.length && render; sectionIndex++) {
			OptionTab section = sections[sectionIndex];
			if (section.filtered) {
				continue;
			}
			int lineStartY = (int) (y + Fonts.LARGE.getLineHeight() * 0.6f);
			if (render) {
				if (section.options == null) {
					FontUtil.drawRightAligned(Fonts.XLARGE, width, -paddingRight, (int) (y + Fonts.XLARGE.getLineHeight() * 0.3f), section.name, COL_CYAN);
				} else {
					Fonts.MEDIUMBOLD.drawString(paddingTextLeft, lineStartY, section.name, COL_WHITE);
				}
			}
			y += Fonts.LARGE.getLineHeight() * 1.5f;
			maxScrollOffset += Fonts.LARGE.getLineHeight() * 1.5f;
			if (section.options == null) {
				continue;
			}
			int lineHeight = (int) (Fonts.LARGE.getLineHeight() * 0.9f);
			for (int optionIndex = 0; optionIndex < section.options.length; optionIndex++) {
				GameOption option = section.options[optionIndex];
				if (!option.showCondition() || option.isFiltered()) {
					continue;
				}
				if (y > -optionHeight || (openDropdownMenu != null && openDropdownMenu.equals(dropdownMenus.get(option)))) {
					renderOption(g, option, y);
				}
				y += optionHeight;
				maxScrollOffset += optionHeight;
				lineHeight += optionHeight;
				if (y > height) {
					render = false;
					maxScrollOffset += (section.options.length - optionIndex - 1) * optionHeight;
					break;
				}
			}
			g.setColor(COL_GREY);
			g.fillRect(paddingLeft, lineStartY, LINEWIDTH, lineHeight);
		}
		// iterate over skipped options to correctly calculate max scroll offset
		for (; sectionIndex < sections.length; sectionIndex++) {
			maxScrollOffset += Fonts.LARGE.getLineHeight() * 1.5f;
			if (sections[sectionIndex].options != null) {
				maxScrollOffset += sections[sectionIndex].options.length * optionHeight;
			}
		}
		if (openDropdownMenu != null) {
			maxScrollOffset = Math.max(maxScrollOffset, openDropdownVirtualY + openDropdownMenu.getHeight());
		}
		maxScrollOffset -= height * 2 / 3;
		if (maxScrollOffset < 0) {
			maxScrollOffset = 0;
		}
		scrollHandler.setMinMax(0, maxScrollOffset);
	}

	private void renderOption(Graphics g, GameOption option, int y) {
		OptionType type = option.getType();
		Object[] listItems = option.getListItems();
		if (listItems != null) {
			renderListOption(g, option, y);
		} else if (type == OptionType.BOOLEAN) {
			renderCheckOption(option, y);
		} else if (type == OptionType.NUMERIC) {
			renderSliderOption(g, option, y);
		} else {
			renderGenericOption(option, y);
		}
	}

	private void renderListOption(Graphics g, GameOption option, int y) {
		// draw option name
		int nameWith = Fonts.MEDIUM.getWidth(option.getName());
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.getName(), COL_WHITE);
		nameWith += 15;
		int comboboxStartX = optionStartX + nameWith;
		int comboboxWidth = optionWidth - nameWith;
		if (comboboxWidth < controlImageSize) {
			return;
		}
		DropdownMenu<Object> dropdown = dropdownMenus.get(option);
		if (dropdown == null) {
			return;
		}
		visibleDropdownMenus.add(dropdown);
		dropdown.setWidth(comboboxWidth);
		dropdown.x = comboboxStartX;
		dropdown.y = y + dropdownMenuPaddingY;
		if (dropdown.isOpen()) {
			openDropdownMenu = dropdown;
			openDropdownVirtualY = maxScrollOffset;
			return;
		}
		dropdown.render(g);
	}

	private void renderCheckOption(GameOption option, int y) {
		if (option.getBooleanValue()) {
			checkOnImg.draw(optionStartX, y + controlImagePadding, COL_PINK);
		} else {
			checkOffImg.draw(optionStartX, y + controlImagePadding, COL_PINK);
		}
		Fonts.MEDIUM.drawString(optionStartX + 30, y + optionTextOffsetY, option.getName(), COL_WHITE);
	}

	private void renderSliderOption(Graphics g, GameOption option, int y) {
		final int padding = 10;
		int nameLen = Fonts.MEDIUM.getWidth(option.getName());
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.getName(), COL_WHITE);
		int sliderLen = optionWidth - nameLen - padding;
		if (sliderLen <= 1) {
			return;
		}
		int sliderStartX = optionStartX + nameLen + padding;
		int sliderEndX = optionStartX + optionWidth;

		if (hoverOption == option) {
			if (!isAdjustingSlider) {
				sliderOptionLength = sliderLen;
				sliderOptionStartX = sliderStartX;
			} else {
				sliderLen = sliderOptionLength;
			}
		}

		float sliderValue = (float) (option.getIntegerValue() - option.getMinValue()) / (option.getMaxValue() - option.getMinValue());
		float sliderBallPos = sliderStartX + (int) ((sliderLen - controlImageSize) * sliderValue);

		g.setLineWidth(3f);
		g.setColor(COL_PINK);
		if (sliderValue > 0.0001f) {
			g.drawLine(sliderStartX, y + optionHeight / 2, sliderBallPos, y + optionHeight / 2);
		}
		sliderBallImg.draw(sliderBallPos, y + controlImagePadding, COL_PINK);
		if (sliderValue < 0.999f) {
			float a = COL_PINK.a;
			COL_PINK.a *= 0.45f;
			g.setColor(COL_PINK);
			g.drawLine(sliderBallPos + controlImageSize + 1, y + optionHeight / 2, sliderEndX, y + optionHeight / 2);
			COL_PINK.a = a;
		}
	}

	private void renderGenericOption(GameOption option, int y) {
		String value = option.getValueString();
		int valueLen = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.getName(), COL_WHITE);
		Fonts.MEDIUM.drawString(optionStartX + optionWidth - valueLen, y + optionTextOffsetY, value, COL_BLUE);
	}

	private void renderTitle() {
		FontUtil.drawCentered(Fonts.LARGE, width, 0, textOptionsY - scrollHandler.getIntPosition(), "Options", COL_WHITE);
		FontUtil.drawCentered(Fonts.MEDIUM, width, 0, textChangeY - scrollHandler.getIntPosition(), "Change the way opsu! behaves", COL_PINK);
	}

	private void renderSearch(Graphics g) {
		int ypos = posSearchY + textSearchYOffset - scrollHandler.getIntPosition();
		if (scrollHandler.getIntPosition() > posSearchY) {
			ypos = textSearchYOffset;
			g.setColor(COL_BG);
			g.fillRect(0, 0, width, textSearchYOffset * 2 + Fonts.LARGE.getLineHeight());
		}
		String searchText = "Type to search!";
		if (lastSearchText.length() > 0) {
			searchText = lastSearchText;
		}
		FontUtil.drawCentered(Fonts.LARGE, width, 0, ypos, searchText, COL_WHITE);
		int imgPosX = (width - Fonts.LARGE.getWidth(searchText)) / 2 - searchImg.getWidth() - 10;
		searchImg.draw(imgPosX, ypos + Fonts.LARGE.getLineHeight() * 0.25f, COL_WHITE);
	}

	@Override
	public void hide() {
		searchField.setFocused(false);
		acceptInput = false;
		SoundController.playSound(SoundEffect.MENUBACK);
		hideAnimationTime = animationtime;
		hideAnimationStartProgress = (float) animationtime / SHOWANIMATIONTIME;
	}

	@Override
	public void show() {
		indicatorPos = -optionHeight;
		indicatorOffsetToNextPos = 0;
		indicatorMoveAnimationTime = 0;
		indicatorHideAnimationTime = 0;
		acceptInput = true;
		active = true;
		animationtime = 0;
		resetSearch();
	}

	@Override
	public void onPreRenderUpdate() {
		int mouseX = displayContainer.mouseX;
		int mouseY = displayContainer.mouseY;
		int delta = displayContainer.renderDelta;

		scrollHandler.update(delta);

		if (openDropdownMenu == null) {
			for (DropdownMenu<Object> menu : visibleDropdownMenus) {
				menu.updateHover(mouseX, mouseY);
			}
		} else {
			openDropdownMenu.updateHover(mouseX, mouseY);
		}

		updateShowHideAnimation(delta);
		if (animationtime <= 0) {
			active = false;
			return;
		}

		if (sliderSoundDelay > 0) {
			sliderSoundDelay -= delta;
		}

		if (mouseX - prevMouseX == 0 && mouseY - prevMouseY == 0) {
			updateIndicatorAlpha();
			return;
		}
		prevMouseX = mouseX;
		prevMouseY = mouseY;
		updateHoverOption(mouseX, mouseY);
		updateIndicatorAlpha();
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
		if (isAdjustingSlider) {
			int sliderValue = hoverOption.getIntegerValue();
			updateSliderOption();
			if (hoverOption.getIntegerValue() - sliderValue != 0 && sliderSoundDelay <= 0) {
				sliderSoundDelay = 90;
				SoundController.playSound(SoundEffect.MENUHIT);
			}
		}
	}

	private void updateIndicatorAlpha() {
		if (hoverOption == null) {
			if (indicatorHideAnimationTime < INDICATORHIDEANIMATIONTIME) {
				indicatorHideAnimationTime += displayContainer.renderDelta;
				if (indicatorHideAnimationTime > INDICATORHIDEANIMATIONTIME) {
					indicatorHideAnimationTime = INDICATORHIDEANIMATIONTIME;
				}
				float progress = AnimationEquation.IN_CUBIC.calc((float) indicatorHideAnimationTime / INDICATORHIDEANIMATIONTIME);
				COL_INDICATOR.a = (1f - progress) * INDICATOR_ALPHA;
			}
		} else if (indicatorHideAnimationTime > 0) {
			indicatorHideAnimationTime -= displayContainer.renderDelta * 3;
			if (indicatorHideAnimationTime < 0) {
				indicatorHideAnimationTime = 0;
			}
			COL_INDICATOR.a = (1f - (float) indicatorHideAnimationTime / INDICATORHIDEANIMATIONTIME) * INDICATOR_ALPHA;
		}
	}

	private void updateShowHideAnimation(int delta) {
		if (acceptInput && animationtime >= SHOWANIMATIONTIME) {
			// animation already finished
			width = finalWidth;
			return;
		}
		optionWidth = width - optionStartX - paddingRight;

		// if acceptInput is false, it means that we're currently hiding ourselves
		float progress;
		if (acceptInput) {
			animationtime += delta;
			if (animationtime >= SHOWANIMATIONTIME) {
				animationtime = SHOWANIMATIONTIME;
			}
			progress = AnimationEquation.OUT_EXPO.calc((float) animationtime / SHOWANIMATIONTIME);
		} else {
			animationtime -= delta;
			if (animationtime < 0) {
				animationtime = 0;
			}
			progress = hideAnimationStartProgress * AnimationEquation.IN_EXPO.calc((float) animationtime / hideAnimationTime);
		}
		width = (int) (progress * finalWidth);
		COL_BG.a = BG_ALPHA * progress;
		COL_WHITE.a = progress;
		COL_PINK.a = progress;
		COL_CYAN.a = progress;
		COL_GREY.a = progress * LINEALPHA;
		COL_BLUE.a = progress;
		COL_COMBOBOX_HOVER.a = progress;
		COL_INDICATOR.a = progress * (1f - (float) indicatorHideAnimationTime / INDICATORHIDEANIMATIONTIME) * INDICATOR_ALPHA;
	}

	@Override
	public boolean onMousePressed(int button, int x, int y) {
		if (keyEntryLeft || keyEntryRight) {
			keyEntryLeft = keyEntryRight = false;
			return true;
		}

		if (x > width) {
			return false;
		}

		scrollHandler.pressed();

		mousePressY = y;
		selectedOption = hoverOption;

		if (hoverOption != null && hoverOption.getType() == OptionType.NUMERIC) {
			isAdjustingSlider = sliderOptionStartX <= x && x < sliderOptionStartX + sliderOptionLength;
			if (isAdjustingSlider) {
				updateSliderOption();
			}
		}

		return true;
	}

	@Override
	public boolean onMouseReleased(int button, int x, int y) {
		selectedOption = null;
		if (isAdjustingSlider && listener != null) {
			listener.onSaveOption(hoverOption);
		}
		isAdjustingSlider = false;
		sliderOptionLength = 0;

		if (openDropdownMenu != null) {
			openDropdownMenu.mouseReleased(button);
			updateHoverOption(x, y);
			return true;
		} else {
			for (DropdownMenu<Object> menu : visibleDropdownMenus) {
				menu.mouseReleased(button);
				if (menu.isOpen()) {
					return true;
				}
			}
		}

		scrollHandler.released();

		// check if clicked, not dragged
		if (Math.abs(y - mousePressY) >= 5) {
			return true;
		}

		if (x > finalWidth) {
			return false;
		}

		if (hoverOption != null) {
			if (hoverOption.getType() == OptionType.BOOLEAN) {
				hoverOption.click();
				if (listener != null) {
					listener.onSaveOption(hoverOption);
				}
				SoundController.playSound(SoundEffect.MENUHIT);
				return true;
			} else if (hoverOption == GameOption.KEY_LEFT) {
				keyEntryLeft = true;
			} else if (hoverOption == GameOption.KEY_RIGHT) {
				keyEntryLeft = true;
			}
		}

		if (UI.getBackButton().contains(x, y)){
			hide();
			if (listener != null) {
				listener.onLeaveOptionsMenu();
			}
		}
		return true;
	}

	@Override
	public boolean onMouseDragged(int oldx, int oldy, int newx, int newy) {
		if (!isAdjustingSlider) {
			int diff = newy - oldy;
			if (diff != 0) {
				scrollHandler.dragged(-diff);
			}
		}
		return true;
	}

	@Override
	public boolean onMouseWheelMoved(int delta) {
		if (!isAdjustingSlider) {
			scrollHandler.scrollOffset(-delta);
		}
		updateHoverOption(prevMouseX, prevMouseY);
		return true;
	}

	@Override
	public boolean onKeyPressed(int key, char c) {
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

		if (key == Input.KEY_ESCAPE) {
			if (openDropdownMenu != null) {
				openDropdownMenu.keyPressed(key, c);
				return true;
			}
			if (lastSearchText.length() != 0) {
				resetSearch();
				updateHoverOption(prevMouseX, prevMouseY);
				return true;
			}
			hide();
			if (listener != null) {
				listener.onLeaveOptionsMenu();
			}
			return true;
		}

		searchField.keyPressed(key, c);
		if (!searchField.getText().equals(lastSearchText)) {
			lastSearchText = searchField.getText().toLowerCase();
			updateSearch();
		}

		return true;
	}

	@Override
	public boolean onKeyReleased(int key, char c) {
		return false;
	}

	private void updateSliderOption() {
		int min = hoverOption.getMinValue();
		int max = hoverOption.getMaxValue();
		int value = min + Math.round((float) (max - min) * (displayContainer.mouseX - sliderOptionStartX) / (sliderOptionLength));
		hoverOption.setValue(Utils.clamp(value, min, max));
	}

	private void updateHoverOption(int mouseX, int mouseY) {
		if (openDropdownMenu != null || keyEntryLeft || keyEntryRight) {
			return;
		}
		if (selectedOption != null) {
			hoverOption = selectedOption;
			return;
		}
		hoverOption = null;
		if (mouseX > width) {
			return;
		}

		int mouseVirtualY = scrollHandler.getIntPosition() + mouseY - optionStartY;
		for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
			OptionTab section = sections[sectionIndex];
			if (section.filtered) {
				continue;
			}
			mouseVirtualY -= Fonts.LARGE.getLineHeight() * 1.5f;
			if (section.options == null) {
				continue;
			}
			for (int optionIndex = 0; optionIndex < section.options.length; optionIndex++) {
				GameOption option = section.options[optionIndex];
				if (option.isFiltered() || !option.showCondition()) {
					continue;
				}
				if (mouseVirtualY <= optionHeight) {
					if (mouseVirtualY >= 0) {
						int indicatorPos = scrollHandler.getIntPosition() + mouseY - mouseVirtualY;
						if (indicatorPos != this.indicatorPos + indicatorOffsetToNextPos) {
							this.indicatorPos += indicatorOffsetToNextPos; // finish the current moving animation
							indicatorOffsetToNextPos = indicatorPos - this.indicatorPos;
							indicatorMoveAnimationTime = 1; // starts animation
						}
						hoverOption = option;
					}
					return;
				}
				mouseVirtualY -= optionHeight;
			}
		}
	}

	private void resetSearch() {
		for (OptionTab section : sections) {
			section.filtered = false;
			if (section.options == null) {
				continue;
			}
			for (GameOption opt : section.options) {
				opt.filter(null);
			}
		}
		searchField.setText("");
		lastSearchText = "";
	}

	private void updateSearch() {
		OptionTab lastBigSection = null;
		boolean lastBigSectionMatches = false;
		for (OptionTab section : sections) {
			boolean sectionMatches = section.name.toLowerCase().contains(lastSearchText);
			if (section.options == null) {
				lastBigSectionMatches = sectionMatches;
				lastBigSection = section;
				section.filtered = true;
				continue;
			}
			section.filtered = true;
			for (GameOption option : section.options) {
				if (lastBigSectionMatches || sectionMatches) {
					section.filtered = false;
					option.filter(null);
					continue;
				}
				if (!option.filter(lastSearchText)) {
					section.filtered = false;
					lastBigSection.filtered = false;
				}
			}
		}
		updateHoverOption(prevMouseX, prevMouseY);
	}

	public static class OptionTab {

		public final String name;
		public final GameOption[] options;
		private boolean filtered;

		public OptionTab(String name, GameOption[] options) {
			this.name = name;
			this.options = options;
		}

	}

	public interface Listener {

		void onLeaveOptionsMenu();
		void onSaveOption(GameOption option);

	}

}
