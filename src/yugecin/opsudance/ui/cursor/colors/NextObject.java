// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor.colors;

import static itdelatrisu.opsu.ui.cursor.CursorImpl.nextObjColor;

class NextObject extends CursorColor
{
	NextObject(String name)
	{
		super(name);
	}

	@Override
	public int getMovementColor(float movementProgress)
	{
		return CursorColorManager.col(nextObjColor);
	}

	@Override
	public int getCurrentColor()
	{
		return CursorColorManager.col(nextObjColor);
	}

	@Override
	public void bindCurrentColor()
	{
		nextObjColor.bind();
	}
}
