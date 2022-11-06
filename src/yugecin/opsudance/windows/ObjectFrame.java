package yugecin.opsudance.windows;

import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class ObjectFrame extends JFrame
{
	public long lastGLUpdate;
	public long lastRender;
	public int top, left, width, height;
	public boolean show;
	public ThingPanel tp;

	public ObjectFrame()
	{
		this.setLayout(null);
		this.add(tp = new ThingPanel());
	}

	public void update()
	{
		if (lastGLUpdate > lastRender) {
			if (this.getWidth() != width || this.getHeight() != height) {
				this.setSize(width, height);
			}
			if (this.getX() != left || this.getY() != top) {
				this.setLocation(left, top);
				tp.setSize(width, height);
			}
			tp.invalidate();
			tp.repaint();
			this.lastRender = System.currentTimeMillis();
			if (!this.isVisible()) {
				this.setVisible(true);
			}
		}
	}

	class ThingPanel extends JPanel
	{
		@Override
		protected void paintComponent(Graphics g)
		{
			g.drawImage(WindowManager.a, 0, 0, width, height, left, top, left + width, top + height, null);
		}
	}
}
