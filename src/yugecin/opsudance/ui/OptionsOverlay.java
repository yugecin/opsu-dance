// Copyright 2016-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
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

import yugecin.opsudance.core.Constants;
import yugecin.opsudance.events.ResolutionChangedListener;
import yugecin.opsudance.events.SkinChangedListener;
import yugecin.opsudance.options.*;
import yugecin.opsudance.utils.FontUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import static itdelatrisu.opsu.GameImage.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

public class OptionsOverlay implements ResolutionChangedListener, SkinChangedListener
{
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

	private boolean active;
	private boolean acceptInput;
	private boolean dirty;

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

	private float showHideProgress;

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
	private int unchangedSliderValue;

	private final HashMap<ListOption, DropdownMenu<Object>> dropdownMenus;
	private final LinkedList<DropdownMenu<Object>> visibleDropdownMenus;
	private int dropdownMenuPaddingY;
	private DropdownMenu<Object> openDropdownMenu;
	private int openDropdownVirtualY;

	private int targetWidth;
	private int currentWidth;

	private int navButtonSize;
	private int navStartY;
	private int navTargetWidth;
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
	private int lastOptionHeight;

	private int mousePressY;
	
	private boolean isDraggingFromOutside;
	private boolean wasPressed;

	private boolean keyEntryLeft;
	private boolean keyEntryRight;

	private int prevMouseX;
	private int prevMouseY;

	private int sliderSoundDelay;

	private int sectionLineHeight;

	private final TextField searchField;
	private String lastSearchText;
	private int invalidSearchImgRotation;
	private int invalidSearchTextRotation;
	private int invalidSearchAnimationProgress;
	private final int INVALID_SEARCH_ANIMATION_TIME = 500;
	
	private final ArrayList<MyOptionListener> installedOptionListeners;

	public OptionsOverlay(OptionTab[] sections) {
		this.installedOptionListeners = new ArrayList<>();
		this.sections = sections;
		this.dirty = true;

		dropdownMenus = new HashMap<>();
		visibleDropdownMenus = new LinkedList<>();

		searchField = new TextField(null, 0, 0, 0, 0);
		searchField.setMaxLength(20);

		scrollHandler = new KineticScrolling();
		scrollHandler.setAllowOverScroll(true);
		
		displayContainer.addResolutionChangedListener(this);
		skinservice.addSkinChangedListener(this);
	}

	@Override
	public void onResolutionChanged(int w, int h) {
		this.dirty = true;
		if (this.active) {
			this.revalidate();
		}
	}

	@Override
	public void onSkinChanged(String name) {
		this.dirty = true;
		if (this.active) {
			this.revalidate();
		}
	}
	
	public boolean isActive() {
		return this.active;
	}
	
