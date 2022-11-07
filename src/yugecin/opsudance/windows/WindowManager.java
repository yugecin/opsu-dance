package yugecin.opsudance.windows;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import yugecin.opsudance.core.InstanceContainer;

public class WindowManager
{
	private static List<ObjectFrame> frames = new ArrayList<>();

	public static long lastGLRender;
	public static ObjectFrame cursorFrame, skipbtnframe, hpframe, comboframe, scoreframe;
	public static BufferedImage a, b;
	public static BufferedImage missing;
	public static int offsetX, offsetY;

	public static void kickstart()
	{
		missing = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = missing.createGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, 300, 300);
		g.setColor(Color.magenta);
		for (int i = 0; i < 300; i += 10) {
			for (int j = 0; j < 300; j += 10) {
				if ((i / 10 + j / 10) % 2 == 0) {
					g.fillRect(i, j, 10, 10);
				}
			}
		}
		g.dispose();
		cursorFrame = new ObjectFrame("cursor");
		skipbtnframe = new ObjectFrame("skipbtn");
		comboframe = new ObjectFrame("combo");
		scoreframe = new ObjectFrame("score");
		hpframe = new ObjectFrame("hp");
		WindowManager.addFrame(cursorFrame);
		WindowManager.addFrame(skipbtnframe);
		WindowManager.addFrame(comboframe);
		WindowManager.addFrame(scoreframe);
		WindowManager.addFrame(hpframe);
	}

	public static void swapBuffers()
	{
		BufferedImage tmp = a;
		a = b;
		b = tmp;
	}

	public static void updateNow()
	{
		try {
			SwingUtilities.invokeAndWait(WindowManager::updateNow2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateNow2()
	{
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		offsetX = (ss.width - InstanceContainer.width) / 2;
		offsetY = (ss.height - InstanceContainer.height) / 2;
		for (ObjectFrame frame : frames) {
			frame.update();
		}
		Toolkit.getDefaultToolkit().sync();
	}

	public static void addFrame(ObjectFrame frame)
	{
		frames.add(frame);
	}

	public static void removeFrame(ObjectFrame frame)
	{
		if (frames.remove(frame)) {
			frame.setVisible(false);
		}
	}

	public static boolean hasFrame(ObjectFrame frame)
	{
		return frames.contains(frame);
	}
}
