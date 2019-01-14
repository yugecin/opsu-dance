// Copyright 2016-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance;

import org.newdawn.slick.Color;

import static yugecin.opsudance.options.Options.*;

// TODO: code duplication with CursorColorOverrides
public enum ObjectColorOverrides
{
	NONE ("Do not override", 0) {
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
			int val = (comboColorIndex + comboColors.length / 2) % comboColors.length;
			return comboColors[val];
		}
	},
	RAINBOW ("Rainbow", 10) {
		@Override
		public Color getColor(int comboColorIndex) {
			return new Color(java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f));
		}
	},
	RAINBOWSHIFT ("Rainbow + 180° hue shift", 11) {
		@Override
		public Color getColor(int comboColorIndex) {
			float val = hue + .5f;
			val = val - (float) Math.floor(val);
			return new Color(java.awt.Color.HSBtoRGB(val, 1.0f, 1.0f));
		}
	},
	BLACK ("Black", 12) {
		@Override
		public Color getColor(int comboColorIndex) {
			return Color.black;
		}
	},
	WHITE ("White", 13) {
		@Override
		public Color getColor(int comboColorIndex) {
			return white;
		}
	};

	public static float hue;

	public static Color[] comboColors;

	/**
	 * Sets hue value for rainbow cursor to a value based on the given seed
	 * to make the rainbow color somewhat deterministic.
	 */
	public static void resetRainbowHue(int seed)
	{
		hue = (seed % 360) / 360f;
	}

	/**
	 * since hue is only incremented per object (not time), this should be called everytime
	 * a new object is created
	 */
	public static void updateRainbowHue()
	{
		hue += OPTION_DANCE_RGB_OBJECT_INC.val / 100f / 360f;
		hue = hue - (float) Math.floor(hue);
	}

	private static Color white = new Color(255, 255, 255);

	public int nr;
	private String displayText;

	ObjectColorOverrides(String displayText, int nr)
	{
		this.displayText = displayText;
		this.nr = nr;
	}

	@Override
	public String toString()
	{
		return displayText;
	}

	public Color getColor(int comboColorIndex)
	{
		// default impl is based on combo colors
		return comboColors[nr % comboColors.length];
	}
}