	public boolean containsMouse() {
		return this.active && mouseX <= this.currentWidth;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void revalidate() {
		this.dirty = false;

		targetWidth = (int) (width * (isWidescreen ? 0.4f : 0.5f));

		// calculate positions
		float navIconWidthRatio = isWidescreen ? 0.046875f : 0.065f;
		// non-widescreen ratio is not accurate
		navButtonSize = (int) (width * navIconWidthRatio);
		navIndicatorSize = navButtonSize / 10;
		navTargetWidth = (int) (targetWidth * 0.45f) - navButtonSize;
		paddingRight = (int) (width * 0.009375f); // not so accurate
		paddingLeft = navButtonSize + (int) (width * 0.0180f); // not so accurate
		paddingTextLeft = paddingLeft + LINEWIDTH + (int) (width * 0.00625f); // not so accurate
		optionStartX = paddingTextLeft;
		textOptionsY = Fonts.LARGE.getLineHeight() * 2;
		textChangeY = textOptionsY + Fonts.LARGE.getLineHeight();
		posSearchY = textChangeY + Fonts.MEDIUM.getLineHeight() * 2;
		textSearchYOffset = Fonts.MEDIUM.getLineHeight() / 2;
		optionStartY = posSearchY + Fonts.MEDIUM.getLineHeight() + Fonts.LARGE.getLineHeight();
		sectionLineHeight = (int) (Fonts.LARGE.getLineHeight() * 1.5f);

		if (active) {
			currentWidth = targetWidth;
			optionWidth = currentWidth - optionStartX - paddingRight;
		}

		optionHeight = (int) (Fonts.MEDIUM.getLineHeight() * 1.3f);
		optionTextOffsetY = (int) ((optionHeight - Fonts.MEDIUM.getLineHeight()) / 2f);
		controlImageSize = (int) (Fonts.MEDIUM.getLineHeight() * 0.7f);
		controlImagePadding = (optionHeight - controlImageSize) / 2;

		final int s = controlImageSize;
		sliderBallImg = CONTROL_SLIDER_BALL.getScaledImage(s, s);
		checkOnImg = CONTROL_CHECK_ON.getScaledImage(s, s);
		checkOffImg = CONTROL_CHECK_OFF.getScaledImage(s, s);

		for (MyOptionListener listener : this.installedOptionListeners) {
			listener.uninstall();
		}

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
				DropdownMenu<Object> menu = new DropdownMenu<Object>(items, 0, 0, 0) {
					@Override
					public void itemSelected(int index, Object item) {
						listOption.clickListItem(index);
						openDropdownMenu = null;
					}
				};
				final Runnable observer = () -> {
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
				};
				observer.run();
				listOption.addListener(observer);
				final MyOptionListener optionlistener;
				optionlistener = new MyOptionListener(listOption, observer);
				this.installedOptionListeners.add(optionlistener);

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

	public void render(Graphics g) {
		if (!this.active && this.currentWidth == this.navButtonSize) {
			return;
		}

		g.setClip(navButtonSize, 0, currentWidth - navButtonSize, height);

		// bg
		g.setColor(COL_BG);
		g.fillRect(navButtonSize, 0, currentWidth, height);

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
		g.fillRect(currentWidth - 5, scrollHandler.getPosition() / maxScrollOffset * (height - 45), 5, 45);
		g.clearClip();

		renderNavigation(g);

		// tooltip
		if (this.active) {
			renderTooltip(g);
		}

		// key input options
		if (keyEntryLeft || keyEntryRight) {
			renderKeyEntry(g);
		}
	}

	private void renderNavigation(Graphics g) {
		navWidth = navButtonSize;
		if (navHoverTime >= 600) {
			navWidth += navTargetWidth;
		} else if (navHoverTime > 300) {
			AnimationEquation anim = AnimationEquation.IN_EXPO;
			if (mouseX < navWidth) {
				anim = AnimationEquation.OUT_EXPO;
			}
			float progress = anim.calc((navHoverTime - 300f) / 300f);
			navWidth += (int) (progress * navTargetWidth);
		}

		g.setClip(0, 0, navWidth, height);
		g.setColor(COL_NAV_BG);
		g.fillRect(0, 0, navWidth, height);
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
			indicatorMoveAnimationTime += renderDelta;
			if (indicatorMoveAnimationTime > INDICATORMOVEANIMATIONTIME) {
				indicatorMoveAnimationTime = 0;
				indicatorPos += indicatorOffsetToNextPos;
				indicatorOffsetToNextPos = 0;
				this.indicatorPos = indicatorPos;
			} else {
				indicatorPos += AnimationEquation.OUT_BACK.calc((float) indicatorMoveAnimationTime / INDICATORMOVEANIMATIONTIME) * indicatorOffsetToNextPos;
			}
		}
		g.fillRect(navButtonSize, indicatorPos - scrollHandler.getPosition(), currentWidth, optionHeight);
	}

	private void renderKeyEntry(Graphics g) {
		g.setColor(COL_BG);
		g.fillRect(0, 0, width, height);
		g.setColor(COL_WHITE);
		String prompt = (keyEntryLeft) ? "Please press the new left-click key." : "Please press the new right-click key.";
		int y = height2 - Fonts.LARGE.getLineHeight() / 2;
		FontUtil.drawCentered(Fonts.LARGE, width, 0, y, prompt, COL_WHITE);
	}

	private void renderTooltip(Graphics g) {
		if (hoverOption != null) {
			String tip = hoverOption.description;
			if (hoverOption instanceof NumericOption) {
				tip = "(" + hoverOption.getValueString() + ") " + tip;
			}
			UI.updateTooltip(renderDelta, tip, true);
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
				FontUtil.drawRightAligned(Fonts.XLARGE, currentWidth, -paddingRight,
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
		lastOptionHeight = maxScrollOffset;
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
		int textWidth = currentWidth - navButtonSize;
		FontUtil.drawCentered(Fonts.LARGE, textWidth, navButtonSize,
			textOptionsY - scrollHandler.getIntPosition(), "Options", COL_WHITE);
		FontUtil.drawCentered(Fonts.MEDIUM, textWidth, navButtonSize,
			textChangeY - scrollHandler.getIntPosition(), "Change the way opsu! behaves", COL_PINK);

		int y = lastOptionHeight - scrollHandler.getIntPosition();
		y += Fonts.LARGE.getLineHeight() * 2.5f;
		FontUtil.drawCentered(Fonts.MEDIUM, textWidth, navButtonSize,
			y, Constants.PROJECT_NAME + " " + updater.getCurrentVersion(), COL_WHITE);
		y += Fonts.MEDIUM.getLineHeight() * 1.2f;
		FontUtil.drawCentered(Fonts.MEDIUM, textWidth, navButtonSize,
			y, Constants.DANCE_REPOSITORY_URI.toString(), COL_WHITE);
		y += Fonts.MEDIUM.getLineHeight() * 1.2f;
		FontUtil.drawCentered(Fonts.MEDIUM, textWidth, navButtonSize,
			y, Constants.REPOSITORY_URI.toString(), COL_WHITE);
	}

	private void renderSearch(Graphics g) {
		int ypos = posSearchY + textSearchYOffset - scrollHandler.getIntPosition();
		if (scrollHandler.getIntPosition() > posSearchY) {
			ypos = textSearchYOffset;
			g.setColor(COL_BG);
			g.fillRect(navButtonSize, 0, currentWidth, textSearchYOffset * 2 + Fonts.LARGE.getLineHeight());
		}
		Color searchCol = COL_WHITE;
		float invalidProgress = 0f;
		if (invalidSearchAnimationProgress > 0) {
			invalidProgress = 1f - (float) invalidSearchAnimationProgress / INVALID_SEARCH_ANIMATION_TIME;
			searchCol = new Color(0f, 0f, 0f, searchCol.a);
			searchCol.r = COL_PINK.r + (1f - COL_PINK.r) * invalidProgress;
			searchCol.g = COL_PINK.g + (1f - COL_PINK.g) * invalidProgress;
			searchCol.b = COL_PINK.b + (1f - COL_PINK.b) * invalidProgress;
			invalidProgress = 1f - invalidProgress;
		}
		String searchText = "Type to search!";
		if (lastSearchText.length() > 0) {
			searchText = lastSearchText;
		}
		int textWidth = currentWidth - navButtonSize;
		if (invalidSearchAnimationProgress > 0) {
			g.rotate(navButtonSize + textWidth / 2, ypos, invalidProgress * invalidSearchTextRotation);
		}
		FontUtil.drawCentered(Fonts.LARGE, textWidth, navButtonSize, ypos, searchText, searchCol);
		g.resetTransform();
		int imgPosX = navButtonSize + (textWidth - Fonts.LARGE.getWidth(searchText)) / 2 - searchImg.getWidth() - 10;
		if (invalidSearchAnimationProgress > 0) {
			g.rotate(imgPosX + searchImg.getWidth() / 2, ypos, invalidProgress * invalidSearchImgRotation);
		}
		searchImg.draw(imgPosX, ypos + Fonts.LARGE.getLineHeight() * 0.25f, searchCol);
		g.resetTransform();
	}

	public void hide() {
		if (!this.active) {
			return;
		}
		acceptInput = active = false;
		searchField.setFocused(false);
		hideAnimationTime = animationtime;
		hideAnimationStartProgress = (float) animationtime / SHOWANIMATIONTIME;
		hoverOption = null;
	}

	public void show() {
		navHoverTime = 0;
		indicatorPos = -optionHeight;
		indicatorOffsetToNextPos = 0;
		indicatorMoveAnimationTime = 0;
		indicatorHideAnimationTime = 0;
		acceptInput = active = true;
		animationtime = 0;
		resetSearch();
		if (this.dirty) {
			this.revalidate();
		}
	}

	public void preRenderUpdate() {
		if (!this.active && this.currentWidth == this.navButtonSize) {
			return;
		}

		int delta = renderDelta;

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

		if (invalidSearchAnimationProgress > 0) {
			invalidSearchAnimationProgress -= delta;
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
		navHoverTime = Utils.clamp(navHoverTime, 0, 600);

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
				indicatorHideAnimationTime += renderDelta;
				if (indicatorHideAnimationTime > INDICATORHIDEANIMATIONTIME) {
					indicatorHideAnimationTime = INDICATORHIDEANIMATIONTIME;
				}
				float progress = AnimationEquation.IN_CUBIC.calc((float) indicatorHideAnimationTime /
					INDICATORHIDEANIMATIONTIME);
				COL_INDICATOR.a = (1f - progress) * INDICATOR_ALPHA * showHideProgress;
			}
		} else if (indicatorHideAnimationTime > 0) {
			indicatorHideAnimationTime -= renderDelta * 3;
			if (indicatorHideAnimationTime < 0) {
				indicatorHideAnimationTime = 0;
			}
			COL_INDICATOR.a = (1f - (float) indicatorHideAnimationTime / INDICATORHIDEANIMATIONTIME) *
				INDICATOR_ALPHA * showHideProgress;
		}
	}

	private void updateShowHideAnimation(int delta) {
		if (acceptInput && animationtime >= SHOWANIMATIONTIME) {
			// animation already finished
			currentWidth = targetWidth;
			showHideProgress = 1f;
			return;
		}
		optionWidth = currentWidth - optionStartX - paddingRight;

		// navigation elemenst fade out with a different animation
		float navProgress;
		// if acceptInput is false, it means that we're currently hiding ourselves
		if (acceptInput) {
			animationtime += delta;
			if (animationtime >= SHOWANIMATIONTIME) {
				animationtime = SHOWANIMATIONTIME;
			}
			showHideProgress = (float) animationtime / SHOWANIMATIONTIME;
			navProgress = Utils.clamp(showHideProgress * 10f, 0f, 1f);
			showHideProgress = AnimationEquation.OUT_EXPO.calc(showHideProgress);
		} else {
			animationtime -= delta;
			if (animationtime < 0) {
				animationtime = 0;
			}
			showHideProgress = (float) animationtime / hideAnimationTime;
			navProgress = hideAnimationStartProgress * AnimationEquation.IN_CIRC.calc(showHideProgress);
			showHideProgress = hideAnimationStartProgress * AnimationEquation.IN_EXPO.calc(showHideProgress);
		}
		currentWidth = navButtonSize + (int) (showHideProgress * (targetWidth - navButtonSize));
		COL_NAV_FILTERED.a = COL_NAV_INACTIVE.a = COL_NAV_FILTERED_HOVERED.a = COL_NAV_INDICATOR.a =
			COL_NAV_WHITE.a = COL_NAV_BG.a = navProgress;
		COL_BG.a = BG_ALPHA * showHideProgress;
		COL_WHITE.a = showHideProgress;
		COL_PINK.a = showHideProgress;
		COL_CYAN.a = showHideProgress;
		COL_GREY.a = showHideProgress * LINEALPHA;
		COL_BLUE.a = showHideProgress;
		COL_COMBOBOX_HOVER.a = showHideProgress;
	}

	public boolean mousePressed(int button, int x, int y) {
		if (!this.active) {
			return false;
		}
		if (x > this.currentWidth) {
			this.isDraggingFromOutside = true;
			return false;
		}
		
		wasPressed = true;

		if (keyEntryLeft || keyEntryRight) {
			keyEntryLeft = keyEntryRight = false;
			return true;
		}

		if (x > currentWidth) {
			return false;
		}

		scrollHandler.pressed();

		mousePressY = y;
		selectedOption = hoverOption;

		if (hoverOption != null && hoverOption instanceof NumericOption) {
			isAdjustingSlider = sliderOptionStartX <= x && x < sliderOptionStartX + sliderOptionLength;
			if (isAdjustingSlider) {
				unchangedSliderValue = ((NumericOption) hoverOption).val;
				updateSliderOption();
			}
		}

		return true;
	}

	public boolean mouseReleased(int button, int x, int y) {
		this.isDraggingFromOutside = false;
		if (!this.active || (!wasPressed && x > this.currentWidth)) {
			return false;
		}
		
		wasPressed = false;

		selectedOption = null;
		if (isAdjustingSlider) {
			if (listener != null) {
				listener.onSaveOption(hoverOption);
			}
			updateHoverOption(x, y);
			isAdjustingSlider = false;
		}
		sliderOptionLength = 0;

		if (backButton.contains(x, y)){
			SoundController.playSound(SoundEffect.MENUBACK);
			hide();
			if (listener != null) {
				listener.onLeaveOptionsMenu();
			}
			return true;
		}

		if (x > navWidth) {
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
		}

		scrollHandler.released();

		// check if clicked, not dragged
		if (Math.abs(y - mousePressY) >= 5) {
			return true;
		}

		if (x > targetWidth) {
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
				keyEntryRight = true;
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
			sectionPosition = Utils.clamp(sectionPosition, (int) scrollHandler.min, (int) scrollHandler.max);
			scrollHandler.scrollToPosition(sectionPosition);
		}
		return true;
	}

	public boolean mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (!this.active || this.isDraggingFromOutside) {
			return false;
		}

		if (!isAdjustingSlider) {
			int diff = newy - oldy;
			if (diff != 0) {
				scrollHandler.dragged(-diff);
			}
		}
		return true;
	}

	public boolean mouseWheelMoved(int delta) {
		if (!this.active || mouseX > this.currentWidth) {
			return false;
		}

		if (!isAdjustingSlider) {
			scrollHandler.scrollOffset(-delta);
		}
		return true;
	}

	public boolean keyPressed(int key, char c) {
		if (!this.active) {
			return false;
		}

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
			if (isAdjustingSlider) {
				cancelAdjustingSlider();
			}
			if (openDropdownMenu != null) {
				openDropdownMenu.keyPressed(key, c);
				return true;
			}
			if (lastSearchText.length() != 0) {
				resetSearch();
				updateHoverOption(prevMouseX, prevMouseY);
				return true;
			}
			SoundController.playSound(SoundEffect.MENUBACK);
			hide();
			if (listener != null) {
				listener.onLeaveOptionsMenu();
			}
			return true;
		}

		searchField.keyPressed(key, c);
		if (!searchField.getText().equals(lastSearchText)) {
			String newSearchText = searchField.getText().toLowerCase();
			if (!hasSearchResults(newSearchText)) {
				searchField.setText(lastSearchText);
				invalidSearchAnimationProgress = INVALID_SEARCH_ANIMATION_TIME;
				Random rand = new Random();
				invalidSearchImgRotation = 10 + rand.nextInt(10);
				invalidSearchTextRotation = 10 + rand.nextInt(10);
				if (rand.nextBoolean()) {
					invalidSearchImgRotation = -invalidSearchImgRotation;
				}
				if (rand.nextBoolean()) {
					invalidSearchTextRotation = -invalidSearchTextRotation;
				}
			} else {
				lastSearchText = newSearchText;
				updateSearch();
			}
		}

		return true;
	}

