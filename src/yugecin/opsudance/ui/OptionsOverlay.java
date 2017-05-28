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
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.ui.*;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.lwjgl.input.Keyboard;
import org.newdawn.slick.*;
import org.newdawn.slick.gui.TextField;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.state.OverlayOpsuState;
import yugecin.opsudance.options.*;
import yugecin.opsudance.utils.FontUtil;

import java.util.HashMap;
import java.util.LinkedList;

import static yugecin.opsudance.options.Options.*;

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
	private static final Color COL_NAV_BG = new Color(COL_BG);
	private static final Color COL_NAV_INDICATOR = new Color(COL_PINK);
	private static final Color COL_NAV_WHITE = new Color(COL_WHITE);
	private static final Color COL_NAV_FILTERED = new Color(37, 37, 37);
	private static final Color COL_NAV_INACTIVE = new Color(153, 153, 153);
	private static final Color COL_NAV_FILTERED_HOVERED = new Color(58, 58, 58);

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
	private OptionTab activeSection;
	private OptionTab hoveredNavigationEntry;

	private Option hoverOption;
	private Option selectedOption;

	private int sliderOptionStartX;
	private int sliderOptionLength;
	private boolean isAdjustingSlider;

	private final HashMap<ListOption, DropdownMenu<Object>> dropdownMenus;
	private final LinkedList<DropdownMenu<Object>> visibleDropdownMenus;
	private int dropdownMenuPaddingY;
	private DropdownMenu<Object> openDropdownMenu;
	private int openDropdownVirtualY;

	private int finalWidth;
	private int width;
	private int height;

	private int navButtonSize;
	private int navStartY;
	private int navExpadedWidth;
	private int navWidth;
	private int navHoverTime;
	private int navIndicatorSize;

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

	private int sectionLineHeight;

	private final TextField searchField;
	private String lastSearchText;

	public OptionsOverlay(DisplayContainer displayContainer, OptionTab[] sections) {
		this.displayContainer = displayContainer;

		this.sections = sections;

		dropdownMenus = new HashMap<>();
		visibleDropdownMenus = new LinkedList<>();

		searchField = new TextField(null, 0, 0, 0, 0);
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
		float navIconWidthRatio = displayContainer.isWidescreen() ? 0.046875f : 0.065f;
		// non-widescreen ratio is not accurate
		navButtonSize = (int) (displayContainer.width * navIconWidthRatio);
		navIndicatorSize = navButtonSize / 10;
		navExpadedWidth = (int) (finalWidth * 0.45f) - navButtonSize;
		paddingRight = (int) (displayContainer.width * 0.009375f); // not so accurate
		paddingLeft = navButtonSize + (int) (displayContainer.width * 0.0180f); // not so accurate
		paddingTextLeft = paddingLeft + LINEWIDTH + (int) (displayContainer.width * 0.00625f); // not so accurate
		optionStartX = paddingTextLeft;
		textOptionsY = Fonts.LARGE.getLineHeight() * 2;
		textChangeY = textOptionsY + Fonts.LARGE.getLineHeight();
		posSearchY = textChangeY + Fonts.MEDIUM.getLineHeight() * 2;
		textSearchYOffset = Fonts.MEDIUM.getLineHeight() / 2;
		optionStartY = posSearchY + Fonts.MEDIUM.getLineHeight() + Fonts.LARGE.getLineHeight();
		sectionLineHeight = (int) (Fonts.LARGE.getLineHeight() * 1.5f);

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

		int navTotalHeight = 0;
		dropdownMenus.clear();
		for (OptionTab section : sections) {
			if (section.options == null) {
				navTotalHeight += navButtonSize;
				continue;
			}
			for (final Option option : section.options) {
				if (!(option instanceof ListOption)) {
					continue;
				}
				final ListOption listOption = (ListOption) option;
				Object[] items = listOption.getListItems();
				DropdownMenu<Object> menu = new DropdownMenu<Object>(displayContainer, items, 0, 0, 0) {
					@Override
					public void itemSelected(int index, Object item) {
						listOption.clickListItem(index);
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
				dropdownMenuPaddingY = (optionHeight - menu.getHeight()) / 2;
				dropdownMenus.put(listOption, menu);
			}
		}
		navStartY = (height - navTotalHeight) / 2;

		int searchImgSize = (int) (Fonts.LARGE.getLineHeight() * 0.75f);
		searchImg = GameImage.SEARCH.getImage().getScaledCopy(searchImgSize, searchImgSize);
	}

	@Override
	public void onRender(Graphics g) {
		g.setClip(navButtonSize, 0, width - navButtonSize, height);

		// bg
		g.setColor(COL_BG);
		g.fillRect(navButtonSize, 0, width, height);

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

		renderNavigation(g);

		// UI
		UI.getBackButton().draw(g);

		// tooltip
		renderTooltip(g);

		// key input options
		if (keyEntryLeft || keyEntryRight) {
			renderKeyEntry(g);
		}
	}

	private void renderNavigation(Graphics g) {
		navWidth = navButtonSize;
		if (navHoverTime >= 600) {
			navWidth += navExpadedWidth;
		} else if (navHoverTime > 300) {
			AnimationEquation anim = AnimationEquation.IN_EXPO;
			if (displayContainer.mouseX < navWidth) {
				anim = AnimationEquation.OUT_EXPO;
			}
			float progress = anim.calc((navHoverTime - 300f) / 300f);
			navWidth += (int) (progress * navExpadedWidth);
		}

		g.setClip(0, 0, navWidth, height);
		g.setColor(COL_NAV_BG);
		g.fillRect(0, 0, navWidth, displayContainer.height);
		int y = navStartY;
		float iconSize = navButtonSize / 2.5f;
		float iconPadding = iconSize * 0.75f;
		int fontOffsetX = navButtonSize + navIndicatorSize;
		int fontOffsetY = (navButtonSize - Fonts.MEDIUM.getLineHeight()) / 2;
		for (OptionTab section : sections) {
			if (section.icon == null) {
				continue;
			}
			Color iconCol = COL_NAV_INACTIVE;
			Color fontCol = COL_NAV_WHITE;
			if (section == activeSection) {
				iconCol = COL_NAV_WHITE;
				g.fillRect(0, y, navWidth, navButtonSize);
				g.setColor(COL_NAV_INDICATOR);
				g.fillRect(navWidth - navIndicatorSize, y, navIndicatorSize, navButtonSize);
			} else if (section == hoveredNavigationEntry) {
				iconCol = COL_NAV_WHITE;
			}
			if (section.filtered) {
				iconCol = fontCol = COL_NAV_FILTERED;
				if (section == hoveredNavigationEntry) {
					iconCol = COL_NAV_FILTERED_HOVERED;
				}
			}
			section.icon.getImage().draw(iconPadding, y + iconPadding, iconSize, iconSize, iconCol);
			if (navHoverTime > 0) {
				Fonts.MEDIUM.drawString(fontOffsetX, y + fontOffsetY, section.name, fontCol);
			}
			y += navButtonSize;
		}

		g.clearClip();
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
		g.fillRect(navButtonSize, indicatorPos - scrollHandler.getPosition(), width, optionHeight);
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
			String tip = hoverOption.description;
			if (hoverOption instanceof NumericOption) {
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
			if (section.options == null) {
				float previousAlpha = COL_CYAN.a;
				if (section != activeSection) {
					COL_CYAN.a *= 0.2f;
				}
				FontUtil.drawRightAligned(Fonts.XLARGE, width, -paddingRight,
					(int) (y + Fonts.XLARGE.getLineHeight() * 0.3f), section.name.toUpperCase(),
					COL_CYAN);
				COL_CYAN.a = previousAlpha;
			} else {
				Fonts.MEDIUMBOLD.drawString(paddingTextLeft, lineStartY, section.name, COL_WHITE);
			}
			y += sectionLineHeight;
			maxScrollOffset += sectionLineHeight;
			if (section.options == null) {
				continue;
			}
			int lineHeight = (int) (Fonts.LARGE.getLineHeight() * 0.9f);
			for (int optionIndex = 0; optionIndex < section.options.length; optionIndex++) {
				Option option = section.options[optionIndex];
				if (!option.showCondition() || option.isFiltered()) {
					continue;
				}
				if (y > -optionHeight || (option instanceof ListOption && openDropdownMenu != null
						&& openDropdownMenu.equals(dropdownMenus.get(option)))) {
					renderOption(g, option, y);
				}
				y += optionHeight;
				maxScrollOffset += optionHeight;
				lineHeight += optionHeight;
				if (y > height) {
					render = false;
					while (++optionIndex < section.options.length) {
						option = section.options[optionIndex];
						if (option.showCondition() && !option.isFiltered()) {
							maxScrollOffset += optionHeight;
						}
					}
				}
			}
			g.setColor(COL_GREY);
			g.fillRect(paddingLeft, lineStartY, LINEWIDTH, lineHeight);
		}
		// iterate over skipped options to correctly calculate max scroll offset
		for (; sectionIndex < sections.length; sectionIndex++) {
			if (sections[sectionIndex].filtered) {
				continue;
			}
			maxScrollOffset += Fonts.LARGE.getLineHeight() * 1.5f;
			if (sections[sectionIndex].options == null) {
				continue;
			}
			for (Option option : sections[sectionIndex].options) {
				if (option.showCondition() && !option.isFiltered()) {
					maxScrollOffset += optionHeight;
				}
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

	private void renderOption(Graphics g, Option option, int y) {
		if (option instanceof ListOption) {
			renderListOption(g, (ListOption) option, y);
		} else if (option instanceof ToggleOption) {
			renderCheckOption((ToggleOption) option, y);
		} else if (option instanceof NumericOption) {
			renderSliderOption(g, (NumericOption) option, y);
		} else if (option instanceof GenericOption) {
			renderGenericOption((GenericOption) option, y);
		}
	}

	private void renderListOption(Graphics g, ListOption option, int y) {
		// draw option name
		int nameWith = Fonts.MEDIUM.getWidth(option.name);
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.name, COL_WHITE);
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

	private void renderCheckOption(ToggleOption option, int y) {
		if (option.state) {
			checkOnImg.draw(optionStartX, y + controlImagePadding, COL_PINK);
		} else {
			checkOffImg.draw(optionStartX, y + controlImagePadding, COL_PINK);
		}
		Fonts.MEDIUM.drawString(optionStartX + 30, y + optionTextOffsetY, option.name, COL_WHITE);
	}

	private void renderSliderOption(Graphics g, NumericOption option, int y) {
		final int padding = 10;
		int nameLen = Fonts.MEDIUM.getWidth(option.name);
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.name, COL_WHITE);
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

		float sliderValue = (float) (option.val - option.min) / (option.max - option.min);
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

	private void renderGenericOption(GenericOption option, int y) {
		String value = option.getValueString();
		int valueLen = Fonts.MEDIUM.getWidth(value);
		Fonts.MEDIUM.drawString(optionStartX, y + optionTextOffsetY, option.name, COL_WHITE);
		Fonts.MEDIUM.drawString(optionStartX + optionWidth - valueLen, y + optionTextOffsetY, value, COL_BLUE);
	}

	private void renderTitle() {
		int textWidth = width - navButtonSize;
		FontUtil.drawCentered(Fonts.LARGE, textWidth, navButtonSize,
			textOptionsY - scrollHandler.getIntPosition(), "Options", COL_WHITE);
		FontUtil.drawCentered(Fonts.MEDIUM, textWidth, navButtonSize,
			textChangeY - scrollHandler.getIntPosition(), "Change the way opsu! behaves", COL_PINK);
	}

	private void renderSearch(Graphics g) {
		int ypos = posSearchY + textSearchYOffset - scrollHandler.getIntPosition();
		if (scrollHandler.getIntPosition() > posSearchY) {
			ypos = textSearchYOffset;
			g.setColor(COL_BG);
			g.fillRect(navButtonSize, 0, width, textSearchYOffset * 2 + Fonts.LARGE.getLineHeight());
		}
		String searchText = "Type to search!";
		if (lastSearchText.length() > 0) {
			searchText = lastSearchText;
		}
		FontUtil.drawCentered(Fonts.LARGE, width, navButtonSize, ypos, searchText, COL_WHITE);
		int imgPosX = navButtonSize + (width - Fonts.LARGE.getWidth(searchText)) / 2 - searchImg.getWidth() - 10;
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
		navHoverTime = 0;
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

		int prevscrollpos = scrollHandler.getIntPosition();
		scrollHandler.update(delta);
		boolean scrollPositionChanged = prevscrollpos != scrollHandler.getIntPosition();

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

		if (mouseX < navWidth) {
			if (navHoverTime < 600) {
				navHoverTime += delta;
			}
		} else if (navHoverTime > 0) {
			navHoverTime -= delta;
		}

		if (!scrollPositionChanged && (mouseX - prevMouseX == 0 && mouseY - prevMouseY == 0)) {
			updateIndicatorAlpha();
			return;
		}
		updateActiveSection();
		updateHoverNavigation(mouseX, mouseY);
		prevMouseX = mouseX;
		prevMouseY = mouseY;
		updateHoverOption(mouseX, mouseY);
		updateIndicatorAlpha();
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
		if (isAdjustingSlider) {
			int sliderValue = ((NumericOption) hoverOption).val;
			updateSliderOption();
			if (((NumericOption) hoverOption).val - sliderValue != 0 && sliderSoundDelay <= 0) {
				sliderSoundDelay = 90;
				SoundController.playSound(SoundEffect.MENUHIT);
			}
		}
	}

	private void updateHoverNavigation(int mouseX, int mouseY) {
		hoveredNavigationEntry = null;
		if (mouseX >= navWidth) {
			return;
		}
		int y = navStartY;
		for (OptionTab section : sections) {
			if (section.options != null) {
				continue;
			}
			int nextY = y + navButtonSize;
			if (y <= mouseY && mouseY < nextY) {
				hoveredNavigationEntry = section;
			}
			y = nextY;
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
		// navigation elemenst fade out with a different animation
		float navProgress;
		if (acceptInput) {
			animationtime += delta;
			if (animationtime >= SHOWANIMATIONTIME) {
				animationtime = SHOWANIMATIONTIME;
			}
			progress = (float) animationtime / SHOWANIMATIONTIME;
			navProgress = Utils.clamp(progress * 10f, 0f, 1f);
			progress = AnimationEquation.OUT_EXPO.calc(progress);
		} else {
			animationtime -= delta;
			if (animationtime < 0) {
				animationtime = 0;
			}
			progress = (float) animationtime / hideAnimationTime;
			navProgress = hideAnimationStartProgress * AnimationEquation.IN_CIRC.calc(progress);
			progress = hideAnimationStartProgress * AnimationEquation.IN_EXPO.calc(progress);
		}
		width = navButtonSize + (int) (progress * (finalWidth - navButtonSize));
		COL_NAV_FILTERED.a = COL_NAV_INACTIVE.a = COL_NAV_FILTERED_HOVERED.a = COL_NAV_INDICATOR.a =
			COL_NAV_WHITE.a = COL_NAV_BG.a = navProgress;
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

		if (hoverOption != null && hoverOption instanceof NumericOption) {
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
			if (hoverOption instanceof ToggleOption) {
				((ToggleOption) hoverOption).toggle();
				if (listener != null) {
					listener.onSaveOption(hoverOption);
				}
				SoundController.playSound(SoundEffect.MENUHIT);
				return true;
			} else if (hoverOption == OPTION_KEY_LEFT) {
				keyEntryLeft = true;
			} else if (hoverOption == OPTION_KEY_RIGHT) {
				keyEntryLeft = true;
			}
		}

		if (hoveredNavigationEntry != null && !hoveredNavigationEntry.filtered) {
			int sectionPosition = 0;
			for (OptionTab section : sections) {
				if (section == hoveredNavigationEntry) {
					break;
				}
				if (section.filtered) {
					continue;
				}
				sectionPosition += sectionLineHeight;
				if (section.options == null) {
					continue;
				}
				for (Option option : section.options) {
					if (!option.isFiltered() && option.showCondition()) {
						sectionPosition += optionHeight;
					}
				}
			}
			scrollHandler.scrollToPosition(sectionPosition);
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
		return true;
	}

	@Override
	public boolean onKeyPressed(int key, char c) {
		if (keyEntryRight) {
			if (Utils.isValidGameKey(key)) {
				OPTION_KEY_RIGHT.intval = key;
			}
			keyEntryRight = false;
			return true;
		}

		if (keyEntryLeft) {
			if (Utils.isValidGameKey(key)) {
				OPTION_KEY_LEFT.intval = key;
			}
			keyEntryLeft = false;
			return true;
		}

		if (key == Keyboard.KEY_ESCAPE) {
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
		NumericOption o = (NumericOption) hoverOption;
		int value = o.min + Math.round((float) (o.max - o.min) * (displayContainer.mouseX - sliderOptionStartX) / (sliderOptionLength));
		o.setValue(Utils.clamp(value, o.min, o.max));
	}

	private void updateActiveSection() {
		// active section is the one that is visible in the top half of the screen
		activeSection = sections[0];
		int virtualY = optionStartY;
		for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
			OptionTab section = sections[sectionIndex];
			if (section.filtered) {
				continue;
			}
			virtualY += sectionLineHeight;
			if (virtualY > scrollHandler.getPosition() + height / 2) {
				return;
			}
			if (section.options == null) {
				activeSection = section;
				continue;
			}
			for (int optionIndex = 0; optionIndex < section.options.length; optionIndex++) {
				Option option = section.options[optionIndex];
				if (option.isFiltered() || !option.showCondition()) {
					continue;
				}
				virtualY += optionHeight;
			}
		}
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
			mouseVirtualY -= sectionLineHeight;
			if (section.options == null) {
				continue;
			}
			for (int optionIndex = 0; optionIndex < section.options.length; optionIndex++) {
				Option option = section.options[optionIndex];
				if (option.isFiltered() || !option.showCondition()) {
					continue;
				}
				if (mouseVirtualY <= optionHeight) {
					if (mouseX > navWidth && mouseVirtualY >= 0) {
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
			for (Option opt : section.options) {
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
				if (!lastBigSectionMatches) {
					section.filtered = true;
				}
				continue;
			}
			section.filtered = true;
			for (Option option : section.options) {
				if (lastBigSectionMatches || sectionMatches) {
					section.filtered = false;
					option.filter(null);
					continue;
				}
				if (!option.filter(lastSearchText)) {
					section.filtered = false;
					//noinspection ConstantConditions
					lastBigSection.filtered = false;
				}
			}
		}
		updateHoverOption(prevMouseX, prevMouseY);
	}

	public interface Listener {
		void onLeaveOptionsMenu();
		void onSaveOption(Option option);
	}

}
