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
package yugecin.opsudance.core.state;

import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.ui.UI;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.events.EventListener;
import yugecin.opsudance.core.inject.Inject;
import yugecin.opsudance.events.BarNotificationEvent;
import yugecin.opsudance.events.ResolutionOrSkinChangedEvent;
import yugecin.opsudance.options.Configuration;
import yugecin.opsudance.skinning.SkinService;

import java.io.StringWriter;

import static yugecin.opsudance.options.Options.*;

public abstract class BaseOpsuState implements OpsuState, EventListener<ResolutionOrSkinChangedEvent> {

	@Inject
	protected DisplayContainer displayContainer;

	@Inject
	protected Configuration config;

	@Inject
	protected SkinService skinService;

	/**
	 * state is dirty when resolution or skin changed but hasn't rendered yet
	 */
	private boolean isDirty;
	private boolean isCurrentState;

	public BaseOpsuState() {
		EventBus.subscribe(ResolutionOrSkinChangedEvent.class, this);
	}

	protected void revalidate() {
	}

	@Override
	public void update() {
	}

	@Override
	public void preRenderUpdate() {
	}

	@Override
	public void render(Graphics g) {
	}

	@Override
	public void onEvent(ResolutionOrSkinChangedEvent event) {
		if (isCurrentState) {
			revalidate();
			return;
		}
		isDirty = true;
	}

	@Override
	public void enter() {
		isCurrentState = true;
		if (isDirty) {
			revalidate();
			isDirty = false;
		}
	}

	@Override
	public void leave() {
		isCurrentState = false;
	}

	@Override
	public boolean onCloseRequest() {
		return true;
	}

	@Override
	public boolean keyPressed(int key, char c) {
		return false;
	}

	@Override
	public boolean keyReleased(int key, char c) {
		if (key == Input.KEY_F7) {
			OPTION_TARGET_FPS.clickListItem((targetFPSIndex + 1) % targetFPS.length);
			EventBus.post(new BarNotificationEvent(String.format("Frame limiter: %s", OPTION_TARGET_FPS.getValueString())));
			return true;
		}
		if (key == Input.KEY_F10) {
			OPTION_DISABLE_MOUSE_BUTTONS.toggle();
			return true;
		}
		if (key == Input.KEY_F12) {
			config.takeScreenShot();
			return true;
		}
		Input input = displayContainer.input;
		if (key == Input.KEY_S && input.isKeyDown(Input.KEY_LMENU) && input.isKeyDown(Input.KEY_LSHIFT) &&input.isKeyDown(Input.KEY_LCONTROL) && !displayContainer.isInState(Game.class)) {
			skinService.reloadSkin();
		}
		return false;
	}

	@Override
	public boolean mouseWheelMoved(int delta) {
		if (displayContainer.input.isKeyDown(Input.KEY_LALT) || displayContainer.input.isKeyDown(Input.KEY_RALT)) {
			UI.changeVolume((delta < 0) ? -1 : 1);
			return true;
		}
		return false;
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		return false;
	}

	@Override
	public boolean mouseReleased(int button, int x, int y) {
		return false;
	}

	@Override
	public boolean mouseDragged(int oldx, int oldy, int newx, int newy) {
		return false;
	}

	@Override
	public void writeErrorDump(StringWriter dump) {
		dump.append("> BaseOpsuState dump\n");
		dump.append("isDirty: ").append(String.valueOf(isDirty)).append('\n');
	}

}
