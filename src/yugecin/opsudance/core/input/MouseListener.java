// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.input;

public interface MouseListener
{
	void mouseWheelMoved(MouseWheelEvent e);
	void mousePressed(MouseEvent e);
	void mouseReleased(MouseEvent e);
	void mouseDragged(MouseDragEvent e);
}
