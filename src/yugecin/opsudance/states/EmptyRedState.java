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
package yugecin.opsudance.states;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.state.OpsuState;
import yugecin.opsudance.events.BarNotificationEvent;
import yugecin.opsudance.events.BubbleNotificationEvent;

import java.io.StringWriter;

public class EmptyRedState implements OpsuState {

	private int counter;
	private long start;

	private final DisplayContainer displayContainer;

	public EmptyRedState(DisplayContainer displayContainer) {
		this.displayContainer = displayContainer;
	}

	@Override
	public void update() {
		counter -= displayContainer.delta;
		if (counter < 0) {
			counter = 10000; // to prevent more calls to switch, as this will keep rendering until state transitioned
			System.out.println(System.currentTimeMillis() - start);
			displayContainer.switchState(EmptyState.class);
		}
	}

	@Override
	public void preRenderUpdate() {
	}

	@Override
	public void render(Graphics g) {
		g.setColor(Color.red);
		g.fillRect(0, 0, 100, 100);
	}

	@Override
	public void enter() {
		counter = 5000;
		start = System.currentTimeMillis();
	}

	@Override
	public void leave() {
	}

	@Override
	public boolean onCloseRequest() {
		return true;
	}

	@Override
	public boolean keyPressed(int key, char c) {
		EventBus.post(new BubbleNotificationEvent("this is a bubble notification... bubbly bubbly bubbly linewraaaaaaaaaap", BubbleNotificationEvent.COMMONCOLOR_RED));
		return false;
	}

	@Override
	public boolean keyReleased(int key, char c) {
		return false;
	}

	@Override
	public boolean mouseWheelMoved(int delta) {
		EventBus.post(new BubbleNotificationEvent("Life is like a box of chocolates. It's all going to melt by the end of the day.\n-Emily", BubbleNotificationEvent.COMMONCOLOR_PURPLE));
		return false;
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		return false;
	}

	@Override
	public boolean mouseReleased(int button, int x, int y) {
		EventBus.post(new BarNotificationEvent("this is a\nbar notification"));
		return false;
	}

	@Override
	public boolean mouseDragged(int oldx, int oldy, int newx, int newy) {
		return false;
	}

	@Override
	public void writeErrorDump(StringWriter dump) {
		dump.append("> EmptyRedState dump\n");
		dump.append("its red\n");
	}

}
