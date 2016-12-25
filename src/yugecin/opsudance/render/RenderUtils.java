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
package yugecin.opsudance.render;

import org.newdawn.slick.Graphics;

public class RenderUtils {

	public static void drawDottedLine(Graphics g, float fromX, float fromY, float toX, float toY, int dotAmount, int dotRadius) {
		float dx = (toX - fromX) / (dotAmount - 1);
		float dy = (toY - fromY) / (dotAmount - 1);
		for (; dotAmount > 0; dotAmount--) {
			fillCenteredRect(g, fromX, fromY, dotRadius);
			fromX += dx;
			fromY += dy;
		}
	}

	public static void drawDottedLine(Graphics g, int fromX, int fromY, int toX, int toY, int dotAmount, int dotRadius) {
		float dx = (float) (toX - fromX) / (dotAmount - 1);
		float dy = (float) (toY - fromY) / (dotAmount - 1);
		for (; dotAmount > 0; dotAmount--) {
			fillCenteredRect(g, fromX, fromY, dotRadius);
			fromX += dx;
			fromY += dy;
		}
	}

	public static void fillCenteredRect(Graphics g, int x, int y, int radius) {
		int totalLength = radius * 2 + 1;
		g.fillRect(x - radius, y - radius, totalLength, totalLength);
	}

	public static void fillCenteredRect(Graphics g, float x, float y, float radius) {
		float totalLength = radius * 2f + 1f;
		g.fillRect(x - radius, y - radius, totalLength, totalLength);
	}

}
