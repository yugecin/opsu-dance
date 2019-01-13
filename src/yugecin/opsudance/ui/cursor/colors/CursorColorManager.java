// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor.colors;

import org.newdawn.slick.Color;

import static yugecin.opsudance.core.InstanceContainer.cursorColor;

// ...Manager, *shrieks*
public class CursorColorManager
{
	public static final CursorColor[] impls;

	static Color[] comboCols;
	static int[] comboColors;

	static
	{
		impls = new CursorColor[] {
			new Fixed("Do not override", -1),
			cursorColor = new DistanceRainbow("Rainbow (distance based)"),
			new TimeRainbow("Rainbow (time based)"),
			new LastObject("Last object's color"),
			new NextObject("Next object's color"),
			new Combo("Combo1", 0),
			new Combo("Combo2", 1),
			new Combo("Combo3", 2),
			new Combo("Combo4", 3),
			new Combo("Combo5", 4),
			new Combo("Combo6", 5),
			new Combo("Combo7", 6),
			new Combo("Combo8", 7),
		};
		setComboColors(new Color[0]);
	}

	public static void setComboColors(Color[] colors)
	{
		if (colors.length == 0) {
			comboCols = new Color[] { Color.white };
			comboColors = new int[] { -1 };
		} else {
			comboCols = colors;
			comboColors = new int[colors.length];
			int i = colors.length;
			do {
				--i;
				comboColors[i] = col(colors[i]);
			} while (i > 0);
		}

		for (CursorColor impl : impls) {
			impl.onComboColorsChanged();
		}
	}

	public static void resetColorChange(int seed)
	{
		cursorColor.reset(seed);
	}

	static int col(Color color)
	{
		return
			(color.getRedByte() << 16) |
			(color.getGreenByte() << 8) |
			(color.getBlueByte());
	}

	public static boolean shouldShowCursorHueIncOption()
	{
		return cursorColor instanceof TimeRainbow || cursorColor instanceof DistanceRainbow;
	}
}
