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

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.components.Component;

import java.util.LinkedList;

import static yugecin.opsudance.core.InstanceContainer.*;

public abstract class ComplexOpsuState extends BaseOpsuState {

	protected final LinkedList<Component> components;
	protected final LinkedList<OverlayOpsuState> overlays;

	private Component focusedComponent;

	public ComplexOpsuState() {
		super();
		this.components = new LinkedList<>();
		this.overlays = new LinkedList<>();
	}

	public final void focusComponent(Component component) {
		if (!component.isFocusable()) {
			return;
		}
		if (focusedComponent != null) {
			focusedComponent.setFocused(false);
		}
		focusedComponent = component;
		component.setFocused(true);
	}

	public boolean isAnyComponentFocused() {
		return focusedComponent != null || isAnyOverlayActive();
	}

	public boolean isAnyOverlayActive() {
		for (OverlayOpsuState overlay : overlays) {
			if (overlay.active) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseWheelMoved(int delta) {
		if (super.mouseWheelMoved(delta)) {
			return true;
		}
		for (OverlayOpsuState overlay : overlays) {
			if (overlay.mouseWheelMoved(delta)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		for (OverlayOpsuState overlay : overlays) {
			if (overlay.mousePressed(button, x, y)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(int oldx, int oldy, int newx, int newy) {
		for (OverlayOpsuState overlay : overlays) {
			if (overlay.mouseDragged(oldx, oldy, newx, newy)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(int button, int x, int y) {
		for (OverlayOpsuState overlay : overlays) {
			if (overlay.mouseReleased(button, x, y)) {
				return true;
			}
		}
		if (focusedComponent == null) {
			for (Component component : components) {
				if (!component.isFocusable()) {
					continue;
				}
				component.updateHover(x, y);
				if (component.isHovered()) {
					focusedComponent = component;
					focusedComponent.setFocused(true);
					return true;
				}
			}
			return false;
		}
		focusedComponent.updateHover(x, y);
		if (focusedComponent.isHovered()) {
			focusedComponent.mouseReleased(button);
			return true;
		}
		focusedComponent.setFocused(false);
		focusedComponent = null;
		return true;
	}

	@Override
	public void preRenderUpdate() {
		super.preRenderUpdate();
		for (Component component : components) {
			component.updateHover(mouseX, mouseY);
			component.preRenderUpdate();
		}
		for (OverlayOpsuState overlay : overlays) {
			overlay.preRenderUpdate();
		}
	}

	@Override
	protected void revalidate() {
		super.revalidate();
		for (OverlayOpsuState overlay : overlays) {
			overlay.revalidate();
		}
	}

	@Override
	public void render(Graphics g) {
		for (OverlayOpsuState overlay : overlays) {
			overlay.render(g);
		}
		super.render(g);
	}

	@Override
	public boolean keyReleased(int key, char c) {
		if (super.keyReleased(key, c)) {
			return true;
		}
		for (OverlayOpsuState overlay : overlays) {
			if (overlay.keyReleased(key, c)) {
				return true;
			}
		}
		if (focusedComponent != null) {
			if (key == Keyboard.KEY_ESCAPE) {
				focusedComponent.setFocused(false);
				focusedComponent = null;
				return true;
			}
			focusedComponent.keyReleased(key, c);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int key, char c) {
		for (OverlayOpsuState overlay : overlays) {
			if (overlay.keyPressed(key, c)) {
				return true;
			}
		}
		if (focusedComponent != null) {
			if (key == Keyboard.KEY_ESCAPE) {
				focusedComponent.setFocused(false);
				focusedComponent = null;
				return true;
			}
			focusedComponent.keyPressed(key, c);
			return true;
		}
		return false;
	}

}
