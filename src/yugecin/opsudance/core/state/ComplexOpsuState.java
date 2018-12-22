// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.state;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.components.Component;
import yugecin.opsudance.core.input.*;

import java.util.LinkedList;

import static yugecin.opsudance.core.InstanceContainer.*;

@Deprecated
public abstract class ComplexOpsuState extends BaseOpsuState
{
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
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		for (OverlayOpsuState overlay : overlays) {
			overlay.mouseWheelMoved(e);
			if (e.isConsumed()) {
				return;
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		for (OverlayOpsuState overlay : overlays) {
			overlay.mousePressed(e);
			if (e.isConsumed()) {
				return;
			}
		}
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
		for (OverlayOpsuState overlay : overlays) {
			overlay.mouseDragged(e);
			if (e.isConsumed()) {
				return;
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		for (OverlayOpsuState overlay : overlays) {
			overlay.mouseReleased(e);
			if (e.isConsumed()) {
				return;
			}
		}
		if (focusedComponent == null) {
			for (Component component : components) {
				if (!component.isFocusable()) {
					continue;
				}
				component.updateHover(e.x, e.y);
				if (component.isHovered()) {
					focusedComponent = component;
					focusedComponent.setFocused(true);
					e.consume();
					return;
				}
			}
			return;
		}
		focusedComponent.updateHover(e.x, e.y);
		if (focusedComponent.isHovered()) {
			focusedComponent.mouseReleased(e);
			e.consume();
			return;
		}
		focusedComponent.setFocused(false);
		focusedComponent = null;
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
	public void keyReleased(KeyEvent e)
	{
		for (OverlayOpsuState overlay : overlays) {
			overlay.keyReleased(e);
			if (e.isConsumed()) {
				return;
			}
		}
		if (focusedComponent != null) {
			if (e.keyCode == Keyboard.KEY_ESCAPE) {
				focusedComponent.setFocused(false);
				focusedComponent = null;
			} else {
				focusedComponent.keyReleased(e);
			}
			e.consume();
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		for (OverlayOpsuState overlay : overlays) {
			overlay.keyPressed(e);
			if (e.isConsumed()) {
				return;
			}
		}
		if (focusedComponent != null) {
			if (e.keyCode == Keyboard.KEY_ESCAPE) {
				focusedComponent.setFocused(false);
				focusedComponent = null;
			} else {
				focusedComponent.keyPressed(e);
			}
			e.consume();
		}
	}
}
