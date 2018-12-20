// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor;

public interface Cursor
{
	void draw(boolean expanded);
	void setCursorPosition(int x, int y);
	void reset();
	void destroy();
	boolean isBeatmapSkinned();
}
