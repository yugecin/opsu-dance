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


import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;

import java.awt.Rectangle;

public class SimpleButton {

	private final Color bg;
	private final Color fg;
	private final Color border;
	private final Color hoverBorder;
	private final Font font;
	private String text;
	private int textY;
	private Rectangle hitbox;
	private boolean isHovered;

	public SimpleButton(int x, int y, int width, int height, Font font, String text, Color bg, Color fg, Color border, Color hoverBorder) {
		this.bg = bg;
		this.fg = fg;
		this.border = border;
		this.hoverBorder = hoverBorder;
		this.hitbox = new Rectangle(x, y, width, height);
		this.font = font;
		this.text = text;
		this.textY = y + (height - font.getLineHeight()) / 2;
	}

	public void render(Graphics g) {
		g.setLineWidth(2f);
		g.setColor(bg);
		g.fillRect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
		g.setColor(fg);
		font.drawString(hitbox.x + 5, textY, text);
		if (isHovered) {
			g.setColor(hoverBorder);
		} else {
			g.setColor(border);
		}
		g.drawRect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
	}

	public void update(int x, int y) {
		isHovered = hitbox.contains(x, y);
	}

	public boolean isHovered() {
		return isHovered;
	}

}
