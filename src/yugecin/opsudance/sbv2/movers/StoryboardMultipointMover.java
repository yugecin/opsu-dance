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
package yugecin.opsudance.sbv2.movers;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.render.MovablePointCollectionRenderer;

public abstract class StoryboardMultipointMover extends StoryboardMover {

	protected final MovablePointCollectionRenderer movablePointCollectionRenderer;

	public StoryboardMultipointMover(Color renderColor, Color pointColor) {
		super(renderColor);
		movablePointCollectionRenderer = new MovablePointCollectionRenderer(pointColor);
	}

	@Override
	public void update(int delta, int x, int y) {
		movablePointCollectionRenderer.update(x, y);
	}

	@Override
	public boolean mousePressed(int x, int y) {
		return movablePointCollectionRenderer.mousePressed(x, y);
	}

	@Override
	public boolean mouseReleased(int x, int y) {
		return movablePointCollectionRenderer.mouseReleased();
	}

	@Override
	public void render(Graphics g) {
		movablePointCollectionRenderer.renderWithDottedLines(g, Color.gray, start, end);
		super.render(g);
	}

}
