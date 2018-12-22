// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.render;

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import yugecin.opsudance.skinning.SkinService;

import static itdelatrisu.opsu.GameImage.*;
import static yugecin.opsudance.options.Options.*;

public class GameObjectRenderer
{
	public GameData gameData;

	public float circleDiameter;
	public int circleDiameterInt;

	private Image hitcircle;
	private Image hitcircleOverlay;
	private Image approachCircle;

	public void initForGame(GameData gameData, float circleDiameter) {
		this.gameData = gameData;
		this.circleDiameter = circleDiameter * HitObject.getXMultiplier();  // convert from Osupixels (640x480)
		this.circleDiameterInt = (int) this.circleDiameter;
		GameImage.HITCIRCLE.setImage(GameImage.HITCIRCLE.getImage().getScaledCopy(circleDiameterInt, circleDiameterInt));
		GameImage.HITCIRCLE_OVERLAY.setImage(GameImage.HITCIRCLE_OVERLAY.getImage().getScaledCopy(circleDiameterInt, circleDiameterInt));
		GameImage.APPROACHCIRCLE.setImage(GameImage.APPROACHCIRCLE.getImage().getScaledCopy(circleDiameterInt, circleDiameterInt));
		hitcircle = GameImage.HITCIRCLE.getImage();
		hitcircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage();
		approachCircle = GameImage.APPROACHCIRCLE.getImage();
	}

	public void initForFrame() {
		if (!OPTION_DANCING_CIRCLES.state) {
			return;
		}
		final float position = MusicController.getBeatProgressOrDefault(0f);
		int size = circleDiameterInt + (int) (circleDiameter * OPTION_DANCING_CIRCLES_MULTIPLIER.val / 1000f * AnimationEquation.IN_OUT_QUAD.calc(position));
		hitcircle = GameImage.HITCIRCLE.getImage().getScaledCopy(size, size);
		hitcircleOverlay = GameImage.HITCIRCLE_OVERLAY.getImage().getScaledCopy(size, size);
	}

	public void renderHitCircle(float x, float y, Color color, int comboNumber, float comboNumberAlpha) {
		renderHitCircleOnly(x, y, color);
		boolean overlayAboveNumber = SkinService.skin.isHitCircleOverlayAboveNumber();
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
			float scale = HITCIRCLE.getWidth() * 0.40f / gameData.getDefaultSymbolImage(0).getHeight();
			gameData.drawSymbolNumber(number, x, y, scale, alpha);
		}
	}

	public void renderApproachCircle(float x, float y, Color color, float approachScale) {
		if (!GameMod.HIDDEN.isActive() && OPTION_DANCE_DRAW_APPROACH.state) {
			approachCircle.getScaledCopy(approachScale).drawCentered(x, y, color);
		}
	}
}
