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
import yugecin.opsudance.events.ResolutionChangedEvent;

public class FpsRenderState implements EventListener<ResolutionChangedEvent> {

	private final DisplayContainer displayContainer;

	private int x;
	private int y;
	private int singleHeight;

	public FpsRenderState(DisplayContainer displayContainer) {
		this.displayContainer = displayContainer;
		displayContainer.eventBus.subscribe(ResolutionChangedEvent.class, this);
	}

	public void render(Graphics g) {
		int x = this.x;
		x = drawText(g, (1000 / displayContainer.renderDelta) + " fps", x, this.y);
		drawText(g, (1000 / displayContainer.delta) + " ups", x, this.y);
	}

	private int drawText(Graphics g, String text, int x, int y) {
		int width = Fonts.SMALL.getWidth(text) + 10;
		g.setColor(new Color(0, 0x80, 0));
		g.fillRoundRect(x - width, y, width, singleHeight + 6, 2);
		Fonts.SMALL.drawString(x - width + 3, y + 3, text, Color.white);
		return x - width - 6;
	}

	@Override
	public void onEvent(ResolutionChangedEvent event) {
		singleHeight = Fonts.SMALL.getLineHeight();
		x = event.width - 3;
		y = event.height - 3 - singleHeight - 10;
	}

}