	public boolean keyReleased(int key, char c) {
		return false;
	}

	private void cancelAdjustingSlider() {
		if (isAdjustingSlider) {
			isAdjustingSlider = false;
			((NumericOption) hoverOption).setValue(unchangedSliderValue);
		}
	}

	private void updateSliderOption() {
		NumericOption o = (NumericOption) hoverOption;
		int value = o.min + Math.round((float) (o.max - o.min) * (mouseX - sliderOptionStartX) / (sliderOptionLength));
		o.setValue(Utils.clamp(value, o.min, o.max));
	}

	private void updateActiveSection() {
		// active section is the one that is visible in the top half of the screen
		activeSection = sections[0];
		int virtualY = optionStartY;
		for (OptionTab section : sections) {
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
		if (mouseX < navWidth) {
			cancelAdjustingSlider();
			hoverOption = null;
			return;
		}
		if (openDropdownMenu != null || keyEntryLeft || keyEntryRight) {
			return;
		}
		if (selectedOption != null) {
			hoverOption = selectedOption;
			return;
		}
		hoverOption = null;
		if (mouseX > currentWidth) {
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
				section.filtered = !lastBigSectionMatches;
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
		updateActiveSection();
		if (openDropdownMenu != null) {
			openDropdownMenu.reset();
			openDropdownMenu = null;
		}
	}

	private boolean hasSearchResults(String searchText) {
		for (OptionTab section : sections) {
			if (section.name.toLowerCase().contains(searchText)) {
				return true;
			}
			if (section.options == null) {
				continue;
			}
			for (Option option : section.options) {
				boolean wasFiltered = option.isFiltered();
				boolean isFiltered = option.filter(searchText);
				option.setFiltered(wasFiltered);
				if (!isFiltered) {
					return true;
				}
			}
		}
		return false;
	}

	public interface Listener {
		void onLeaveOptionsMenu();
		void onSaveOption(Option option);
	}
	
	private static class MyOptionListener
	{
		private ListOption option;
		private Runnable listener;
		
		public MyOptionListener(ListOption option, Runnable listener)
		{
			this.option = option;
			this.listener = listener;
		}
		
		public void uninstall()
		{
			this.option.removeListener(this.listener);
		}
	}
}
