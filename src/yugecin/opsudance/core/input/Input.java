/*
 * Copyright (c) 2013, Slick2D
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Slick2D nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package yugecin.opsudance.core.input;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.newdawn.slick.InputListener;
import org.newdawn.slick.KeyListener;
import org.newdawn.slick.MouseListener;

import yugecin.opsudance.core.GlobalInputListener;

import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * somewhat based on {@link org.newdawn.slick.Input}
 */
public class Input
{
	public static final int LMB = 0;
	public static final int RMB = 1;
	public static final int MMB = 2;
	
	/** The last recorded mouse x position */
	private int lastMouseX;
	/** The last recorded mouse y position */
	private int lastMouseY;

	/** The character values representing the pressed keys */
	protected char[] keys = new char[1024];

	public final InputListenerCollection<KeyListener> keyListeners;
	public final InputListenerCollection<MouseListener> mouseListeners;

	/** True if the display is active */
	private boolean displayActive = true;

	public Input()
	{
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

	/**
	 * Check if any mouse button is down
	 * 
	 * @return True if any mouse button is down
	 */
	public boolean anyMouseDown() {
		for (int i=0;i<3;i++) {
			if (Mouse.isButtonDown(i)) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isAltDown() {
		return Keyboard.isKeyDown(KEY_LMENU) || Keyboard.isKeyDown(KEY_RMENU);
	}

	public boolean isControlDown() {
		return Keyboard.isKeyDown(KEY_RCONTROL) || Keyboard.isKeyDown(KEY_LCONTROL);
	}

	public void poll()
	{
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				int eventKey = Keyboard.getEventKey();

				keys[eventKey] = Keyboard.getEventCharacter();

				for (KeyListener listener : keyListeners) {
					if (listener.keyPressed(eventKey, Keyboard.getEventCharacter())) {
						break;
					}
				}
			} else {
				int eventKey = Keyboard.getEventKey();
				
				for (KeyListener listener : keyListeners) {
					if (listener.keyReleased(eventKey, keys[eventKey])) {
						break;
					}
				}
			}
		}
		
		while (Mouse.next()) {
			if (Mouse.getEventButton() >= 0) {
				if (Mouse.getEventButtonState()) {
					lastMouseX = Mouse.getEventX();
					lastMouseY = height - Mouse.getEventY();

					for (MouseListener listener : mouseListeners) {
						if (listener.mousePressed(Mouse.getEventButton(), lastMouseX, lastMouseY)) {
							break;
						}
					}
				} else {
					int releasedX = Mouse.getEventX();
					int releasedY = height - Mouse.getEventY();

					for (MouseListener listener : mouseListeners) {
						if (listener.mouseReleased(Mouse.getEventButton(), releasedX, releasedY)) {
							break;
						}
					}
				}
			} else {
				if (Mouse.isGrabbed() && displayActive && anyMouseDown() &&
						((Mouse.getEventDX() != 0) || (Mouse.getEventDY() != 0))) {
					for (MouseListener listener : mouseListeners) {
						if (listener.mouseDragged(0, 0, Mouse.getEventDX(), -Mouse.getEventDY())) {
							break;
						}
					}
				}
				
				int dwheel = Mouse.getEventDWheel();
				if (dwheel != 0) {
					for (MouseListener listener : mouseListeners) {
						if (listener.mouseWheelMoved(dwheel)) {
							break;
						}
					}
				}
			}
		}

		mouseX = Mouse.getX();
		mouseY = height - Mouse.getY();

		if (!displayActive || Mouse.isGrabbed()) {
			lastMouseX = mouseX;
			lastMouseY = mouseY;
		} else {
			if (anyMouseDown() && (lastMouseX != mouseX || lastMouseY != mouseY)) {
				for (MouseListener listener : mouseListeners) {
					if (listener.mouseDragged(lastMouseX ,  lastMouseY, mouseX, mouseY)) {
						break;
					}
				}
				lastMouseX = mouseX;
				lastMouseY = mouseY;
			}
		}

		if (Display.isCreated()) {
			displayActive = Display.isActive();
		}
	}
}
