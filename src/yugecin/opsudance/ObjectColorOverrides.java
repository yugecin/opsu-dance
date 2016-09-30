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

import org.newdawn.slick.Color;

public enum ObjectColorOverrides {

	NONE ("None", 0) {
		@Override
		public Color getColor(int comboColorIndex) {
			return comboColors[comboColorIndex];
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
	OPPOSITECOMBOCOLOR ("Opposite combo color", 9) {
		@Override
		public Color getColor(int comboColorIndex) {
			return comboColors[(comboColorIndex + comboColors.length / 2) % comboColors.length];
		}
	},
	RAINBOW ("Rainbow", 10) {
		@Override
		public Color getColor(int comboColorIndex) {
			return nextRainbowColor();
		}
	},
	RAINBOWSHIFT ("Rainbow + 180° hue shift", 11) {
		@Override
		public Color getColor(int comboColorIndex) {
			return nextMirrorRainbowColor();
		}
	};

	public int nr;
	private String displayText;

	public static Color[] comboColors;

	public static float hue;

	ObjectColorOverrides(String displayText, int nr) {
		this.displayText = displayText;
		this.nr = nr;
	}

	@Override
	public String toString() {
		return displayText;
	}

	public Color getColor(int comboColorIndex) {
		return comboColors[nr % comboColors.length];
	}

	private static Color nextRainbowColor() {
		hue += Dancer.rgbhueinc / 10f;
		return new Color(java.awt.Color.getHSBColor(hue / 360f, 1.0f, 1.0f).getRGB());
	}

	private static Color nextMirrorRainbowColor() {
		return new Color(java.awt.Color.getHSBColor((hue + 180f) / 360f, 1.0f, 1.0f).getRGB());
	}


}

