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

import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.components.Component;

import java.util.LinkedList;

public class ComplexOpsuState extends BaseOpsuState {

	protected final LinkedList<Component> components;

	private Component focusedComponent;

	public ComplexOpsuState(DisplayContainer displayContainer) {
		super(displayContainer);
		this.components = new LinkedList<>();
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
		return focusedComponent != null;
	}

	@Override
	public boolean mouseReleased(int button, int x, int y) {
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
			component.updateHover(displayContainer.mouseX, displayContainer.mouseY);
			component.preRenderUpdate();
		}
	}

	@Override
	public void render(Graphics g) {
		super.render(g);
		for (Component component : components) {
			component.render(g);
		}
	}

	@Override
	public boolean keyReleased(int key, char c) {
		if (super.keyReleased(key, c)) {
			return true;
		}
		if (focusedComponent != null) {
			if (key == Input.KEY_ESCAPE) {
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
		if (focusedComponent != null) {
			if (key == Input.KEY_ESCAPE) {
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
