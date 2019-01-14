// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import yugecin.opsudance.core.input.MouseEvent;

import org.newdawn.slick.*;

import static itdelatrisu.opsu.GameImage.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class BackButton
{
	/** Skinned back button. */
	private MenuButton backButton;

	/** Colors. */
	private static final Color
		COLOR_PINK = new Color(238, 51, 153),
		COLOR_DARKPINK = new Color(186, 19, 121);

	/** Target duration, in ms, of the button animations. */
	private static final int ANIMATION_TIME = 500;

	/** How much time passed for the animations. */
	private int animationTime;

	/** The size of the slope image (square shape). */
	private int slopeImageSize;

	/** The width of the slope part in the slope image. */
	private int slopeImageSlopeWidth;

	/** The width of the first part of the button. */
	private int firstButtonWidth;

	/** The width of the second part of the button. */
	private int secondButtonSize;

	private boolean wasHoveredLastFrame;
	private boolean isHoveredLastFrame;

	/** The width of the "back" text to draw. */
	private int textWidth;

	/** Y padding for the text and general positioning. */
	private float paddingY;

	/** X padding for the text. */
	private float paddingX;

	/** Y text offset because getHeight is not so accurate. */
	private float textOffset;

	/** The base size of the chevron. */
	private float chevronBaseSize;

	/** The Y position of where the button starts. */
	private int buttonYpos;

	/** Variable holding the slope image. */
	private Image slopeImage;

	/** The real button with, determined by the size and animations. */
	private int realButtonWidth;

	public Listener activeListener;

	public void revalidate()
	{
		textWidth = Fonts.MEDIUM.getWidth("back");
		paddingY = Fonts.MEDIUM.getHeight("back");
		// getHeight doesn't seem to be so accurate
		textOffset = paddingY * 0.264f;
		paddingY *= 0.736f;
		paddingX = paddingY / 2f;
		chevronBaseSize = paddingY * 3f / 2f;
		buttonYpos = height - (int) (paddingY * 4f);
		slopeImageSize = (int) (paddingY * 3f);
		slopeImageSlopeWidth = (int) (slopeImageSize * 0.295f);
		firstButtonWidth = slopeImageSize;
		secondButtonSize = (int) (slopeImageSlopeWidth + paddingX * 2 + textWidth);
		slopeImage = MENU_BACK_SLOPE.getScaledImage(slopeImageSize, slopeImageSize);

		if (GameImage.MENU_BACK.getImages() != null) {
			Animation back = GameImage.MENU_BACK.getAnimation(120);
			backButton = new MenuButton(back, back.getWidth() / 2f, height - (back.getHeight() / 2f));
		} else {
			Image back = GameImage.MENU_BACK.getImage();
			backButton = new MenuButton(back, back.getWidth() / 2f, height - (back.getHeight() / 2f));
		}
	}

	public boolean hasSkinnedVariant()
	{
		return this.backButton != null;
	}

	/**
	 * @param skinned should be {@code true} if the skinned button should be updated,
	 *                default button otherwise
	 */
	public void preRenderUpdate(boolean skinned)
	{
		if (backButton != null && skinned) {
			final boolean wasHovered = this.backButton.isHovered();
			backButton.hoverUpdate(renderDelta, mouseX, mouseY);
			if (!wasHovered && this.backButton.isHovered()) {
				SoundController.playSound(SoundEffect.MENUCLICK);
			}
			return;
		}

		wasHoveredLastFrame = isHoveredLastFrame;
		isHoveredLastFrame = buttonYpos - paddingY < mouseY && mouseX < realButtonWidth;
		displayContainer.suppressHover |= isHoveredLastFrame;
		if (!wasHoveredLastFrame && isHoveredLastFrame) {
			SoundController.playSound(SoundEffect.MENUCLICK);
		}
	}

	/**
	 * unchecked!
	 */
	public void drawSkinned()
	{
		backButton.draw();
	}

	public void drawDefault(Graphics g)
	{
		AnimationEquation anim;
		if (isHoveredLastFrame) {
			if (!wasHoveredLastFrame) {
				animationTime = 0;
			}
			animationTime += renderDelta;
			if (animationTime > ANIMATION_TIME) {
				animationTime = ANIMATION_TIME;
			}
			anim = AnimationEquation.OUT_ELASTIC;
		} else {
			if (wasHoveredLastFrame) {
				animationTime = ANIMATION_TIME;
			}
			animationTime -= renderDelta;
			if (animationTime < 0) {
				animationTime = 0;
			}
			anim = AnimationEquation.IN_ELASTIC;
		}

		// calc chevron size
		float beatProgress = MusicController.getBeatProgressOrDefault(0f);
		if (beatProgress < 0.2f) {
			beatProgress = AnimationEquation.IN_QUINT.calc(beatProgress * 5f);
		} else {
			beatProgress = 1f - AnimationEquation.OUT_QUAD.calc((beatProgress - 0.2f) * 1.25f);
		}
		int chevronSize = (int) (chevronBaseSize - (isHoveredLastFrame ? 6f : 3f) * beatProgress);

		// calc button sizes
		float progress = anim.calc((float) animationTime / ANIMATION_TIME);
		float firstSize = firstButtonWidth + (firstButtonWidth - slopeImageSlopeWidth * 2) * progress;
		float secondSize = secondButtonSize + secondButtonSize * 0.25f * progress;
		realButtonWidth = (int) (firstSize + secondSize);

		// right part
		g.setColor(COLOR_PINK);
		g.fillRect(0, buttonYpos, firstSize + secondSize - slopeImageSlopeWidth, slopeImageSize);
		slopeImage.draw(firstSize + secondSize - slopeImageSize, buttonYpos, COLOR_PINK);

		// left part
		Color hoverColor = new Color(0f, 0f, 0f);
		hoverColor.r = COLOR_PINK.r + (COLOR_DARKPINK.r - COLOR_PINK.r) * progress;
		hoverColor.g = COLOR_PINK.g + (COLOR_DARKPINK.g - COLOR_PINK.g) * progress;
		hoverColor.b = COLOR_PINK.b + (COLOR_DARKPINK.b - COLOR_PINK.b) * progress;
		g.setColor(hoverColor);
		g.fillRect(0, buttonYpos, firstSize - slopeImageSlopeWidth, slopeImageSize);
		slopeImage.draw(firstSize - slopeImageSize, buttonYpos, hoverColor);

		// chevron
		GameImage.MENU_BACK_CHEVRON.getImage().getScaledCopy(chevronSize, chevronSize).drawCentered((firstSize - slopeImageSlopeWidth / 2) / 2, buttonYpos + paddingY * 1.5f);

		// text
		float textY = buttonYpos + paddingY - textOffset;
		float textX = firstSize + (secondSize - paddingX * 2 - textWidth) / 2;
		Fonts.MEDIUM.drawString(textX, textY + 1, "back", Color.black);
		Fonts.MEDIUM.drawString(textX, textY, "back", Color.white);
	}

	private boolean contains(float cx, float cy)
	{
		if (backButton != null && !displayContainer.hasActiveOverlays()) {
			return backButton.contains(cx, cy);
		}
		return buttonYpos - paddingY < cy && cx < realButtonWidth;
	}

	/**
	 * Resets the hover fields for the button.
	 */
	public void resetHover()
	{
		if (backButton != null) {
			backButton.resetHover();
		}
		isHoveredLastFrame = false;
		animationTime = 0;
	}

	public boolean mouseReleased(MouseEvent e)
	{
		if (!displayContainer.disableBackButton &&
			this.contains(e.x, e.y) &&
			this.contains(mousePressX, mousePressY))
		{
			this.activeListener.callback.run();
			return true;
		}
		return false;
	}

	public static class Listener
	{
		public static Listener fromState(Runnable callback)
		{
			return new Listener(callback, false);
		}

		public static Listener fromOverlay(Runnable callback)
		{
			return new Listener(callback, true);
		}

		public final Runnable callback;
		public final boolean isFromOverlay;

		private Listener(Runnable callback, boolean isFromOverlay)
		{
			this.callback = callback;
			this.isFromOverlay = isFromOverlay;
		}
	}
}
