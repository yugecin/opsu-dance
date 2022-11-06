package yugecin.opsudance.windows;

import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

import yugecin.opsudance.core.InstanceContainer;

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
			/*
			if (this.getX() != x || this.getY() != y) {
				this.setLocation(x, y);
			}
			*/
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
			if (ObjectFrame.this.getX() != x || ObjectFrame.this.getY() != y) {
				ObjectFrame.this.setLocation(x, y);
			}
			g.drawImage(WindowManager.a, 0, 0, width, height, x, y, x + width, y + height, null);
			if (y < 0) {
				int h = -y;
				int b = y % 20 + 20;
				int a = x % 20;
				g.drawImage(WindowManager.missing, 0, 0, width, h, a, b, width + a, b + h, null);
			} else if (y + height > InstanceContainer.height) {
				int h = y + height - InstanceContainer.height;
				int a = x % 20;
				g.drawImage(WindowManager.missing, 0, height - h, width, height, a, 0, width + a, h, null);
			}
			if (x < 0) {
				int w = -x;
				int a = x % 20 + 20;
				int b = y % 20;
				g.drawImage(WindowManager.missing, 0, 0, w, height, a, b, a + w, height + b, null);
			} else if (x + width > InstanceContainer.width) {
				int w = x + width - InstanceContainer.width;
				int b = y % 20;
				g.drawImage(WindowManager.missing, width - w, 0, width, height, 0, b, w, height + b, null);
			}
		}
	}
}
