// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor.colors;

import static yugecin.opsudance.ui.cursor.colors.CursorColorManager.*;

import org.newdawn.slick.Color;

class Combo extends CursorColor
{
	final int comboIndex;

	private Color currentCol;
	private int currentColor;

	Combo(String name, int comboIndex)
	{
		super(name);
		this.comboIndex = comboIndex;
	}

	@Override
	public int getMovementColor(float movementProgress)
	{
		return currentColor;
	}

	@Override
	public int getCurrentColor()
	{
		return currentColor;
	}

	@Override
	public void bindCurrentColor()
	{
		this.currentCol.bind();
	}

	@Override
	void onComboColorsChanged()
	{
		final int idx = this.comboIndex % comboColors.length;
		this.currentCol = comboCols[idx];
		this.currentColor = comboColors[idx];
	}
}
