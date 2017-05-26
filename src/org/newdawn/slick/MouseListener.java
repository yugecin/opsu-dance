package org.newdawn.slick;

/**
 * Description of classes that respond to mouse related input events
 * Edited for opsu!
 *
 * @author kevin
 */
public interface MouseListener {

	/**
	 * Notification that the mouse wheel position was updated
	 *
	 * @param delta The amount of the wheel has moved
	 */
	boolean mouseWheelMoved(int delta);

	/**
	 * Notification that a mouse button was pressed
	 *
	 * @param button The index of the button (starting at 0)
	 * @param x The x position of the mouse when the button was pressed
	 * @param y The y position of the mouse when the button was pressed
	 */
	boolean mousePressed(int button, int x, int y);

	/**
	 * Notification that a mouse button was released
	 *
	 * @param button The index of the button (starting at 0)
	 * @param x The x position of the mouse when the button was released
	 * @param y The y position of the mouse when the button was released
	 */
	boolean mouseReleased(int button, int x, int y);

	/**
	 * Notification that mouse cursor was dragged
	 *
	 * @param oldx The old x position of the mouse
	 * @param oldy The old y position of the mouse
	 * @param newx The new x position of the mouse
	 * @param newy The new y position of the mouse
	 */
	boolean mouseDragged(int oldx, int oldy, int newx, int newy);

}
