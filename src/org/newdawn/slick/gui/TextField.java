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
import org.newdawn.slick.Input;
import org.newdawn.slick.geom.Rectangle;
import yugecin.opsudance.core.components.ActionListener;
import yugecin.opsudance.core.components.Component;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * A single text field supporting text entry
 * 
 * @author kevin
 */
public class TextField extends Component {

	private static final int INITIAL_KEY_REPEAT_INTERVAL = 400;
	private static final int KEY_REPEAT_INTERVAL = 50;

	private String value = "";
	private Font font;
	private int maxCharacter = 10000;

	private Color borderCol = Color.white;
	private Color textCol = Color.white;
	private Color backgroundCol = new Color(0, 0, 0, 0.5f);

	private int cursorPos;
	private int lastKey = -1;
	private char lastChar = 0;
	private long repeatTimer;

	private ActionListener listener;

	public TextField(Font font, int x, int y, int width, int height) {
		this.font = font;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setListener(ActionListener listener) {
		this.listener = listener;
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

	public void render(Graphics g) {
		if (lastKey != -1) {
			if (input.isKeyDown(lastKey)) {
				if (repeatTimer < System.currentTimeMillis()) {
					repeatTimer = System.currentTimeMillis() + KEY_REPEAT_INTERVAL;
					keyPressed(lastKey, lastChar);
				}
			} else {
				lastKey = -1;
			}
		}
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

		int cpos = font.getWidth(value.substring(0, cursorPos));
		int tx = 0;
		if (cpos > width) {
			tx = width - cpos - font.getWidth("_");
		}

		g.translate(tx + 2, 0);
		g.setFont(font);
		g.drawString(value, x + 1, y + 1);

		if (focused) {
			g.drawString("|", x + 1 + cpos + 2, y + 1);
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
		if (cursorPos > value.length()) {
			cursorPos = value.length();
		}
	}

	public void setMaxLength(int length) {
		maxCharacter = length;
		if (value.length() > maxCharacter) {
			value = value.substring(0, maxCharacter);
		}
	}

	protected void doPaste(String text) {
		for (int i=0;i<text.length();i++) {
			keyPressed(-1, text.charAt(i));
		}
	}

	public void keyPressed(int key, char c) {
		if (key != -1)
		{
			if ((key == Input.KEY_V) &&
			   ((input.isKeyDown(Input.KEY_LCONTROL)) || (input.isKeyDown(Input.KEY_RCONTROL)))) {
				String text = Sys.getClipboard();
				if (text != null) {
					doPaste(text);
				}
				return;
			}
		}

		if (lastKey != key) {
			lastKey = key;
			repeatTimer = System.currentTimeMillis() + INITIAL_KEY_REPEAT_INTERVAL;
		} else {
			repeatTimer = System.currentTimeMillis() + KEY_REPEAT_INTERVAL;
		}
		lastChar = c;

		if (key == Input.KEY_LEFT) { /*
			if (cursorPos > 0) {
				cursorPos--;
			}
			// Nobody more will be notified
			if (consume) {
				container.getInput().consumeEvent();
			}
		*/ } else if (key == Input.KEY_RIGHT) { /*
			if (cursorPos < value.length()) {
				cursorPos++;
			}
			// Nobody more will be notified
			if (consume) {
				container.getInput().consumeEvent();
			}
		*/ } else if (key == Input.KEY_BACK) {
			if ((cursorPos > 0) && (value.length() > 0)) {
				if (input.isKeyDown(Input.KEY_LCONTROL) || input.isKeyDown(Input.KEY_RCONTROL)) {
					int sp = 0;
					boolean startSpace = Character.isWhitespace(value.charAt(cursorPos - 1));
					boolean charSeen = false;
					for (int i = cursorPos - 1; i >= 0; i--) {
						boolean isSpace = Character.isWhitespace(value.charAt(i));
						if (!startSpace && isSpace) {
							sp = i;
							break;
						} else if (startSpace) {
							if (charSeen && isSpace) {
								sp = i + 1;
								break;
							} else if (!charSeen && !isSpace)
								charSeen = true;
						}
					}
					if (cursorPos < value.length())
						value = value.substring(0, sp) + value.substring(cursorPos);
					else
						value = value.substring(0, sp);
					cursorPos = sp;
				} else {
					if (cursorPos < value.length()) {
						value = value.substring(0, cursorPos - 1)
								+ value.substring(cursorPos);
					} else {
						value = value.substring(0, cursorPos - 1);
					}
					cursorPos--;
				}
			}
		} else if (key == Input.KEY_DELETE) {
			if (value.length() > cursorPos) {
				value = value.substring(0,cursorPos) + value.substring(cursorPos+1);
			}
		} else if ((c < 127) && (c > 31) && (value.length() < maxCharacter)) {
			if (cursorPos < value.length()) {
				value = value.substring(0, cursorPos) + c
						+ value.substring(cursorPos);
			} else {
				value = value.substring(0, cursorPos) + c;
			}
			cursorPos++;
		} else if (key == Input.KEY_RETURN) {
			if (listener != null) {
				listener.onAction();
			}
		}

	}

}
