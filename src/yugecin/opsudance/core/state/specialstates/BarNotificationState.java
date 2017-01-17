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
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.events.EventListener;
import yugecin.opsudance.events.BarNotificationEvent;
import yugecin.opsudance.events.ResolutionChangedEvent;

public class BarNotificationState implements EventListener<BarNotificationEvent> {

	private final int NOTIFICATION_TIME = 5000;

	private final DisplayContainer displayContainer;
	private final Color bgcol;

	private int timeShown;

	private String message;
	private int textX;
	private int textY;
	private int barY;
	private int barHeight;

	public BarNotificationState(DisplayContainer displayContainer, EventBus eventBus) {
		this.displayContainer = displayContainer;
		this.bgcol = new Color(0f, 0f, 0f, 0f);
		this.timeShown = NOTIFICATION_TIME;
		eventBus.subscribe(BarNotificationEvent.class, this);
		eventBus.subscribe(ResolutionChangedEvent.class, new EventListener<ResolutionChangedEvent>() {
			@Override
			public void onEvent(ResolutionChangedEvent event) {
				if (timeShown >= NOTIFICATION_TIME) {
					return;
				}
				calculatePosition();
			}
		});
	}

	public void render(Graphics g, int delta) {
		if (timeShown >= NOTIFICATION_TIME) {
			return;
		}
		timeShown += delta;
		g.setColor(bgcol);
		g.fillRect(0, barY, displayContainer.width, barHeight);
		Fonts.LARGE.drawString(textX, textY, message);
	}

	private void calculatePosition() {
		int textHeight = Fonts.LARGE.getHeight(message);
		int textWidth = Fonts.LARGE.getWidth(message);
		textX = (displayContainer.width - textWidth) / 2;
		textY = (displayContainer.height - textHeight) / 2;
		barY = textY - 5; // TODO uiscale stuff?
		barHeight = textHeight + 10;
	}

	@Override
	public void onEvent(BarNotificationEvent event) {
		this.message = event.message;
		calculatePosition();
		timeShown = 0;
	}

}
