// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.state.specialstates;

import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import yugecin.opsudance.core.input.*;
import yugecin.opsudance.events.ResolutionChangedListener;

import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static yugecin.opsudance.core.InstanceContainer.*;

public class BubNotifState implements MouseListener, ResolutionChangedListener
{
	public static final int IN_TIME = 633;
	public static final int DISPLAY_TIME = 7000 + IN_TIME;
	public static final int OUT_TIME = 433;
	public static final int TOTAL_TIME = DISPLAY_TIME + OUT_TIME;

	private final LinkedList<Notification> bubbles;

	private int addAnimationTime;
	private int addAnimationHeight;

	public BubNotifState() {
		this.bubbles = new LinkedList<>();
		this.addAnimationTime = IN_TIME;
	}

	public void render(Graphics g) {
		ListIterator<Notification> iter = bubbles.listIterator();
		if (!iter.hasNext()) {
			return;
		}
		addAnimationTime += renderDelta;
		if (addAnimationTime > IN_TIME) {
			finishAddAnimation();
		}
		boolean animateUp = false;
		do {
			Notification next = iter.next();
			if (animateUp && addAnimationTime < IN_TIME) {
				next.y = next.baseY - (int) (addAnimationHeight * AnimationEquation.OUT_QUINT.calc((float) addAnimationTime / IN_TIME));
			}
			if (next.render(g, mouseX, mouseY, renderDelta)) {
				iter.remove();
			}
			animateUp = true;
		} while (iter.hasNext());
	}

	private void calculatePositions() {
		// if width is 0, attempting to wrap it will result in infinite loop
		Notification.width = Math.max(50, (int) (width * 0.1703125f));
		Notification.baseLine = (int) (height * 0.9645f);
		Notification.paddingY = (int) (height * 0.0144f);
		Notification.finalX = width - Notification.width - (int) (width * 0.01);
		Notification.fontPaddingX = (int) (Notification.width * 0.02f);
		Notification.fontPaddingY = (int) (Fonts.SMALLBOLD.getLineHeight() / 4f);
		Notification.lineHeight = Fonts.SMALLBOLD.getLineHeight();
		if (bubbles.isEmpty()) {
			return;
		}
		finishAddAnimation();
		int y = Notification.baseLine;
		for (Notification bubble : bubbles) {
			bubble.recalculateDimensions();
			y -= bubble.height;
			bubble.baseY = bubble.y = y;
			y -= Notification.paddingY;
		}
	}

	private void finishAddAnimation() {
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
	public void sendf(Color borderColor, String format, Object... args) {
		this.send(borderColor, new Formatter().format(format, args).toString());
	}

	public void send(Color borderColor, String message) {
		finishAddAnimation();
		Notification newBubble = new Notification(message, borderColor);
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

	@Override
	public void onResolutionChanged(int w, int h) {
		calculatePositions();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.x < Notification.finalX) {
			return;
		}
		for (Notification bubble : bubbles) {
			if (bubble.mouseReleased(e.x, e.y)) {
				e.consume();
				return;
			}
		}
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
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
			this.lines = Fonts.wrap(Fonts.SMALLBOLD, message, (int) (width * 0.96f), true);
			this.height = (int) (Fonts.SMALLBOLD.getLineHeight() * (lines.size() + 0.5f));
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
				Fonts.SMALLBOLD.drawString(x + fontPaddingX, y, line, textColor);
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
			float hoverProgress = (float) hoverTime / HOVER_ANIM_TIME;
			borderColor.r = targetBorderColor.r + (0.977f - targetBorderColor.r) * hoverProgress;
			borderColor.g = targetBorderColor.g + (0.977f - targetBorderColor.g) * hoverProgress;
			borderColor.b = targetBorderColor.b + (0.977f - targetBorderColor.b) * hoverProgress;
			if (timeShown < BubNotifState.IN_TIME) {
				float progress = (float) timeShown / BubNotifState.IN_TIME;
				this.x = finalX + (int) ((1 - AnimationEquation.OUT_BACK.calc(progress)) * width / 2);
				textColor.a = borderColor.a = bgcol.a = progress;
				bgcol.a = borderColor.a * 0.8f;
				return;
			}
			x = Notification.finalX;
			if (timeShown > BubNotifState.DISPLAY_TIME) {
				isFading = true;
				float progress = (float) (timeShown - BubNotifState.DISPLAY_TIME) / BubNotifState.OUT_TIME;
				textColor.a = borderColor.a = 1f - progress;
				bgcol.a = borderColor.a * 0.8f;
			}
		}

		private boolean mouseReleased(int x, int y) {
			if (!isFading && isMouseHovered(x, y)) {
				timeShown = BubNotifState.DISPLAY_TIME;
				return true;
			}
			return false;
		}

		private boolean isMouseHovered(int x, int y) {
			return this.x <= x && x < this.x + width && this.y <= y && y <= this.y + this.height;
		}

	}
}
