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

import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.sbv2.movers.LinearStoryboardMover;
import yugecin.opsudance.ui.SimpleButton;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MoveStoryboard {

	private final SimpleButton btnAddLinear;
	private final SimpleButton btnAddQuadratic;
	private final SimpleButton btnAddCubic;

	private final StoryboardMove dummyMove;

	private int width;

	private StoryboardMove[] moves;

	private GameObject[] gameObjects;
	private int objectIndex;

	private int trackPosition;

	public MoveStoryboard(GameContainer container) {
		this.width = container.getWidth();
		btnAddLinear = new SimpleButton(width - 205, 50, 200, 25, Fonts.SMALL, "add linear", Colors.BLUE_BUTTON, Colors.WHITE_FADE, Colors.WHITE_FADE, Colors.ORANGE_BUTTON);
		btnAddQuadratic = new SimpleButton(width - 205, 80, 200, 25, Fonts.SMALL, "add quadratic", Colors.BLUE_BUTTON, Colors.WHITE_FADE, Colors.WHITE_FADE, Colors.ORANGE_BUTTON);
		btnAddCubic = new SimpleButton(width - 205, 110, 200, 25, Fonts.SMALL, "add cubic", Colors.BLUE_BUTTON, Colors.WHITE_FADE, Colors.WHITE_FADE, Colors.ORANGE_BUTTON);
		dummyMove = (StoryboardMove) Proxy.newProxyInstance(StoryboardMove.class.getClassLoader(), new Class<?>[]{StoryboardMove.class}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return null;
			}
		});
	}

	/**
	 * Get the point at the current time
	 * @param trackPosition current time in ms
	 * @return point calculated by the storyboardmover or null if there is no mover
	 */
	public float[] getPoint(int trackPosition) {
		this.trackPosition = trackPosition;
		if (moves[objectIndex] == null || objectIndex == 0) {
			return null;
		}
		if (trackPosition < gameObjects[objectIndex - 1].getEndTime() || trackPosition > gameObjects[objectIndex].getTime()) {
			return null;
		}
		float t = (float) (trackPosition - gameObjects[objectIndex - 1].getEndTime()) / (gameObjects[objectIndex].getTime() - gameObjects[objectIndex - 1].getEndTime());
		return moves[objectIndex].getPointAt(t);
	}

	public void render(Graphics g) {
		btnAddLinear.render(g);
		btnAddQuadratic.render(g);
		btnAddCubic.render(g);
		if (moves[objectIndex] != null && objectIndex > 0 && trackPosition >= gameObjects[objectIndex - 1].getEndTime() && trackPosition < gameObjects[objectIndex].getTime()) {
			moves[objectIndex].render(g, width);
		}
	}

	public void mousePressed(int x, int y) {
		if (moves[objectIndex] != null) {
			moves[objectIndex].mousePressed(x, y);
		}
	}

	public void mouseReleased(int x, int y) {
		if (moves[objectIndex] != null) {
			moves[objectIndex].mouseReleased(x, y);
		}
		if (objectIndex == 0) {
			return;
		}
		if (btnAddLinear.isHovered()) {
			getCurrentMoveOrCreateNew().add(new LinearStoryboardMover());
		}
		if (btnAddQuadratic.isHovered()) {
		}
		if (btnAddCubic.isHovered()) {
		}
	}

	private StoryboardMove getCurrentMoveOrCreateNew() {
		if (gameObjects[objectIndex].isSlider() && trackPosition > gameObjects[objectIndex].getTime() && trackPosition < gameObjects[objectIndex].getEndTime()) {
			UI.sendBarNotification("wait until the slider ended");
			return dummyMove;
		}
		if (moves[objectIndex] == null) {
			return moves[objectIndex] = new StoryboardMoveImpl(gameObjects[objectIndex - 1].end, gameObjects[objectIndex].start);
		}
		return moves[objectIndex];
	}

	public void update(int delta, int x, int y) {
		btnAddLinear.update(x, y);
		btnAddQuadratic.update(x, y);
		btnAddCubic.update(x, y);
		if (moves[objectIndex] != null) {
			moves[objectIndex].update(delta, x, y);
		}
	}

	public void setGameObjects(GameObject[] gameObjects) {
		this.gameObjects = gameObjects;
		this.moves = new StoryboardMove[gameObjects.length];
	}

	public void setIndex(int objectIndex) {
		this.objectIndex = objectIndex;
	}

}
