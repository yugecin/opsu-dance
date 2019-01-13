// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor.colors;

import static itdelatrisu.opsu.ui.cursor.CursorImpl.lastObjColor;

class LastObject extends CursorColor
{
	LastObject(String name)
	{
		super(name);
	}

	@Override
	public int getMovementColor(float movementProgress)
	{
		return CursorColorManager.col(lastObjColor);
	}

	@Override
	public int getCurrentColor()
	{
		return CursorColorManager.col(lastObjColor);
	}

	@Override
	public void bindCurrentColor()
	{
		lastObjColor.bind();
	}
}
