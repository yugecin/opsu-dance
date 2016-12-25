/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.sbv2;

import itdelatrisu.opsu.objects.curves.Vec2f;
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.sbv2.movers.StoryboardMover;

import java.util.ArrayList;
import java.util.List;

public class StoryboardMoveImpl implements StoryboardMove {

	private static final int POINTSIZE = 6;

	private int screenWidth;

	private Vec2f start;
	private Vec2f end;
	private List<StoryboardMover> movers;
	private List<Vec2f> midPoints;

	private StoryboardMover nextMover;
	private Vec2f currentPoint;
	private StoryboardMover prevMover;

	private float totalLength;

	private int recalculateDelay;

	public StoryboardMoveImpl(Vec2f start, Vec2f end, int screenWidth) {
		this.start = start;
		this.end = end;
		this.screenWidth = screenWidth;
		movers = new ArrayList<>();
		midPoints = new ArrayList<>();
		recalculateDelay = 700;
	}

	@Override
	public int getAmountOfMovers() {
		return movers.size();
	}

	@Override
	public void add(StoryboardMover mover) {
		mover.end = end;
		if (movers.size() == 0) {
			mover.start = start;
			mover.recalculateLength();
			totalLength += mover.getLength();
		} else {
			StoryboardMover lastMover = movers.get(movers.size() - 1);
			Vec2f mid = new Vec2f(
				(lastMover.start.x + lastMover.end.x) / 2f,
				(lastMover.start.y + lastMover.end.y) / 2f
			);
			midPoints.add(mid);
			lastMover.end = mid;
			totalLength -= lastMover.getLength();
			lastMover.recalculateLength();
			totalLength += lastMover.getLength();
			mover.start = mid;
		}
		movers.add(mover);
		recalculateTimes();
	}

	@Override
	public float[] getPointAt(float t) {
		if (movers.size() == 0) {
			return new float[] { end.x, end.y };
		}
		float cumulativeTime = 0f;
		for (StoryboardMover mover : movers) {
			cumulativeTime += mover.timeLengthPercentOfTotalTime;
			if (cumulativeTime > t) {
				return mover.getPointAt((t - (cumulativeTime - mover.timeLengthPercentOfTotalTime)) / mover.timeLengthPercentOfTotalTime);
			}
		}
		return new float[] { end.x, end.y };
	}

	@Override
	public void update(int delta, int x, int y) {
		if (currentPoint != null) {
			moveCurrentPoint(x, y);
			recalculateDelay -= delta;
			if (recalculateDelay < 0) {
				recalculateDelay = 700;
				recalculateLengths();
				recalculateTimes();
			}
		}
	}

	@Override
	public void mousePressed(int x, int y) {
		int i = 0;
		for (Vec2f point : midPoints) {
			if (point.x - POINTSIZE <= x && x <= point.x + POINTSIZE && point.y - POINTSIZE <= y && y <= point.y + POINTSIZE) {
				currentPoint = point;
				prevMover = movers.get(i);
				nextMover = movers.get(i + 1);
				break;
			}
			i++;
		}
	}

	@Override
	public void mouseReleased(int x, int y) {
		int posY = 200;
		if (currentPoint == null) {
			for (int i = 0; i < movers.size(); i++) {
				int dif = posY;
				posY += Fonts.SMALL.getLineHeight() * 1.1f;
				dif = posY - dif;
				if (screenWidth - 20 <= x && x <= screenWidth - 10 && posY - dif / 2 - 5 <= y && y <= posY - dif / 2 + 5) {
					if (movers.size() == 1) {
						movers.clear();
						return;
					}
					StoryboardMover mover = movers.get(i);
					if (i == movers.size() - 1) {
						midPoints.remove(i - 1);
						movers.get(i - 1).end = mover.end;
					} else {
						midPoints.remove(i);
						movers.get(i + 1).start = mover.start;
					}
					movers.remove(i);
					totalLength -= mover.getLength();
					recalculateTimes();
				}
			}
		} else {
			moveCurrentPoint(x, y);
			recalculateLengths();
			recalculateTimes();
			currentPoint = null;
		}
		recalculateDelay = 700;
	}

	@Override
	public void recalculateTimes() {
		for (StoryboardMover mover : movers) {
			mover.timeLengthPercentOfTotalTime = mover.getLength() / totalLength;
		}
	}

	private void recalculateLengths() {
		totalLength -= prevMover.getLength() + nextMover.getLength();
		prevMover.recalculateLength();
		nextMover.recalculateLength();
		totalLength += prevMover.getLength() + nextMover.getLength();
		recalculateTimes();
	}

	private void moveCurrentPoint(int x, int y) {
		currentPoint.x = x;
		currentPoint.y = y;
		prevMover.end = currentPoint;
		nextMover.start = currentPoint;
	}

	@Override
	public void render(Graphics g) {
		g.setColor(Color.red);
		int y = 200;
		for (StoryboardMover mover : movers) {
			mover.render(g);
			String text = mover.toString();
			Fonts.SMALL.drawString(screenWidth - Fonts.SMALL.getWidth(text) - 30, y, text);
			int dif = y;
			y += Fonts.SMALL.getLineHeight() * 1.1f;
			dif = y - dif;
			g.fillRect(screenWidth - 20, y - dif / 2 - 5, 10, 10);
		}
		g.setColor(Color.cyan);
		for (Vec2f point : midPoints) {
			g.fillRect(point.x - POINTSIZE, point.y - POINTSIZE, POINTSIZE * 2 + 1, POINTSIZE * 2 + 1);
		}
	}

}
