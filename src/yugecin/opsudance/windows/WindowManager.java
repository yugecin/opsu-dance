package yugecin.opsudance.windows;

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

	public static void kickstart()
	{
		cursorFrame = new ObjectFrame();
		WindowManager.addFrame(cursorFrame);
		SwingUtilities.invokeLater(WindowManager::updateWindows);
	}

	public static void swapBuffers()
	{
		BufferedImage tmp = a;
		a = b;
		b = tmp;
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
		SwingUtilities.invokeLater(WindowManager::updateWindows);
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
