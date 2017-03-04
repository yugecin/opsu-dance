/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
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
package yugecin.opsudance.render;

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.ui.Colors;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.inject.Inject;

public class GameObjectRenderer {

	@Inject
	private DisplayContainer displayContainer;

	private GameData gameData;

	private float circleDiameter;

	private Image hitcircle;
	private Image hitcircleOverlay;
	private Image approachCircle;

	@Deprecated
	public static GameObjectRenderer instance;

	public GameObjectRenderer() {
		instance = this; // TODO get rid of this
	}

	public void initForGame(GameData gameData, float circleDiameter) {
		this.gameData = gameData;
		this.circleDiameter = circleDiameter * HitObject.getXMultiplier();  // convert from Osupixels (640x480)
		int diameterInt = (int) this.circleDiameter;
		GameImage.HITCIRCLE.setImage(GameImage.HITCIRCLE.getImage().getScaledCopy(diameterInt, diameterInt));
		GameImage.HITCIRCLE_OVERLAY.setImage(GameImage.HITCIRCLE_OVERLAY.getImage().getScaledCopy(diameterInt, diameterInt));
		GameImage.APPROACHCIRCLE.setImage(GameImage.APPROACHCIRCLE.getImage().getScaledCopy(diameterInt, diameterInt));
		hitcircle = GameImage.HITCIRCLE.getImage();
		hitcircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();
		approachCircle = GameImage.APPROACHCIRCLE.getImage();
	}

	public float getCircleDiameter() {
		return circleDiameter;
	}

	public void setGameData(GameData gameData) {
		this.gameData = gameData;
	}

	public void renderHitCircle(float x, float y, Color color, int comboNumber, float comboNumberAlpha) {
		renderHitCircleOnly(x, y, color);
		boolean overlayAboveNumber = Options.getSkin().isHitCircleOverlayAboveNumber();
		if (!overlayAboveNumber) {
			renderHitCircleOverlayOnly(x, y, Colors.WHITE_FADE);
		}
		renderComboNumberOnly(x, y, comboNumber, comboNumberAlpha);
		if (overlayAboveNumber) {
			renderHitCircleOverlayOnly(x, y, Colors.WHITE_FADE);
		}
	}

	public void renderHitCircleOnly(float x, float y, Color color) {
		hitcircle.drawCentered(x, y, color);
	}

	public void renderHitCircleOverlayOnly(float x, float y, Color color) {
		hitcircleOverlay.drawCentered(x, y, color);
	}

	public void renderComboNumberOnly(float x, float y, int number, float alpha) {
		if (number > 0) {
			gameData.drawSymbolNumber(number, x, y, GameImage.HITCIRCLE.getImage().getWidth() * 0.40f / gameData.getDefaultSymbolImage(0).getHeight(), alpha);
		}
	}

	public void renderApproachCircle(float x, float y, Color color, float approachScale) {
		if (!GameMod.HIDDEN.isActive() && Options.isDrawApproach()) {
			approachCircle.getScaledCopy(approachScale).drawCentered(x, y, color);
		}
	}

}
