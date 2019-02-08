/*
 * Copyright (c) 2013, Slick2D
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Slick2D nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.newdawn.slick.gui;

import org.lwjgl.Sys;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.geom.Rectangle;
import yugecin.opsudance.core.components.Component;
import yugecin.opsudance.core.input.*;

import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * based on {@link org.newdawn.slick.gui.TextField}
 */
public class TextField extends Component
{
	private String value = "";
	public final UnicodeFont font;
	private int maxCharacters = 10000;

	private Color borderCol = Color.white;
	private Color textCol = Color.white;
	private Color backgroundCol = new Color(0, 0, 0, 0.5f);

	public TextField(UnicodeFont font, int x, int y, int width, int height) {
		this.font = font;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setBorderColor(Color border) {
		this.borderCol = border;
	}

	public void setTextColor(Color text) {
		this.textCol = text;
	}

	public void setBackgroundColor(Color background) {
		this.backgroundCol = background;
	}

	@Override
	public boolean isFocusable() {
		return true;
	}

	public void render(Graphics g)
	{
		Rectangle oldClip = g.getClip();
		g.setWorldClip(x,y,width, height);
		
		// Someone could have set a color for me to blend...
		Color clr = g.getColor();

		if (backgroundCol != null) {
			g.setColor(backgroundCol.multiply(clr));
			g.fillRect(x, y, width, height);
		}
		g.setColor(textCol.multiply(clr));
		Font temp = g.getFont();

		int cursorpos = font.getWidth(value);
		int tx = 0;
		if (cursorpos > width) {
			tx = width - cursorpos - font.getWidth("_");
		}

		g.translate(tx + 2, 0);
		g.setFont(font);
		g.drawString(value, x + 1, y + 1);

		if (focused) {
			g.drawString("|", x + cursorpos, y + 1);
		}

		g.translate(-tx - 2, 0);

		if (borderCol != null) {
			g.setColor(borderCol.multiply(clr));
			g.drawRect(x, y, width, height);
		}
		g.setColor(clr);
		g.setFont(temp);
		g.clearWorldClip();
		g.setClip(oldClip);
	}

	public String getText() {
		return value;
	}

	public void setText(String value) {
		this.value = value;
	}

	public void setMaxLength(int length) {
		maxCharacters = length;
		if (value.length() > maxCharacters) {
			value = value.substring(0, maxCharacters);
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		e.consume();

		if (e.keyCode == KEY_V && input.isControlDown()) {
			String text = Sys.getClipboard();
			if (text != null) {
				value += text;
				this.setMaxLength(this.maxCharacters);
			}
			return;
		}

		switch (e.keyCode) {
		case KEY_BACK:
			final int len = value.length();
			if (len == 0) {
				break;
			}
			if (!input.isControlDown() || len == 1) {
				value = value.substring(0, len - 1);
				return;
			}
			int lastindex = len - 2;
			while (true) {
				if (Character.isWhitespace(value.charAt(lastindex))) {
					while (lastindex > 0 &&
						Character.isWhitespace(value.charAt(lastindex - 1)))
					{
						lastindex--;
					}
					value = value.substring(0, lastindex);
					break;
				}
				if (--lastindex <= 0) {
					value = "";
					break;
				}
			}
			break;
		default:
			if (e.chr > 31 && value.length() < maxCharacters) {
				value += e.chr;
			}
		}
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
	public void keyReleased(KeyEvent e)
	{
	}
}
