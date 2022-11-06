package yugecin.opsudance.windows;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

public class WindowManager
{
	private static List<ObjectFrame> frames = new ArrayList<>();
	private static ObjectFrame[] frameArray = new ObjectFrame[20];

	public static long lastGLRender;
	public static ObjectFrame cursorFrame;
	public static BufferedImage a, b;
	public static BufferedImage missing;

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
		WindowManager.addFrame(cursorFrame);
		//SwingUtilities.invokeLater(WindowManager::updateWindows);
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
		synchronized(frames) {
			frameArray = frames.toArray(frameArray);
		}
		for (ObjectFrame frame : frameArray) {
			if (frame == null) {
				break;
			}
			frame.update();
		}
	}

	public static void updateWindows()
	{
		synchronized(frames) {
			frameArray = frames.toArray(frameArray);
		}
		for (ObjectFrame frame : frameArray) {
			if (frame == null) {
				break;
			}
			frame.update();
		}
		//SwingUtilities.invokeLater(WindowManager::updateWindows);
	}

	public static void addFrame(ObjectFrame frame)
	{
		synchronized(frames) {
			frames.add(frame);
		}
	}

	public static void removeFrame(ObjectFrame frame)
	{
		synchronized(frames) {
			frames.remove(frame);
		}
	}
}
