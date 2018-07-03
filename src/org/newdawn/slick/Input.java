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

package org.newdawn.slick;

import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import static org.lwjgl.input.Keyboard.*;

/**
 * A wrapped for all keyboard, mouse and controller input
 * Edited for opsu!
 *
 * @author kevin
 */
@SuppressWarnings({"unused"})
public class Input {

	/** A helper for left ALT */
	public static final int KEY_LALT = KEY_LMENU;
	/** A helper for right ALT */
	public static final int KEY_RALT = KEY_RMENU;
	
	/** Control index */
	private static final int LEFT = 0;
	/** Control index */
	private static final int RIGHT = 1;
	/** Control index */
	private static final int UP = 2;
	/** Control index */
	private static final int DOWN = 3;
	/** Control index */
	private static final int BUTTON1 = 4;
	/** Control index */
	private static final int BUTTON2 = 5;
	/** Control index */
	private static final int BUTTON3 = 6;
	/** Control index */
	private static final int BUTTON4 = 7;
	/** Control index */
	private static final int BUTTON5 = 8;
	/** Control index */
	private static final int BUTTON6 = 9;
	/** Control index */
	private static final int BUTTON7 = 10;
	/** Control index */
	private static final int BUTTON8 = 11;
	/** Control index */
	private static final int BUTTON9 = 12;
	/** Control index */
	private static final int BUTTON10 = 13;
	
	/** The left mouse button indicator */
	public static final int MOUSE_LEFT_BUTTON = 0;
	/** The right mouse button indicator */
	public static final int MOUSE_RIGHT_BUTTON = 1;
	/** The middle mouse button indicator */
	public static final int MOUSE_MIDDLE_BUTTON = 2;
	
	/** The last recorded mouse x position */
	private int lastMouseX;
	/** The last recorded mouse y position */
	private int lastMouseY;
	/** THe state of the mouse buttons */
	protected boolean[] mousePressed = new boolean[10];

	/** The character values representing the pressed keys */
	protected char[] keys = new char[1024];
	/** True if the key has been pressed since last queries */
	protected boolean[] pressed = new boolean[1024];
	
	/** The listeners to notify of key events */
	protected ArrayList<KeyListener> keyListeners = new ArrayList<>();
	/** The listener to add */
	protected ArrayList<MouseListener> mouseListeners = new ArrayList<>();
	/** The current value of the wheel */
	private int wheel;
	/** The height of the display */
	private int height;
	
	/** True if the display is active */
	private boolean displayActive = true;
	
	/** The clicked button */
	private int clickButton;

	/**
	 * Create a new input with the height of the screen
	 * 
	 * @param height The height of the screen
	 */
	public Input(int height) {
		init(height);
	}

	/**
	 * Add a listener to be notified of input events
	 * 
	 * @param listener The listener to be notified
	 */
	public void addListener(InputListener listener) {
		addKeyListener(listener);
		addMouseListener(listener);
	}

	/**
	 * Add a key listener to be notified of key input events
	 * 
	 * @param listener The listener to be notified
	 */
	public void addKeyListener(KeyListener listener) {
		if (!keyListeners.contains(listener)) {
			keyListeners.add(listener);
		}
	}
	
	/**
	 * Add a mouse listener to be notified of mouse input events
	 * 
	 * @param listener The listener to be notified
	 */
	public void addMouseListener(MouseListener listener) {
		if (!mouseListeners.contains(listener)) {
			mouseListeners.add(listener);
		}
	}
	
	/**
	 * Remove all the listeners from this input
	 */
	public void removeAllListeners() {
		removeAllKeyListeners();
		removeAllMouseListeners();
	}

	/**
	 * Remove all the key listeners from this input
	 */
	public void removeAllKeyListeners() {
		keyListeners.clear();
	}

	/**
	 * Remove all the mouse listeners from this input
	 */
	public void removeAllMouseListeners() {
		mouseListeners.clear();
	}

	/**
	 * Add a listener to be notified of input events. This listener
	 * will get events before others that are currently registered
	 * 
	 * @param listener The listener to be notified
	 */
	public void addPrimaryListener(InputListener listener) {
		removeListener(listener);
		
		keyListeners.add(0, listener);
		mouseListeners.add(0, listener);
	}
	
	/**
	 * Remove a listener that will no longer be notified
	 * 
	 * @param listener The listen to be removed
	 */
	public void removeListener(InputListener listener) {
		removeKeyListener(listener);
		removeMouseListener(listener);
	}

	/**
	 * Remove a key listener that will no longer be notified
	 * 
	 * @param listener The listen to be removed
	 */
	public void removeKeyListener(KeyListener listener) {
		keyListeners.remove(listener);
	}

