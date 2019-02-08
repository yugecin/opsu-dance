// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.state.specialstates;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import yugecin.opsudance.core.input.*;
import yugecin.opsudance.events.ResolutionChangedListener;

import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static itdelatrisu.opsu.Utils.clamp;
import static itdelatrisu.opsu.ui.Fonts.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class BubNotifState implements ResolutionChangedListener
{
	public static final int IN_TIME = 633;
	public static final int DISPLAY_TIME = 7000 + IN_TIME;
	public static final int OUT_TIME = 433;
	public static final int TOTAL_TIME = DISPLAY_TIME + OUT_TIME;

	/**
	 * access should be synchronized because {@link #send} and {@link #sendf} can be called
	 * from any thread
	 */
	private final LinkedList<Notification> bubbles;

	private int addAnimationTime;
	private int addAnimationHeight;

	public BubNotifState() {
		this.bubbles = new LinkedList<>();
		this.addAnimationTime = IN_TIME;
	}

	public void render(Graphics g)
	{
		if (bubbles.isEmpty()) {
			return;
		}
		synchronized (this.bubbles) {
			ListIterator<Notification> iter = bubbles.listIterator();
			addAnimationTime += renderDelta;
			if (addAnimationTime > IN_TIME) {
				finishAddAnimation();
			}
			boolean animateUp = false;
			do {
				Notification next = iter.next();
				if (animateUp && addAnimationTime < IN_TIME) {
					float progress = addAnimationTime * 2f / IN_TIME;
					progress = OUT_QUAD.calc(clamp(progress, 0f, 1f));
					next.y = next.baseY - (int) (addAnimationHeight * progress);
				}
				if (next.render(g, mouseX, mouseY, renderDelta)) {
					iter.remove();
				}
				animateUp = true;
			} while (iter.hasNext());
		}
	}

	@Override
	public void onResolutionChanged(int w, int h)
	{
		// if width is 0, attempting to wrap it will result in infinite loop
		Notification.width = Math.max(50, (int) (width * 0.25));
		Notification.baseLine = (int) (height * 0.9645f);
		Notification.paddingY = (int) (height * 0.0144f);
		Notification.finalX = width - Notification.width - (int) (width * 0.01);
		Notification.fontPaddingX = (int) (Notification.width * 0.02f);
		Notification.fontPaddingY = (int) (SMALLBOLD.getLineHeight() / 4f);
		Notification.lineHeight = SMALLBOLD.getLineHeight();
		if (bubbles.isEmpty()) {
			return;
		}
		synchronized (this.bubbles) { 
			finishAddAnimation();
			int y = Notification.baseLine;
			for (Notification bubble : bubbles) {
				bubble.recalculateDimensions();
				y -= bubble.height;
				bubble.baseY = bubble.y = y;
				y -= Notification.paddingY;
			}
		}
	}

	/**
	 * synchronize on {@code this.bubbles} before calling!
	 */
	private void finishAddAnimation()
	{
		if (bubbles.isEmpty()) {
			addAnimationHeight = 0;
			addAnimationTime = IN_TIME;
			return;
		}
		ListIterator<Notification> iter = bubbles.listIterator();
		iter.next();
		while (iter.hasNext()) {
			Notification bubble = iter.next();
			bubble.y = bubble.baseY - addAnimationHeight;
			bubble.baseY = bubble.y;
		}
		addAnimationHeight = 0;
		addAnimationTime = IN_TIME;
	}

	@SuppressWarnings("resource")
	public void sendf(Color borderColor, String format, Object... args)
	{
		this.send(borderColor, new Formatter().format(format, args).toString());
	}

	public void send(Color borderColor, String message)
	{
		finishAddAnimation();
		Notification newBubble = new Notification(message, borderColor);
		synchronized (this.bubbles) { 
			bubbles.add(0, newBubble);
			addAnimationTime = 0;
			addAnimationHeight = newBubble.height + Notification.paddingY;
			ListIterator<Notification> iter = bubbles.listIterator();
			iter.next();
			while (iter.hasNext()) {
				Notification next = iter.next();
				next.baseY = next.y;
			}
		}
	}

	public boolean mouseReleased(MouseEvent e)
	{
		if (e.x < Notification.finalX) {
			return false;
		}
		if (!this.bubbles.isEmpty()) {
			synchronized (this.bubbles) { 
				for (Notification bubble : bubbles) {
					if (bubble.mouseReleased(e.x, e.y)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static class Notification {

		private final static int HOVER_ANIM_TIME = 150;

		private static int width;
		private static int finalX;
		private static int baseLine;
		private static int fontPaddingX;
		private static int fontPaddingY;
		private static int lineHeight;
		private static int paddingY;

		private final Color bgcol;
		private final Color textColor;
		private final Color borderColor;
		private final Color targetBorderColor;

		private int timeShown;
		private int x;
		private int y;
		private int baseY;
		private int height;

		private final String message;
		private List<String> lines;

		private boolean isFading;
		private int hoverTime;

		private Notification(String message, Color borderColor) {
			this.message = message;
			recalculateDimensions();
			this.targetBorderColor = borderColor;
			this.borderColor = new Color(borderColor);
			this.textColor = new Color(Color.white);
			this.bgcol = new Color(Color.black);
			this.y = baseLine - height;
			this.baseY = this.y;
		}

		private void recalculateDimensions() {
			this.lines = wrap(SMALLBOLD, message, (int) (width * 0.96f), true);
			this.height = (int) (SMALLBOLD.getLineHeight() * (lines.size() + 0.5f));
		}

		/**
		 * @return true if this notification expired
		 */
		private boolean render(Graphics g, int mouseX, int mouseY, int delta) {
			timeShown += delta;
			processAnimations(isMouseHovered(mouseX, mouseY), delta);
			g.setColor(bgcol);
			g.fillRoundRect(x, y, width, height, 6);
			g.setLineWidth(2f);
			g.setColor(borderColor);
			g.drawRoundRect(x, y, width, height, 6);
			int y = this.y + fontPaddingY;
			for (String line : lines) {
				SMALLBOLD.drawString(x + fontPaddingX, y, line, textColor);
				y += lineHeight;
			}
			return timeShown > BubNotifState.TOTAL_TIME;
		}

		private void processAnimations(boolean mouseHovered, int delta) {
			if (mouseHovered) {
				hoverTime = Math.min(HOVER_ANIM_TIME, hoverTime + delta);
			} else {
				hoverTime = Math.max(0, hoverTime - delta);
			}
			float hp = (float) hoverTime / HOVER_ANIM_TIME;
			borderColor.r = targetBorderColor.r + (0.977f - targetBorderColor.r) * hp;
			borderColor.g = targetBorderColor.g + (0.977f - targetBorderColor.g) * hp;
			borderColor.b = targetBorderColor.b + (0.977f - targetBorderColor.b) * hp;
			if (timeShown < IN_TIME) {
				float p = (float) timeShown / IN_TIME;
				this.x = finalX + (int) ((1 - this.animateX(p)) * width / 2);
				final float alpha = clamp(p * 1.8f, 0f, 1f);
				textColor.a = borderColor.a = bgcol.a = alpha;
				bgcol.a = borderColor.a * 0.8f;
				return;
			}
			x = Notification.finalX;
			if (timeShown > DISPLAY_TIME) {
				isFading = true;
				float progress = (float) (timeShown - DISPLAY_TIME) / OUT_TIME;
				textColor.a = borderColor.a = 1f - progress;
				bgcol.a = borderColor.a * 0.8f;
			}
		}

		/**
		 * ease X position
		 * like {@link itdelatrisu.opsu.ui.animations.AnimationEquation#OUT_ELASTIC},
		 * but less intensive
		 */
		private float animateX(float t)
		{
			if (t == 0 || t == 1)
				return t;
			float period = .5f;
			return
				(float) Math.pow(2, -13 * t)
				* (float) Math.sin((t - period / 4)
				* (Math.PI * 3) / period) + 1;
		}

		private boolean mouseReleased(int x, int y) {
			if (!isFading && isMouseHovered(x, y)) {
				timeShown = DISPLAY_TIME;
				return true;
			}
			return false;
		}

		private boolean isMouseHovered(int x, int y)
		{
			return
				this.x <= x && x < this.x + width &&
				this.y <= y && y <= this.y + this.height;
		}

	}
}
