/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
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
package yugecin.opsudance.utils;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;

public class FontUtil {

	public static void drawCentered(Font font, int width, int xoffset, int y, String text, Color color) {
		int x = (width - font.getWidth(text)) / 2;
		font.drawString(x + xoffset, y, text, color);
	}

	public static void drawRightAligned(Font font, int width, int xoffset, int y, String text, Color color) {
		int x = width - font.getWidth(text);
		font.drawString(x + xoffset, y, text, color);
	}

}