	/**
	 * Remove a mouse listener that will no longer be notified
	 * 
	 * @param listener The listen to be removed
	 */
	public void removeMouseListener(MouseListener listener) {
		mouseListeners.remove(listener);
	}
	
	/**
	 * Initialise the input system
	 * 
	 * @param height The height of the window
	 */
	void init(int height) {
		this.height = height;
		lastMouseX = getMouseX();
		lastMouseY = getMouseY();
	}

	/**
	 * Check if a particular key has been pressed since this method 
	 * was last called for the specified key
	 * 
	 * @param code The key code of the key to check
	 * @return True if the key has been pressed
	 */
	public boolean isKeyPressed(int code) {
		if (pressed[code]) {
			pressed[code] = false;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check if a mouse button has been pressed since last call
	 * 
	 * @param button The button to check
	 * @return True if the button has been pressed since last call
	 */
	public boolean isMousePressed(int button) {
		if (mousePressed[button]) {
			mousePressed[button] = false;
			return true;
		}
		
		return false;
	}

	/**
	 * Clear the state for the <code>isKeyPressed</code> method. This will
	 * resort in all keys returning that they haven't been pressed, until
	 * they are pressed again
	 */
	public void clearKeyPressedRecord() {
		Arrays.fill(pressed, false);
	}

	/**
	 * Clear the state for the <code>isMousePressed</code> method. This will
	 * resort in all mouse buttons returning that they haven't been pressed, until
	 * they are pressed again
	 */
	public void clearMousePressedRecord() {
		Arrays.fill(mousePressed, false);
	}

	/**
	 * Get the x position of the mouse cursor
	 * 
	 * @return The x position of the mouse cursor
	 */
	public int getMouseX() {
		return Mouse.getX();
	}
	
	/**
	 * Get the y position of the mouse cursor
	 * 
	 * @return The y position of the mouse cursor
	 */
	public int getMouseY() {
		return height - Mouse.getY();
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

	public boolean isControlDown() {
		return Keyboard.isKeyDown(KEY_RCONTROL) || Keyboard.isKeyDown(KEY_LCONTROL);
	}

	/**
	 * Poll the state of the input
	 * 
	 * @param width The width of the game view
	 * @param height The height of the game view
	 */
	public void poll(int width, int height) {
		if (!Display.isActive()) {
			clearKeyPressedRecord();
			clearMousePressedRecord();
		}

		this.height = height;

		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				int eventKey = Keyboard.getEventKey();

				keys[eventKey] = Keyboard.getEventCharacter();
				pressed[eventKey] = true;
				
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
			if (Mouse.getEventButton() >= 0 && Mouse.getEventButton() < mousePressed.length) {
				if (Mouse.getEventButtonState()) {
					mousePressed[Mouse.getEventButton()] = true;

					lastMouseX = Mouse.getEventX();
					lastMouseY = height - Mouse.getEventY();

					for (MouseListener listener : mouseListeners) {
						if (listener.mousePressed(Mouse.getEventButton(), lastMouseX, lastMouseY)) {
							break;
						}
					}
				} else {
					mousePressed[Mouse.getEventButton()] = false;
					
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
				wheel += dwheel;
				if (dwheel != 0) {
					for (MouseListener listener : mouseListeners) {
						if (listener.mouseWheelMoved(dwheel)) {
							break;
						}
					}
				}
			}
		}
		
		if (!displayActive || Mouse.isGrabbed()) {
			lastMouseX = getMouseX();
			lastMouseY = getMouseY();
		} else {
			if (anyMouseDown() && (lastMouseX != getMouseX() || lastMouseY != getMouseY())) {
				for (MouseListener listener : mouseListeners) {
					if (listener.mouseDragged(lastMouseX ,  lastMouseY, getMouseX(), getMouseY())) {
						break;
					}
				}
				lastMouseX = getMouseX();
				lastMouseY = getMouseY();
			}
		}

		if (Display.isCreated()) {
			displayActive = Display.isActive();
		}
	}
	
	/**
	 * Enable key repeat for this input context. Uses the system settings for repeat
	 * interval configuration.
	 */
	public void enableKeyRepeat() {
		Keyboard.enableRepeatEvents(true);
	}
	
	/**
	 * Disable key repeat for this input context
	 */
	public void disableKeyRepeat() {
		Keyboard.enableRepeatEvents(false);
	}
	
	/**
	 * Check if key repeat is enabled
	 * 
	 * @return True if key repeat is enabled
	 */
	public boolean isKeyRepeatEnabled() {
		return Keyboard.areRepeatEventsEnabled();
	}

}
