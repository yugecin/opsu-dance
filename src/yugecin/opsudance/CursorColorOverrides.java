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
package yugecin.opsudance;

import itdelatrisu.opsu.ui.Cursor;
import org.newdawn.slick.Color;

public enum CursorColorOverrides {

	NONE ("None", 0) {
		@Override
		public Color getColor(boolean mirrored) {
			return Color.white;
		}
	},
	COMBO1 ("Combo1", 1),
	COMBO2 ("Combo2", 2),
	COMBO3 ("Combo3", 3),
	COMBO4 ("Combo4", 4),
	COMBO5 ("Combo5", 5),
	COMBO6 ("Combo6", 6),
	COMBO7 ("Combo7", 7),
	COMBO8 ("Combo8", 8),
	RAINBOW ("Rainbow", 9) {
		@Override
		public Color getColor(boolean mirrored) {
			return nextRainbowColor();
		}
	},
	RAINBOWSHIFT ("Rainbow + 180° hue shift", 10) {
		@Override
		public Color getColor(boolean mirrored) {
			return nextMirrorRainbowColor();
		}
	},
	BLACK ("Black", 11) {
		@Override
		public Color getColor(boolean mirrored) {
			return Color.black;
		}
	},
	LASTOBJ ("Use last object's colors", 12) {
		@Override
		public Color getColor(boolean mirrored) {
			if (mirrored) {
				return Cursor.lastMirroredObjColor;
			}
			return Cursor.lastObjColor;
		}
	};

	public int nr;
	private String displayText;

	public static Color[] comboColors;

	public static float hue;

	CursorColorOverrides(String displayText, int nr) {
		this.displayText = displayText;
		this.nr = nr;
	}

	public static void reset(int mapID) {
		hue = mapID % 360;
	}

	@Override
	public String toString() {
		return displayText;
	}

	public Color getColor(boolean mirrored) {
		if (comboColors == null || comboColors.length == 0) {
			return Color.white;
		}
		return comboColors[nr % comboColors.length];
	}

	public Color getColor() {
		return getColor(false);
	}

	public Color getMirrorColor() {
		return getColor(true);
	}

	private static Color nextRainbowColor() {
		hue += Dancer.rgbcursorhueinc / 1000f;
		return new Color(java.awt.Color.getHSBColor(hue / 360f, 1.0f, 1.0f).getRGB());
	}

	private static Color nextMirrorRainbowColor() {
		hue += Dancer.rgbcursorhueinc / 1000f;
		return new Color(java.awt.Color.getHSBColor((hue + 180f) / 360f, 1.0f, 1.0f).getRGB());
	}

}
