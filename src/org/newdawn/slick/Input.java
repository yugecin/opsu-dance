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

/**
 * A wrapped for all keyboard, mouse and controller input
 * Edited for opsu!
 *
 * @author kevin
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public class Input {

	public static final int KEY_ESCAPE          = 0x01;
	public static final int KEY_1               = 0x02;
	public static final int KEY_2               = 0x03;
	public static final int KEY_3               = 0x04;
	public static final int KEY_4               = 0x05;
	public static final int KEY_5               = 0x06;
	public static final int KEY_6               = 0x07;
	public static final int KEY_7               = 0x08;
	public static final int KEY_8               = 0x09;
	public static final int KEY_9               = 0x0A;
	public static final int KEY_0               = 0x0B;
	public static final int KEY_MINUS           = 0x0C; /* - on main keyboard */
	public static final int KEY_EQUALS          = 0x0D;
	public static final int KEY_BACK            = 0x0E; /* backspace */
	public static final int KEY_TAB             = 0x0F;
	public static final int KEY_Q               = 0x10;
	public static final int KEY_W               = 0x11;
	public static final int KEY_E               = 0x12;
	public static final int KEY_R               = 0x13;
	public static final int KEY_T               = 0x14;
	public static final int KEY_Y               = 0x15;
	public static final int KEY_U               = 0x16;
	public static final int KEY_I               = 0x17;
	public static final int KEY_O               = 0x18;
	public static final int KEY_P               = 0x19;
	public static final int KEY_LBRACKET        = 0x1A;
	public static final int KEY_RBRACKET        = 0x1B;
	public static final int KEY_RETURN          = 0x1C; /* Enter on main keyboard */
	public static final int KEY_ENTER           = 0x1C; /* Enter on main keyboard */
	public static final int KEY_LCONTROL        = 0x1D;
	public static final int KEY_A               = 0x1E;
	public static final int KEY_S               = 0x1F;
	public static final int KEY_D               = 0x20;
	public static final int KEY_F               = 0x21;
	public static final int KEY_G               = 0x22;
	public static final int KEY_H               = 0x23;
	public static final int KEY_J               = 0x24;
	public static final int KEY_K               = 0x25;
	public static final int KEY_L               = 0x26;
	public static final int KEY_SEMICOLON       = 0x27;
	public static final int KEY_APOSTROPHE      = 0x28;
	public static final int KEY_GRAVE           = 0x29; /* accent grave */
	public static final int KEY_LSHIFT          = 0x2A;
	public static final int KEY_BACKSLASH       = 0x2B;
	public static final int KEY_Z               = 0x2C;
	public static final int KEY_X               = 0x2D;
	public static final int KEY_C               = 0x2E;
	public static final int KEY_V               = 0x2F;
	public static final int KEY_B               = 0x30;
	public static final int KEY_N               = 0x31;
	public static final int KEY_M               = 0x32;
	public static final int KEY_COMMA           = 0x33;
	public static final int KEY_PERIOD          = 0x34; /* . on main keyboard */
	public static final int KEY_SLASH           = 0x35; /* / on main keyboard */
	public static final int KEY_RSHIFT          = 0x36;
	public static final int KEY_MULTIPLY        = 0x37; /* * on numeric keypad */
	public static final int KEY_LMENU           = 0x38; /* left Alt */
	public static final int KEY_SPACE           = 0x39;
	public static final int KEY_CAPITAL         = 0x3A;
	public static final int KEY_F1              = 0x3B;
	public static final int KEY_F2              = 0x3C;
	public static final int KEY_F3              = 0x3D;
	public static final int KEY_F4              = 0x3E;
	public static final int KEY_F5              = 0x3F;
	public static final int KEY_F6              = 0x40;
	public static final int KEY_F7              = 0x41;
	public static final int KEY_F8              = 0x42;
	public static final int KEY_F9              = 0x43;
	public static final int KEY_F10             = 0x44;
	public static final int KEY_NUMLOCK         = 0x45;
	public static final int KEY_SCROLL          = 0x46; /* Scroll Lock */
	public static final int KEY_NUMPAD7         = 0x47;
	public static final int KEY_NUMPAD8         = 0x48;
	public static final int KEY_NUMPAD9         = 0x49;
	public static final int KEY_SUBTRACT        = 0x4A; /* - on numeric keypad */
	public static final int KEY_NUMPAD4         = 0x4B;
	public static final int KEY_NUMPAD5         = 0x4C;
	public static final int KEY_NUMPAD6         = 0x4D;
	public static final int KEY_ADD             = 0x4E; /* + on numeric keypad */
	public static final int KEY_NUMPAD1         = 0x4F;
	public static final int KEY_NUMPAD2         = 0x50;
	public static final int KEY_NUMPAD3         = 0x51;
	public static final int KEY_NUMPAD0         = 0x52;
	public static final int KEY_DECIMAL         = 0x53; /* . on numeric keypad */
	public static final int KEY_F11             = 0x57;
	public static final int KEY_F12             = 0x58;
	public static final int KEY_F13             = 0x64; /*                     (NEC PC98) */
	public static final int KEY_F14             = 0x65; /*                     (NEC PC98) */
	public static final int KEY_F15             = 0x66; /*                     (NEC PC98) */
	public static final int KEY_KANA            = 0x70; /* (Japanese keyboard)            */
	public static final int KEY_CONVERT         = 0x79; /* (Japanese keyboard)            */
	public static final int KEY_NOCONVERT       = 0x7B; /* (Japanese keyboard)            */
	public static final int KEY_YEN             = 0x7D; /* (Japanese keyboard)            */
	public static final int KEY_NUMPADEQUALS    = 0x8D; /* = on numeric keypad (NEC PC98) */
	public static final int KEY_CIRCUMFLEX      = 0x90; /* (Japanese keyboard)            */
	public static final int KEY_AT              = 0x91; /*                     (NEC PC98) */
	public static final int KEY_COLON           = 0x92; /*                     (NEC PC98) */
	public static final int KEY_UNDERLINE       = 0x93; /*                     (NEC PC98) */
	public static final int KEY_KANJI           = 0x94; /* (Japanese keyboard)            */
	public static final int KEY_STOP            = 0x95; /*                     (NEC PC98) */
	public static final int KEY_AX              = 0x96; /*                     (Japan AX) */
	public static final int KEY_UNLABELED       = 0x97; /*                        (J3100) */
	public static final int KEY_NUMPADENTER     = 0x9C; /* Enter on numeric keypad */
	public static final int KEY_RCONTROL        = 0x9D;
	public static final int KEY_NUMPADCOMMA     = 0xB3; /* , on numeric keypad (NEC PC98) */
	public static final int KEY_DIVIDE          = 0xB5; /* / on numeric keypad */
	public static final int KEY_SYSRQ           = 0xB7;
	public static final int KEY_RMENU           = 0xB8; /* right Alt */
	public static final int KEY_PAUSE           = 0xC5; /* Pause */
	public static final int KEY_HOME            = 0xC7; /* Home on arrow keypad */
	public static final int KEY_UP              = 0xC8; /* UpArrow on arrow keypad */
	public static final int KEY_PRIOR           = 0xC9; /* PgUp on arrow keypad */
	public static final int KEY_LEFT            = 0xCB; /* LeftArrow on arrow keypad */
	public static final int KEY_RIGHT           = 0xCD; /* RightArrow on arrow keypad */
	public static final int KEY_END             = 0xCF; /* End on arrow keypad */
	public static final int KEY_DOWN            = 0xD0; /* DownArrow on arrow keypad */
	public static final int KEY_NEXT            = 0xD1; /* PgDn on arrow keypad */
	public static final int KEY_INSERT          = 0xD2; /* Insert on arrow keypad */
	public static final int KEY_DELETE          = 0xD3; /* Delete on arrow keypad */
	public static final int KEY_LWIN            = 0xDB; /* Left Windows key */
	public static final int KEY_RWIN            = 0xDC; /* Right Windows key */
	public static final int KEY_APPS            = 0xDD; /* AppMenu key */
	public static final int KEY_POWER           = 0xDE;
	public static final int KEY_SLEEP           = 0xDF;
	
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
	/** The time since the next key repeat to be fired for the key */
	protected long[] nextRepeat = new long[1024];
	
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
	
	/** True if key repeat is enabled */
	private boolean keyRepeat;
	/** The initial delay for key repeat starts */
	private int keyRepeatInitial;
	/** The interval of key repeat */
	private int keyRepeatInterval;
	
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
		keyListeners.add(listener);
	}
	
	/**
	 * Add a mouse listener to be notified of mouse input events
	 * 
	 * @param listener The listener to be notified
	 */
	public void addMouseListener(MouseListener listener) {
		mouseListeners.add(listener);
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
	 * Get the character representation of the key identified by the specified code
	 * 
	 * @param code The key code of the key to retrieve the name of
	 * @return The name or character representation of the key requested
	 */
	public static String getKeyName(int code) {
		return Keyboard.getKeyName(code);
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
	 * Check if a particular key is down
	 * 
	 * @param code The key code of the key to check
	 * @return True if the key is down
	 */
	public boolean isKeyDown(int code) {
		return Keyboard.isKeyDown(code);
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
	 * Check if a given mouse button is down
	 * 
	 * @param button The index of the button to check (starting at 0)
	 * @return True if the mouse button is down
	 */
	public boolean isMouseButtonDown(int button) {
		return Mouse.isButtonDown(button);
	}
	
	/**
	 * Check if any mouse button is down
	 * 
	 * @return True if any mouse button is down
	 */
	private boolean anyMouseDown() {
		for (int i=0;i<3;i++) {
			if (Mouse.isButtonDown(i)) {
				return true;
			}
		}
		
		return false;
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
				nextRepeat[eventKey] = System.currentTimeMillis() + keyRepeatInitial;
				
				for (KeyListener listener : keyListeners) {
					if (listener.keyPressed(eventKey, Keyboard.getEventCharacter())) {
						break;
					}
				}
			} else {
				int eventKey = Keyboard.getEventKey();
				nextRepeat[eventKey] = 0;
				
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
		
		if (keyRepeat) {
			for (int i=0;i<1024;i++) {
				if (pressed[i] && (nextRepeat[i] != 0)) {
					if (System.currentTimeMillis() > nextRepeat[i]) {
						nextRepeat[i] = System.currentTimeMillis() + keyRepeatInterval;
						for (KeyListener listener : keyListeners) {
							if (listener.keyPressed(i, keys[i])) {
								break;
							}
						}
					}
				}
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
