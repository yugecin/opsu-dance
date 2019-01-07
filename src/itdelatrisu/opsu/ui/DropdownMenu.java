/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.ui;

import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.UnicodeFont;
import yugecin.opsudance.core.components.Component;
import yugecin.opsudance.core.input.*;

import static itdelatrisu.opsu.GameImage.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class DropdownMenu<E> extends Component
{
	private static final float PADDING_Y = 0.1f, CHEVRON_X = 0.03f;

	private E[] items;
	private String[] itemNames;
	private int selectedItemIndex;
	private boolean expanded;

	private AnimatedValue expandProgress = new AnimatedValue(300, 0f, 1f, AnimationEquation.LINEAR);

	private int baseHeight;
	private float offsetY;

	private Color textColor = Color.white;
	private Color backgroundColor = Color.black;
	private Color highlightColor = Colors.BLUE_DIVIDER;
	private Color borderColor = Colors.BLUE_DIVIDER;
	private Color chevronDownColor = textColor;
	private Color chevronRightColor = backgroundColor;

	private UnicodeFont fontNormal = Fonts.MEDIUM;
	private UnicodeFont fontSelected = Fonts.MEDIUMBOLD;

	private Image chevronDown;
	private Image chevronRight;

	public DropdownMenu(E[] items, int x, int y, int width) {
		init(items, x, y, width);
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		if (expanded) {
			return height;
		}
		return baseHeight;
	}

	public boolean isOpen() {
		return expanded;
	}

	/**
	 * Selects the item at the given index.
	 * @param index the list item index
	 * @throws IllegalArgumentException if {@code index} is negative or greater than or equal to size
	 */
	public void setSelectedIndex(int index) {
		if (index < 0 || items.length <= index) {
			throw new IllegalArgumentException();
		}
		this.selectedItemIndex = index;
	}

	private int getMaxItemWidth() {
		int maxWidth = 0;
		for (String itemName : itemNames) {
			int w = fontSelected.getWidth(itemName);
			if (w > maxWidth) {
				maxWidth = w;
			}
		}
		return maxWidth;
	}

	private void init(E[] items, int x, int y, int width) {
		this.items = items;
		this.itemNames = new String[items.length];
		for (int i = 0; i < itemNames.length; i++) {
			itemNames[i] = items[i].toString();
		}
		this.x = x;
		this.y = y;
		this.baseHeight = fontNormal.getLineHeight();
		this.offsetY = baseHeight + baseHeight * PADDING_Y;
		this.height = (int) (offsetY * (items.length + 1));
		int downChevronSize = baseHeight * 4 / 5;
		this.chevronDown = CHEVRON_DOWN.getScaledImage(downChevronSize, downChevronSize);
		int rightChevronSize = baseHeight * 2 / 3;
		//noinspection SuspiciousNameCombination
		this.chevronRight = CHEVRON_RIGHT.getScaledImage(rightChevronSize, rightChevronSize);
		int maxItemWidth = getMaxItemWidth();
		int minWidth = maxItemWidth + chevronRight.getWidth() * 2;
		this.width = Math.max(width, minWidth);
	}

	@Override
	public void updateHover(int x, int y)
	{
		if (displayContainer.suppressHover) {
			this.hovered = false;
			return;
		}
		displayContainer.suppressHover = this.hovered =
			this.x <= x && x <= this.x + width && this.y <= y && y <= this.y +
			(expanded ? height : baseHeight);
	}

	public boolean baseContains(int x, int y)
	{
		return (x > this.x && x < this.x + width && y > this.y && y < this.y + baseHeight);
	}

	public boolean isClosing()
	{
		return !expanded && expandProgress.getValue() >= 0.0001f;
	}

	@Override
	public void render(Graphics g) {
		int delta = renderDelta;

		// update animation
		expandProgress.update((expanded) ? delta : -delta * 2);

		// get parameters
		int idx = getIndexAt(mouseY);
		float t = expandProgress.getValue();
		if (expanded) {
			t = AnimationEquation.OUT_CUBIC.calc(t);
		}

		// background and border
		Color oldGColor = g.getColor();
		float oldLineWidth = g.getLineWidth();
		final int cornerRadius = 6;
		g.setLineWidth(1f);
		g.setColor((idx == -1) ? highlightColor : backgroundColor);
		g.fillRoundRect(x, y, width, baseHeight, cornerRadius);
		g.setColor(borderColor);
		g.drawRoundRect(x, y, width, baseHeight, cornerRadius);
		if (expanded || t >= 0.0001) {
			float oldBackgroundAlpha = backgroundColor.a;
			backgroundColor.a *= t;
			g.setColor(backgroundColor);
			g.fillRoundRect(x, y + offsetY, width, (height - offsetY) * t, cornerRadius);
			backgroundColor.a = oldBackgroundAlpha;
		}
		if (idx >= 0 && t >= 0.9999) {
			g.setColor(highlightColor);
			float yPos = y + offsetY + (offsetY * idx);
			int yOff = 0, hOff = 0;
			if (idx == 0 || idx == items.length - 1) {
				g.fillRoundRect(x, yPos, width, offsetY, cornerRadius);
				if (idx == 0)
					yOff = cornerRadius;
				hOff = cornerRadius;
			}
			g.fillRect(x, yPos + yOff, width, offsetY - hOff);
		}
		g.setColor(oldGColor);
		g.setLineWidth(oldLineWidth);

		// text
		chevronDown.draw(x + width - chevronDown.getWidth() - width * CHEVRON_X, y + (baseHeight - chevronDown.getHeight()) / 2f, chevronDownColor);
		fontNormal.drawString(x + (width * 0.03f), y + (fontNormal.getPaddingTop() + fontNormal.getPaddingBottom()) / 2f, itemNames[selectedItemIndex], textColor);
		float oldTextAlpha = textColor.a;
		textColor.a *= t;
		if (expanded || t >= 0.0001) {
			for (int i = 0; i < itemNames.length; i++) {
				Font f = (i == selectedItemIndex) ? fontSelected : fontNormal;
				if (i == idx && t >= 0.999)
					chevronRight.draw(x, y + offsetY + (offsetY * i) + (offsetY - chevronRight.getHeight()) / 2f, chevronRightColor);
				f.drawString(x + chevronRight.getWidth(), y + offsetY + (offsetY * i * t), itemNames[i], textColor);
			}
		}
		textColor.a = oldTextAlpha;
	}

	/**
	 * Returns the index of the item at the given location, -1 for the base item,
	 * and -2 if there is no item at the location.
	 * @param y the y coordinate
	 */
	private int getIndexAt(int y) {
		if (!hovered) {
			return -2;
		}
		if (y <= this.y + baseHeight) {
			return -1;
		}
		if (!expanded) {
			return -2;
		}
		return (int) ((y - (this.y + offsetY)) / offsetY);
	}

	public void reset() {
		this.expanded = false;
		expandProgress.setTime(0);
	}

	public void openGrabFocus()
	{
		this.setFocused(true);
		input.addListener(this);
	}

	public void closeReleaseFocus()
	{
		if (this.focused) {
			this.setFocused(false);
			input.removeListener(this);
		}
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		expanded = focused;
	}

	@Override
	public boolean isFocusable() {
		return true;
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (e.button == Input.MMB || e.dragDistance > 5f) {
			return;
		}

		int idx = getIndexAt(mouseY);
		if (idx < 0) {
			// -1: base clicked
			// -2: somewhere else clicked
			if (idx == -1) {
				e.consume();
			}
			this.closeReleaseFocus();
			return;
		}
		e.consume();
		if (!canSelect(selectedItemIndex)) {
			return;
		}
		this.expanded = (idx == -1) && !expanded;
		if (0 <= idx && idx < items.length && selectedItemIndex != idx) {
			this.selectedItemIndex = idx;
			itemSelected(idx, items[selectedItemIndex]);
			if (!this.expanded) {
				e.consume();
			}
		}
		this.closeReleaseFocus();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.keyCode == Keyboard.KEY_ESCAPE) {
			this.closeReleaseFocus();
			e.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	protected boolean canSelect(int index) {
		return true;
	}

	protected void itemSelected(int index, E item) {
	}

	public E getSelectedItem() {
		return items[selectedItemIndex];
	}

	public void setBackgroundColor(Color c) {
		this.backgroundColor = c;
	}

	public void setHighlightColor(Color c) {
		this.highlightColor = c;
	}

	public void setBorderColor(Color c) {
		this.borderColor = c;
	}

	public void setChevronDownColor(Color c) {
		this.chevronDownColor = c;
	}

	public void setChevronRightColor(Color c) {
		this.chevronRightColor = c;
	}

	public void setTextColor(Color c) {
		this.textColor = c;
	}
}
