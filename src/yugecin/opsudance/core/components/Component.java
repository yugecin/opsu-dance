// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.components;

import org.newdawn.slick.Graphics;

import yugecin.opsudance.core.input.*;

public abstract class Component implements InputListener
{
	public int width;
	public int height;
	public int x;
	public int y;

	protected boolean focused;
	protected boolean hovered;

	public abstract boolean isFocusable();

	public boolean isHovered() {
		return hovered;
	}

	public void updateHover(int x, int y) {
		this.hovered = this.x <= x && x <= this.x + width && this.y <= y && y <= this.y + height;
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void preRenderUpdate() {
	}

	public abstract void render(Graphics g);

	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	public boolean isFocused()
	{
		return this.focused;
	}
}
