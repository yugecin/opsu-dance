// Copyright 2016-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance;

import org.newdawn.slick.Color;

import itdelatrisu.opsu.ui.cursor.CursorImpl;

import static yugecin.opsudance.options.Options.*;

// TODO: code duplication with ObjectColorOverrides
public enum CursorColorOverrides
{
	NONE ("Do not override", 0) {
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
	RAINBOW ("Rainbow (time based)", 9) {
		@Override
		public Color getColor(boolean mirrored) {
			return new Color(java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f));
		}
	},
	RAINBOWSHIFT ("Rainbow (time based) + 180° hue shift", 10) {
		@Override
		public Color getColor(boolean mirrored) {
			float val = hue + .5f;
			val = val - (float) Math.floor(val);
			return new Color(java.awt.Color.HSBtoRGB(val, 1.0f, 1.0f));
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
				return CursorImpl.lastMirroredObjColor;
			}
			return CursorImpl.lastObjColor;
		}
	},
	NEXTOBJ ("Use next object's colors", 13) {
		@Override
		public Color getColor(boolean mirrored) {
			if (mirrored) {
				return CursorImpl.nextMirroredObjColor;
			}
			return CursorImpl.nextObjColor;
		}
	};

	private static float hue;

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
	 * required to advance the rainbow hue
	 */
	public static void update(int delta)
	{
		hue += OPTION_DANCE_RGB_CURSOR_INC.val / 360f / 1000f * delta;
		hue = hue - (float) Math.floor(hue);
	}

	public int nr;
	private String displayText;

	CursorColorOverrides(String displayText, int nr)
	{
		this.displayText = displayText;
		this.nr = nr;
	}

	@Override
	public String toString() {
		return displayText;
	}

	public Color getColor(boolean mirrored)
	{
		// default impl is based on combo colors
		if (comboColors == null || comboColors.length == 0) {
			return Color.white;
		}
		return comboColors[nr % comboColors.length];
	}

	public Color getColor()
	{
		return this.getColor(false);
	}

	public Color getMirrorColor()
	{
		return this.getColor(true);
	}
}
