// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import yugecin.opsudance.core.GlobalInputListener;

import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.core.InstanceContainer.*;

import java.util.Iterator;
import java.util.function.BiConsumer;

public class Input
{
	public static final int LMB = 0;
	public static final int RMB = 1;
	public static final int MMB = 2;

	private int lastMouseX;
	private int lastMouseY;

	private final KeyEvent[] keyEvents;

	public final InputListenerCollection<KeyListener> keyListeners;
	public final InputListenerCollection<MouseListener> mouseListeners;

	private boolean displayActive = true;

	public Input()
	{
		this.keyEvents = new KeyEvent[Keyboard.KEYBOARD_SIZE];
		final GlobalInputListener globalListener = new GlobalInputListener();
		this.keyListeners = new InputListenerCollection<>(globalListener);
		this.mouseListeners = new InputListenerCollection<>(globalListener);
	}

	/**
	 * convenience method
	 */
	public void addListener(InputListener listener)
	{
		this.mouseListeners.add(listener);
		this.keyListeners.add(listener);
	}

	/**
	 * convenience method
	 */
	public void removeListener(InputListener listener)
	{
		this.mouseListeners.remove(listener);
		this.keyListeners.remove(listener);
	}

	public boolean isAnyMouseButtonDown()
	{
		return Mouse.isButtonDown(0) || Mouse.isButtonDown(1) || Mouse.isButtonDown(2);
	}

	public boolean isAltDown()
	{
		return Keyboard.isKeyDown(KEY_LMENU) || Keyboard.isKeyDown(KEY_RMENU);
	}

	public boolean isControlDown()
	{
		return Keyboard.isKeyDown(KEY_RCONTROL) || Keyboard.isKeyDown(KEY_LCONTROL);
	}

	public boolean isShiftDown()
	{
		return Keyboard.isKeyDown(KEY_RSHIFT) || Keyboard.isKeyDown(KEY_LSHIFT);
	}

	public void poll()
	{
		mouseX = Mouse.getX();
		mouseY = height - Mouse.getY();

		while (Keyboard.next()) {
			final int keyCode = Keyboard.getEventKey();
			final KeyEvent e;
			if (Keyboard.getEventKeyState()) {
				e = new KeyEvent(keyCode, Keyboard.getEventCharacter());
				keyEvents[keyCode] = e;
				this.dispatch(this.keyListeners, KeyListener::keyPressed, e);
			} else {
				e = keyEvents[keyCode];
				e.consumed = false;
				this.dispatch(this.keyListeners, KeyListener::keyReleased, e);
				keyEvents[keyCode] = null; // allow GC
			}
		}

		while (Mouse.next()) {
			final int mouseButton = Mouse.getEventButton();
			// button is -1 if no button state was changed in this event
			if (mouseButton >= 0) {
				int eventX = Mouse.getEventX();
				int eventY = height - Mouse.getEventY();
				final MouseEvent e = new MouseEvent(mouseButton, eventX, eventY);
				final BiConsumer<MouseListener, MouseEvent> consumer;
				if (Mouse.getEventButtonState()) {
					lastMouseX = mousePressX = eventX;
					lastMouseY = mousePressY = eventY;
					consumer = MouseListener::mousePressed;
				} else {
					consumer = MouseListener::mouseReleased;
				}
				this.dispatch(this.mouseListeners, consumer, e);
				continue;
			}

			final int dx = Mouse.getEventDX();
			final int dy = Mouse.getEventDY();

			if (Mouse.isGrabbed() &&
				displayActive &&
				isAnyMouseButtonDown() &&
				(dx != 0 && dy != 0))
			{
				final MouseDragEvent e = new MouseDragEvent(dx, -dy);
				this.dispatch(this.mouseListeners, MouseListener::mouseDragged, e);
			}

			final int dwheel = Mouse.getEventDWheel();
			if (dwheel != 0) {
				this.dispatch(
					this.mouseListeners,
					MouseListener::mouseWheelMoved,
					new MouseWheelEvent(dwheel)
				);
			}
		}

		if (!displayActive || Mouse.isGrabbed()) {
			lastMouseX = mouseX;
			lastMouseY = mouseY;
		} else {
			if (isAnyMouseButtonDown() &&
				(lastMouseX != mouseX || lastMouseY != mouseY))
			{
				final int dx = mouseX - lastMouseX;
				final int dy = mouseY - lastMouseY;
				final MouseDragEvent e = new MouseDragEvent(dx, dy);
				this.dispatch(this.mouseListeners, MouseListener::mouseDragged, e);
				lastMouseX = mouseX;
				lastMouseY = mouseY;
			}
		}

		if (Display.isCreated()) {
			displayActive = Display.isActive();
		}
	}

	private <T, E extends Event> void dispatch(
		Iterable<T> listeners,
		BiConsumer<T, E> mapper,
		E e)
	{
		final Iterator<T> iter = listeners.iterator();
		do {
			mapper.accept(iter.next(), e);
		} while (iter.hasNext() && !e.consumed);
	}
}
