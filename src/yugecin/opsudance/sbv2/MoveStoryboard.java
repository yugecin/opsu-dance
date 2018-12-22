/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016-2018 yugecin
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
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import yugecin.opsudance.core.input.*;
import yugecin.opsudance.core.state.OverlayOpsuState;
import yugecin.opsudance.sbv2.movers.CubicStoryboardMover;
import yugecin.opsudance.sbv2.movers.LinearStoryboardMover;
import yugecin.opsudance.sbv2.movers.QuadraticStoryboardMover;
import yugecin.opsudance.ui.SimpleButton;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static yugecin.opsudance.core.InstanceContainer.*;

public class MoveStoryboard extends OverlayOpsuState
{
	private SimpleButton btnAddLinear;
	private SimpleButton btnAddQuadratic;
	private SimpleButton btnAddCubic;

	private SimpleButton btnAnimLin;
	private SimpleButton btnAnimMid;
	private SimpleButton btnAnimCub;

	private final StoryboardMove dummyMove;

	private StoryboardMove[] moves;

	private GameObject[] gameObjects;
	private int objectIndex;

	private int trackPosition;

	public MoveStoryboard() {
		dummyMove = (StoryboardMove) Proxy.newProxyInstance(StoryboardMove.class.getClassLoader(), new Class<?>[]{StoryboardMove.class}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return null;
			}
		});
	}

	@Override
	public void revalidate() {
		super.revalidate();

		btnAddLinear = new SimpleButton(width - 205, 50, 200, 25, Fonts.SMALL, "add linear", Colors.BLUE_BUTTON, Colors.WHITE_FADE, Colors.WHITE_FADE, Colors.ORANGE_BUTTON);
		btnAddQuadratic = new SimpleButton(width - 205, 80, 200, 25, Fonts.SMALL, "add quadratic", Colors.BLUE_BUTTON, Colors.WHITE_FADE, Colors.WHITE_FADE, Colors.ORANGE_BUTTON);
		btnAddCubic = new SimpleButton(width - 205, 110, 200, 25, Fonts.SMALL, "add cubic", Colors.BLUE_BUTTON, Colors.WHITE_FADE, Colors.WHITE_FADE, Colors.ORANGE_BUTTON);
		btnAnimLin = new SimpleButton(width - 250, 50, 40, 25, Fonts.SMALL, "lin", Color.blue, Color.white, Color.white, Color.orange);
		btnAnimMid = new SimpleButton(width - 250, 80, 40, 25, Fonts.SMALL, "mid", Color.blue, Color.white, Color.white, Color.orange);
		btnAnimCub = new SimpleButton(width - 250, 110, 40, 25, Fonts.SMALL, "cub", Color.blue, Color.white, Color.white, Color.orange);
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

	@Override
	protected void onPreRenderUpdate() {
		int x = mouseX;
		int y = mouseY;
		btnAddLinear.update(x, y);
		btnAddQuadratic.update(x, y);
		btnAddCubic.update(x, y);
		btnAnimLin.update(x, y);
		btnAnimMid.update(x, y);
		btnAnimCub.update(x, y);
		if (moves[objectIndex] != null) {
			moves[objectIndex].update(renderDelta, x, y);
		}
	}

	@Override
	protected void onRender(Graphics g) {
		btnAddLinear.render(g);
		btnAddQuadratic.render(g);
		btnAddCubic.render(g);
		btnAnimLin.render(g);
		btnAnimMid.render(g);
		btnAnimCub.render(g);
		if (moves[objectIndex] != null && objectIndex > 0 && trackPosition >= gameObjects[objectIndex - 1].getEndTime() && trackPosition < gameObjects[objectIndex].getTime()) {
			moves[objectIndex].render(g);
		}
	}

	@Override
	protected void onKeyPressed(KeyEvent e)
	{
		e.consume();
	}

	@Override
	protected void onKeyReleased(KeyEvent e)
	{
		e.consume();
	}

	@Override
	protected void onMouseWheelMoved(MouseWheelEvent e)
	{
		e.consume();
	}

	@Override
	protected void onMousePressed(MouseEvent e)
	{
		if (moves[objectIndex] != null) {
			moves[objectIndex].mousePressed(e.x, e.y);
		}
		e.consume();
	}

	@Override
	protected void onMouseReleased(MouseEvent e)
	{
		if (moves[objectIndex] != null) {
			moves[objectIndex].mouseReleased(e.x, e.y);
			if (moves[objectIndex].getAmountOfMovers() == 0) {
				moves[objectIndex] = null;
			}
		}
		if (objectIndex == 0) {
			e.consume();
			return;
		}
		if (btnAddLinear.isHovered()) {
			getCurrentMoveOrCreateNew().add(new LinearStoryboardMover());
		}
		if (btnAddQuadratic.isHovered()) {
			getCurrentMoveOrCreateNew().add(new QuadraticStoryboardMover());
		}
		if (btnAddCubic.isHovered()) {
			getCurrentMoveOrCreateNew().add(new CubicStoryboardMover());
		}
		if (btnAnimLin.isHovered()) {
			getCurrentMoveOrDummy().setAnimationEquation(AnimationEquation.LINEAR);
		}
		if (btnAnimMid.isHovered()) {
			getCurrentMoveOrDummy().setAnimationEquation(AnimationEquation.IN_OUT_CIRC);
		}
		if (btnAnimCub.isHovered()) {
			getCurrentMoveOrDummy().setAnimationEquation(AnimationEquation.IN_OUT_EASE_MIDDLE);
		}
		e.consume();
	}

	@Override
	protected void onMouseDragged(MouseDragEvent e) {
		e.consume();
	}

	private StoryboardMove getCurrentMoveOrCreateNew() {
		if (gameObjects[objectIndex].isSlider() && trackPosition > gameObjects[objectIndex].getTime() && trackPosition < gameObjects[objectIndex].getEndTime()) {
			barNotifs.send("Wait until the slider ended");
			return dummyMove;
		}
		if (moves[objectIndex] == null) {
			return moves[objectIndex] = new StoryboardMoveImpl(gameObjects[objectIndex - 1].end, gameObjects[objectIndex].start, width);
		}
		return moves[objectIndex];
	}

	private StoryboardMove getCurrentMoveOrDummy() {
		if (objectIndex < 0 || gameObjects.length <= objectIndex || moves[objectIndex] == null) {
			return dummyMove;
		}
		return moves[objectIndex];
	}

	public void setGameObjects(GameObject[] gameObjects) {
		this.gameObjects = gameObjects;
		this.moves = new StoryboardMove[gameObjects.length];
	}

	public void setIndex(int objectIndex) {
		this.objectIndex = objectIndex;
	}
}
