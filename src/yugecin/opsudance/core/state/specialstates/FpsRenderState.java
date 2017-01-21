/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
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
package yugecin.opsudance.core.state.specialstates;

import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.events.EventListener;
import yugecin.opsudance.events.ResolutionOrSkinChangedEvent;

public class FpsRenderState implements EventListener<ResolutionOrSkinChangedEvent> {

	private final DisplayContainer displayContainer;

	private final static Color GREEN = new Color(171, 218, 25);
	private final static Color ORANGE = new Color(255, 204, 34);
	private final static Color DARKORANGE = new Color(255, 149, 24);

	private int x;
	private int y;
	private int singleHeight;

	public FpsRenderState(DisplayContainer displayContainer) {
		this.displayContainer = displayContainer;
		displayContainer.eventBus.subscribe(ResolutionOrSkinChangedEvent.class, this);
	}

	public void render(Graphics g) {
		int x = this.x;
		int target = displayContainer.targetRenderInterval + (displayContainer.targetUpdateInterval % displayContainer.targetRenderInterval);
		x = drawText(g, getColor(target, displayContainer.renderDelta), (1000 / displayContainer.renderDelta) + " fps", x, this.y);
		drawText(g, getColor(displayContainer.targetUpdateInterval, displayContainer.delta), (1000 / displayContainer.delta) + " ups", x, this.y);
	}

	private Color getColor(int targetValue, int realValue) {
		if (realValue <= targetValue) {
			return GREEN;
		}
		if (realValue <= targetValue * 1.15f) {
			return ORANGE;
		}
		return DARKORANGE;
	}

	/**
	 * @return x position where the next block can be drawn (right aligned)
	 */
	private int drawText(Graphics g, Color color, String text, int x, int y) {
		int width = Fonts.SMALL.getWidth(text) + 10;
		g.setColor(color);
		g.fillRoundRect(x - width, y, width, singleHeight + 6, 2);
		Fonts.SMALL.drawString(x - width + 3, y + 3, text, Color.black);
		return x - width - 6;
	}

	@Override
	public void onEvent(ResolutionOrSkinChangedEvent event) {
		singleHeight = Fonts.SMALL.getLineHeight();
		x = displayContainer.width - 3;
		y = displayContainer.height - 3 - singleHeight - 10;
	}

}
