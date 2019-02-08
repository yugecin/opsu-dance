// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor.colors;

public abstract class CursorColor
{
	public final String name;

	CursorColor(String name)
	{
		this.name = name;
	}

	public void update()
	{
	}

	/**
	 * delta can be assumed to be {@link yugecin.opsudance.core.DisplayContainer#delta}
	 */
	public void onMovement(int x1, int y1, int x2, int y2)
	{
	}

	/**
	 * Gets color at the specified progress during the move as specified by the last call
	 * to {@link #onMovement(int, int, int, int)} 
	 */
	public abstract int getMovementColor(float movementProgress);
	/**
	 * RRGGBB, no alpha 
	 */
	public abstract int getCurrentColor();
	public abstract void bindCurrentColor();

	void onComboColorsChanged()
	{
	}

	void reset(int seed)
	{
	}

	@Override
	public String toString()
	{
		return this.name;
	}
}
