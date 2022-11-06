package yugecin.opsudance.windows;

import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class ObjectFrame extends JFrame
{
	public long lastGLUpdate;
	public long lastRender;
	public int y, x, width, height;
	public boolean show;
	public ThingPanel tp;

	public ObjectFrame(String title)
	{
		this.setTitle(title);
		this.setResizable(false);
		this.setType(Type.UTILITY);
		this.setType(Type.POPUP);
		this.setLayout(null);
		this.add(tp = new ThingPanel());
	}

	public void update()
	{
		if (lastGLUpdate > lastRender) {
			if (this.getWidth() != width || this.getHeight() != height) {
				this.setSize(width, height);
				tp.setSize(width, height);
			}
			if (this.getX() != x || this.getY() != y) {
				this.setLocation(x, y);
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
			//if (ObjectFrame.this.getX() != x || ObjectFrame.this.getY() != y) {
				//ObjectFrame.this.setLocation(x, y);
			//}
			g.drawImage(WindowManager.a, 0, 0, width, height, x, y, x + width, y + height, null);
		}
	}
}
