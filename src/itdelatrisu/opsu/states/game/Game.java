/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.states.game;

import itdelatrisu.opsu.*;
import itdelatrisu.opsu.audio.HitSound;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapGroup;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.BeatmapSortOrder;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.beatmap.TimingPoint;
import itdelatrisu.opsu.db.BeatmapDB;
import itdelatrisu.opsu.db.ScoreDB;
import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.DummyObject;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.objects.Spinner;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.objects.curves.Vec2f;
import itdelatrisu.opsu.render.FrameBufferCache;
import itdelatrisu.opsu.replay.PlaybackSpeed;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;
import itdelatrisu.opsu.ui.*;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import itdelatrisu.opsu.ui.cursor.CursorImpl;

import java.io.File;
import java.io.StringWriter;
import java.util.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.*;
import yugecin.opsudance.core.Constants;
import yugecin.opsudance.core.input.*;
import yugecin.opsudance.core.state.ComplexOpsuState;
import yugecin.opsudance.objects.curves.FakeCombinedCurve;
import yugecin.opsudance.options.OptionGroups;
import yugecin.opsudance.sbv2.MoveStoryboard;
import yugecin.opsudance.skinning.SkinService;
import yugecin.opsudance.ui.OptionsOverlay;
import yugecin.opsudance.ui.StoryboardOverlay;
import yugecin.opsudance.ui.cursor.colors.CursorColorManager;
import yugecin.opsudance.utils.GLHelper;

import static itdelatrisu.opsu.GameImage.*;
import static itdelatrisu.opsu.ui.Colors.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static java.lang.System.arraycopy;
import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.windows.WindowManager.*;

/**
 * "Game" state.
 */
public class Game extends ComplexOpsuState {

	public static boolean isInGame; // TODO delete this when #79 is fixed
	/** Game restart states. */
	public enum RestartReason {
		/** First time loading the song. */
		NEWGAME,
		/** Manual retry. */
		USER,
		/** Replay. */
		WATCHREPLAY,
		/** Health is zero: no-continue/force restart. */
		LOSE
	}

	/** Music fade-out time, in milliseconds. */
	private static final int MUSIC_FADEOUT_TIME = 2000;

	/** Game element fade-out time, in milliseconds, when the game ends. */
	private static final int FINISHED_FADEOUT_TIME = 400;

	private static final int LOSE_FADEOUT_TIME = 2000;

	/** Maximum rotation, in degrees, over fade out upon death. */
	private static final float MAX_ROTATION = 90f;

	/** The duration of the score changing animation. */
	private static final float SCOREBOARD_ANIMATION_TIME = 500f;

	/** The time the scoreboard takes to fade in. */
	private static final float SCOREBOARD_FADE_IN_TIME = 300f;

	/** Minimum time before start of song, in milliseconds, to process skip-related actions. */
	private static final int SKIP_OFFSET = 2000;

	/** Tolerance in case if hit object is not snapped to the grid. */
	private static final float STACK_LENIENCE = 3f;

	/** Stack time window of the previous object, in ms. */
	private static final int STACK_TIMEOUT = 1000;

	/** Stack position offset modifier. */
	private static final float STACK_OFFSET_MODIFIER = 0.05f;

	/** The associated beatmap. */
	private Beatmap beatmap;

	/** The associated GameData object. */
	private GameData data;

	/** Current hit object index (in both hit object arrays). */
	private int objectIndex = 0;

	/** The map's game objects, indexed by objectIndex. */
	private GameObject[] gameObjects;

	/** Delay time, in milliseconds, before song starts. */
	private int leadInTime;

	/** Hit object approach time, in milliseconds. */
	private int approachTime;

	/** The amount of time for hit objects to fade in, in milliseconds. */
	private int fadeInTime;

	/** Decay time for hit objects in the "Hidden" mod, in milliseconds. */
	private int hiddenDecayTime;

	/** Time before the hit object time by which the objects have completely faded in the "Hidden" mod, in milliseconds. */
	private int hiddenTimeDiff;

	/** Time offsets for obtaining each hit result (indexed by HIT_* constants). */
	private int[] hitResultOffset;

	private RestartReason restartReason;

	/** Current break index in breaks ArrayList. */
	private int breakIndex;

	/** Break start time (0 if not in break). */
	private int breakTime = 0;

	/** Whether the break sound has been played. */
	private boolean breakSound;

	/** Skip button (displayed at song start, when necessary). */
	private MenuButton skipButton;

	/** Current timing point index in timingPoints ArrayList. */
	private int timingPointIndex;

	/** Current beat lengths (base value and inherited value). */
	private float beatLengthBase, beatLength;

	/** Whether the countdown sound has been played. */
	private boolean
		countdownReadySound, countdown3Sound, countdown1Sound,
		countdown2Sound, countdownGoSound;

	/** Whether a checkpoint has been loaded during this game. */
	private boolean checkpointLoaded = false;

	/** Number of deaths, used if "Easy" mod is enabled. */
	private byte deaths = 0;

	/** Track position at death, used if "Easy" mod is enabled. */
	private int deathTime = -1;

	/** System time position at death. */
	private long failTime;

	/** Track time position at death. */
	private int failTrackTime;

	/** Rotations for game objects at death. */
	private IdentityHashMap<GameObject, Float> rotations;

	/** Number of retries. */
	private int retries = 0;

	/** Whether or not this game is a replay. */
	private boolean isReplay = false;

	/** The replay, if any. */
	private Replay replay;

	/** The current replay frame index. */
	private int replayIndex = 0;

	/** The replay cursor coordinates. */
	private int replayX, replayY;

	/** Whether a replay key is currently pressed. */
	private boolean replayKeyPressed;

	/** The replay skip time, or -1 if none. */
	private int replaySkipTime = -1;

	/** The last replay frame time. */
	private int lastReplayTime = 0;

	/** The keys from the previous replay frame. */
	private int lastReplayKeys = 0;

	/** The last game keys pressed. */
	private int lastKeysPressed = ReplayFrame.KEY_NONE;

	/** The previous game mod state (before the replay). */
	private int previousMods = 0;

	/** The list of current replay frames (for recording replays). */
	private LinkedList<ReplayFrame> replayFrames;

	/** The offscreen image rendered to. */
	private Image offscreen;

	/** The offscreen graphics. */
	private Graphics gOffscreen;

	/** The current flashlight area radius. */
	private int flashlightRadius;

	/** The cursor coordinates using the "auto" or "relax" mods. */
	private Vec2f autoMousePosition;

	/** Whether or not the cursor should be pressed using the "auto" mod. */
	private boolean autoMousePressed;

	/** Playback speed (used in replays and "auto" mod). */
	private PlaybackSpeed playbackSpeed;

	/** Whether the game is currently seeking to a replay position. */
	private boolean isSeeking;

	/** Music position bar coordinates and dimensions (for replay seeking). */
	private float musicBarX, musicBarY, musicBarWidth, musicBarHeight;

	private int mirrorFrom;
	private int mirrorTo;

	private Image epiImg;
	private float epiImgX;
	private float epiImgY;
	private int epiImgTime;

	/** The previous scores. */
	private ScoreData[] previousScores;

	/** The current rank in the scores. */
	private int currentRank;

	/** The time the rank was last updated. */
	private int lastRankUpdateTime;

	/** Whether the scoreboard is visible. */
	private boolean scoreboardVisible;

	/** The current alpha of the scoreboard. */
	private float currentScoreboardAlpha;

	/** The star stream shown when passing another score. */
	private StarStream scoreboardStarStream;

	/** Whether the game is finished (last hit object passed). */
	private boolean gameFinished = false;

	/** Timer after game has finished, before changing states. */
	private AnimatedValue gameFinishedTimer = new AnimatedValue(2500, 0, 1, AnimationEquation.LINEAR);

	/** Music position bar background colors. */
	private static final Color
		MUSICBAR_NORMAL = new Color(12, 9, 10, 0.25f),
		MUSICBAR_HOVER  = new Color(12, 9, 10, 0.35f),
		MUSICBAR_FILL   = new Color(255, 255, 255, 0.75f);

	private final CursorImpl mirrorCursor;
	private final MoveStoryboard moveStoryboardOverlay;
	private final StoryboardOverlay storyboardOverlay;
	private final OptionsOverlay optionsOverlay;
	private final PauseOverlay pauseOverlay;
	private FakeCombinedCurve knorkesliders;

	private boolean skippedToCheckpoint;
	private int firstObjectTime;

	public Game() {
		super();
		mirrorCursor = new CursorImpl(true);
		this.moveStoryboardOverlay = new MoveStoryboard();
		this.optionsOverlay = new OptionsOverlay(OptionGroups.storyboardOptions);
		this.storyboardOverlay = new StoryboardOverlay(moveStoryboardOverlay, optionsOverlay, this);
		this.pauseOverlay = new PauseOverlay();
		storyboardOverlay.show();
		moveStoryboardOverlay.show();
		optionsOverlay.setListener(storyboardOverlay);
	}

	@Override
	public void revalidate() {
		super.revalidate();

		// create offscreen graphics
		try {
			offscreen = new Image(width, height);
			gOffscreen = offscreen.getGraphics();
			gOffscreen.setBackground(Color.black);
		} catch (SlickException e) {
			Log.error("could not create offscreen graphics", e);
			bubNotifs.send(
				BUB_RED,
				"Exception while creating offscreen graphics. See logfile for details."
			);
		}

		// initialize music position bar location
		musicBarX = width * 0.01f;
		musicBarY = height * 0.05f;
		musicBarWidth = Math.max(width * 0.005f, 7);
		musicBarHeight = height * 0.9f;

		// initialize scoreboard star stream
		scoreboardStarStream = new StarStream(0, height * 2f / 3f, width / 4, 0, 0);
		scoreboardStarStream.setPositionSpread(height / 20f);
		scoreboardStarStream.setDirectionSpread(10f);
		scoreboardStarStream.setDurationSpread(700, 100);

		// create the associated GameData object
		gameObjectRenderer.gameData = data = new GameData();
	}

	void saveCheckpoint()
	{
		if (isLeadIn()) {
			return;
		}

		int time = MusicController.getPosition() / 1000;
		if (0 <= time && time < 3600) {
			OPTION_CHECKPOINT.setValue(time);
			SoundController.playSound(SoundEffect.MENUCLICK);
			barNotifs.send("Checkpoint saved.");
		}
	}

	public void loadCheckpoint(int checkpoint) {
		restartReason = RestartReason.USER;
		checkpointLoaded = true;
		skippedToCheckpoint = true;
		enter();
		if (isLeadIn()) {
			leadInTime = 0;
			epiImgTime = 0;
			MusicController.resume();
		}
		// skip to checkpoint
		MusicController.setPosition(checkpoint);
		while (objectIndex < gameObjects.length && beatmap.objects[objectIndex].getTime() <= checkpoint) {
			objectIndex++;
		}
		if (objectIndex > 0) {
			objectIndex--;
		}
		Dancer.instance.setObjectIndex(objectIndex);
		storyboardOverlay.updateIndex(objectIndex);
		lastReplayTime = beatmap.objects[objectIndex].getTime();
	}

	@Override
	public void render(Graphics g) {
		int trackPosition = MusicController.getPosition();
		if (isLeadIn()) {
			trackPosition -= leadInTime - OPTION_MUSIC_OFFSET.val - beatmap.localMusicOffset;
		}
		if (deathTime > -1)  // "Easy" mod: health bar increasing
			trackPosition = deathTime;
		int timeDiff = firstObjectTime - trackPosition;

		g.setBackground(Color.black);

		// "flashlight" mod: initialize offscreen graphics
		if (GameMod.FLASHLIGHT.isActive()) {
			gOffscreen.clear();
			Graphics.setCurrent(gOffscreen);
		}

		Colors.WHITE_FADE.a = 1f;
		final boolean loseState = (restartReason == RestartReason.LOSE);
		if (loseState) {
			final int extratime = (int) (System.currentTimeMillis() - failTime);
			if (extratime > LOSE_FADEOUT_TIME) {
				trackPosition = failTrackTime + LOSE_FADEOUT_TIME;
				Colors.WHITE_FADE.a = 0f;
			} else {
				trackPosition = failTrackTime + extratime;
				final float progress = (float) extratime / LOSE_FADEOUT_TIME;
				Colors.WHITE_FADE.a = 1f - IN_QUAD.calc(progress);
			}
		}

		// background
		if (!OPTION_DANCE_REMOVE_BG.state) {
			float dimLevel = (100 - OPTION_BACKGROUND_DIM.val) / 100f;
			if (trackPosition < firstObjectTime) {
				if (timeDiff < approachTime)
					dimLevel += (1f - dimLevel) * ((float) timeDiff / approachTime);
				else
					dimLevel = 1f;
			}
			if (OPTION_FORCE_DEFAULT_PLAYFIELD.state || !beatmap.drawBackground(width, height, dimLevel, false)) {
				Image playfield = GameImage.PLAYFIELD.getImage();
				playfield.setAlpha(dimLevel);
				playfield.draw();
				playfield.setAlpha(1f);
			}
		}

		// epilepsy warning
		if (epiImgTime > 0) {
			if (epiImgTime < 200) {
				// fade out
				Color c = new Color(Color.white);
				c.a = epiImgTime / 200f;
				epiImg.draw(epiImgX, epiImgY, c);
			} else {
				epiImg.draw(epiImgX, epiImgY);
			}
		}

		// "flashlight" mod: restricted view of hit objects around cursor
		if (GameMod.FLASHLIGHT.isActive()) {
			// render hit objects offscreen
			Graphics.setCurrent(gOffscreen);
			int trackPos = (isLeadIn()) ? (leadInTime - OPTION_MUSIC_OFFSET.val - beatmap.localMusicOffset) * -1 : trackPosition;
			drawHitObjects(gOffscreen, trackPos);

			// restore original graphics context
			gOffscreen.flush();
			Graphics.setCurrent(g);

			// draw alpha map around cursor
			g.setDrawMode(Graphics.MODE_ALPHA_MAP);
			g.clearAlphaMap();
			int mx, my;
			if (this.pauseOverlay.isActive()) {
				mx = this.pauseOverlay.mousePauseX;
				my = this.pauseOverlay.mousePauseY;
			} else if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
				mx = (int) autoMousePosition.x;
				my = (int) autoMousePosition.y;
			} else if (isReplay) {
				mx = replayX;
				my = replayY;
			} else {
				mx = mouseX;
				my = mouseY;
			}
			int alphaRadius = flashlightRadius * 256 / 215;
			int alphaX = mx - alphaRadius / 2;
			int alphaY = my - alphaRadius / 2;
			GameImage.ALPHA_MAP.getImage().draw(alphaX, alphaY, alphaRadius, alphaRadius);

			// blend offscreen image
			g.setDrawMode(Graphics.MODE_ALPHA_BLEND);
			g.setClip(alphaX, alphaY, alphaRadius, alphaRadius);
			g.drawImage(offscreen, 0, 0);
			g.clearClip();
			g.setDrawMode(Graphics.MODE_NORMAL);
		}


		// break periods
		if (beatmap.breaks != null && breakIndex < beatmap.breaks.size() && breakTime > 0) {
			int endTime = beatmap.breaks.get(breakIndex);
			int breakLength = endTime - breakTime;

			// letterbox effect (black bars on top/bottom)
			if (beatmap.letterboxInBreaks && breakLength >= 4000) {
				// let it fade in/out
				float a = Colors.BLACK_ALPHA.a;
				if (trackPosition - breakTime > breakLength / 2) {
					Colors.BLACK_ALPHA.a = (Math.min(500f, breakTime + breakLength - trackPosition)) / 500f;
				} else {
					Colors.BLACK_ALPHA.a = Math.min(500, trackPosition - breakTime) / 500f;
				}
				g.setColor(Colors.BLACK_ALPHA);
				g.fillRect(0, 0, width, height * 0.125f);
				g.fillRect(0, height * 0.875f, width, height * 0.125f);
				Colors.BLACK_ALPHA.a = a;
			}

			if (!OPTION_DANCE_HIDE_UI.state || !GameMod.AUTO.isActive()) {
				data.drawGameElements(g, true, objectIndex == 0, 1f);
			}

			if (breakLength >= 8000 &&
				trackPosition - breakTime > 2000 &&
				trackPosition - breakTime < 5000) {
				// show break start
				Image img;
				if (data.getHealth() >= 50) {
					img = GameImage.SECTION_PASS.getImage();
					if (!breakSound) {
						SoundController.playSound(SoundEffect.SECTIONPASS);
						breakSound = true;
					}
				} else {
					img = GameImage.SECTION_FAIL.getImage();
					if (!breakSound) {
						SoundController.playSound(SoundEffect.SECTIONFAIL);
						breakSound = true;
					}
				}
				img.drawCentered(width / 2f, height / 2f);
				breakframe.width = img.getWidth();
				breakframe.height = img.getHeight();
				breakframe.x = (width - breakframe.width) / 2;
				breakframe.y = (height - breakframe.height) / 2;
				breakframe.lastGLUpdate = System.currentTimeMillis();
			} else if (breakLength >= 4000) {
				// show break end (flash eight times for 125ms)
				int endTimeDiff = endTime - trackPosition;
				if (endTimeDiff < 2000 && (endTimeDiff / 125 % 2) == 1) {
					Image arrow = GameImage.WARNINGARROW.getImage();
					wa1frame.width = arrow.getWidth();
					wa2frame.width = wa1frame.width;
					wa3frame.width = wa1frame.width;
					wa4frame.width = wa1frame.width;
					wa1frame.height = arrow.getHeight();
					wa2frame.height = wa1frame.height;
					wa3frame.height = wa1frame.height;
					wa4frame.height = wa1frame.height;
					wa1frame.x = (int) (width * .15f);
					wa2frame.x = (int) (width * .15f);
					wa3frame.x = (int) (width * .75f);
					wa4frame.x = (int) (width * .75f);
					wa1frame.y = (int) (height * .15f);
					wa2frame.y = (int) (height * .75f);
					wa3frame.y = (int) (height * .15f);
					wa4frame.y = (int) (height * .75f);
					wa1frame.lastGLUpdate = System.currentTimeMillis();
					wa2frame.lastGLUpdate = System.currentTimeMillis();
					wa3frame.lastGLUpdate = System.currentTimeMillis();
					wa4frame.lastGLUpdate = System.currentTimeMillis();
					arrow.setRotation(0);
					arrow.draw(width * 0.15f, height * 0.15f);
					arrow.draw(width * 0.15f, height * 0.75f);
					arrow = arrow.getFlippedCopy(true, false);
					arrow.draw(width * 0.75f, height * 0.15f);
					arrow.draw(width * 0.75f, height * 0.75f);
				}
			}
		}

		// non-break
		else {
			if (!GameMod.AUTO.isActive() || !OPTION_DANCE_HIDE_UI.state) {
				// game elements
				float gameElementAlpha = 1f;
				if (gameFinished) {
					// game finished: fade everything out
					float t = 1f - Math.min(gameFinishedTimer.getTime() / (float) FINISHED_FADEOUT_TIME, 1f);
					gameElementAlpha = AnimationEquation.OUT_CUBIC.calc(t);
				}
				data.drawGameElements(g, false, objectIndex == 0, gameElementAlpha);

				// skip beginning
				if (objectIndex == 0 &&
						trackPosition < beatmap.objects[0].getTime() - SKIP_OFFSET)
				{
					skipButton.draw();
					Image img = skipButton.getImage();
					if (img != null) {
						skipbtnframe.width = img.getWidth();
						skipbtnframe.height = img.getHeight();
						skipbtnframe.x = (int) skipButton.getX() - skipbtnframe.width / 2;
						skipbtnframe.y = (int) skipButton.getY() - skipbtnframe.height / 2;
						skipbtnframe.lastGLUpdate = System.currentTimeMillis();
					}
				}

				// show retries
				if (retries >= 2 && timeDiff >= -1000) {
					int retryHeight = Math.max(
							GameImage.SCOREBAR_BG.getHeight(),
							GameImage.SCOREBAR_KI.getHeight()
					);
					float oldAlpha = Colors.WHITE_FADE.a;
					if (timeDiff < -500)
						Colors.WHITE_FADE.a = (1000 + timeDiff) / 500f;
					Fonts.MEDIUM.drawString(
							2 + (width / 100), retryHeight,
							String.format("%d retries and counting...", retries),
							Colors.WHITE_FADE
					);
					Colors.WHITE_FADE.a = oldAlpha;
				}

				if (isLeadIn()) // render approach circles during song lead-in
					trackPosition = (leadInTime - OPTION_MUSIC_OFFSET.val - beatmap.localMusicOffset) * -1;

				// countdown
				if (beatmap.countdown > 0) {
					float speedModifier = GameMod.getSpeedMultiplier() * playbackSpeed.getModifier();
					timeDiff = firstObjectTime - trackPosition;
					if (timeDiff >= 500 * speedModifier && timeDiff < 3000 * speedModifier) {
						if (timeDiff >= 1500 * speedModifier) {
							GameImage.COUNTDOWN_READY.getImage().drawCentered(width / 2, height / 2);
							if (!countdownReadySound) {
								SoundController.playSound(SoundEffect.READY);
								countdownReadySound = true;
							}
						}
						if (timeDiff < 2000 * speedModifier) {
							GameImage.COUNTDOWN_3.getImage().draw(0, 0);
							if (!countdown3Sound) {
								SoundController.playSound(SoundEffect.COUNT3);
								countdown3Sound = true;
							}
						}
						if (timeDiff < 1500 * speedModifier) {
							COUNTDOWN_2.getImage().draw(width - COUNTDOWN_2.getWidth(), 0);
							if (!countdown2Sound) {
								SoundController.playSound(SoundEffect.COUNT2);
								countdown2Sound = true;
							}
						}
						if (timeDiff < 1000 * speedModifier) {
							GameImage.COUNTDOWN_1.getImage().drawCentered(width / 2, height / 2);
							if (!countdown1Sound) {
								SoundController.playSound(SoundEffect.COUNT1);
								countdown1Sound = true;
							}
						}
					} else if (timeDiff >= -500 * speedModifier && timeDiff < 500 * speedModifier) {
						Image go = GameImage.COUNTDOWN_GO.getImage();
						go.setAlpha((timeDiff < 0) ? 1 - (timeDiff / speedModifier / -500f) : 1);
						go.drawCentered(width / 2, height / 2);
						if (!countdownGoSound) {
							SoundController.playSound(SoundEffect.GO);
							countdownGoSound = true;
						}
					}
				}
			} else {
				// skip beginning
				if (objectIndex == 0 &&
					trackPosition < beatmap.objects[0].getTime() - SKIP_OFFSET)
				{
					skipButton.draw();
					Image img = skipButton.getImage();
					if (img != null) {
						skipbtnframe.width = img.getWidth();
						skipbtnframe.height = img.getHeight();
						skipbtnframe.x = (int) skipButton.getX() - skipbtnframe.width / 2;
						skipbtnframe.y = (int) skipButton.getY() - skipbtnframe.height / 2;
						skipbtnframe.lastGLUpdate = System.currentTimeMillis();
					}
				}
			}

			// draw hit objects
			if (!GameMod.FLASHLIGHT.isActive())
				drawHitObjects(g, trackPosition);
		}

		// in-game scoreboard
		if (previousScores != null && trackPosition >= firstObjectTime && !GameMod.RELAX.isActive() && !GameMod.AUTOPILOT.isActive()) {
			ScoreData currentScore = data.getCurrentScoreData(beatmap, true);
			while (currentRank > 0 && previousScores[currentRank - 1].score < currentScore.score) {
				currentRank--;
				scoreboardStarStream.burst(20);
				lastRankUpdateTime = trackPosition;
			}

			float animation = AnimationEquation.IN_OUT_QUAD.calc(
				Utils.clamp((trackPosition - lastRankUpdateTime) / SCOREBOARD_ANIMATION_TIME, 0f, 1f)
			);
			int scoreboardPosition = 2 * height / 3;

			// draw star stream behind the scores
			scoreboardStarStream.draw();

			if (currentRank < 4) {
				// draw the (new) top 5 ranks
				for (int i = 0; i < 4; i++) {
					int index = i + (i >= currentRank ? 1 : 0);
					if (i < previousScores.length) {
						float position = index + (i == currentRank ? animation - 3f : -2f);
						previousScores[i].drawSmall(g, scoreboardPosition, index + 1, position, data, currentScoreboardAlpha, false);
					}
				}
				currentScore.drawSmall(g, scoreboardPosition, currentRank + 1, currentRank - 1f - animation, data, currentScoreboardAlpha, true);
			} else {
				// draw the top 2 and next 2 ranks
				previousScores[0].drawSmall(g, scoreboardPosition, 1, -2f, data, currentScoreboardAlpha, false);
				previousScores[1].drawSmall(g, scoreboardPosition, 2, -1f, data, currentScoreboardAlpha, false);
				previousScores[currentRank - 2].drawSmall(
					g, scoreboardPosition, currentRank - 1, animation - 1f, data, currentScoreboardAlpha * animation, false
				);
				previousScores[currentRank - 1].drawSmall(g, scoreboardPosition, currentRank, animation, data, currentScoreboardAlpha, false);
				currentScore.drawSmall(g, scoreboardPosition, currentRank + 1, 2f, data, currentScoreboardAlpha, true);
				if (animation < 1.0f && currentRank < previousScores.length) {
					previousScores[currentRank].drawSmall(
						g, scoreboardPosition, currentRank + 2, 1f + 5 * animation, data, currentScoreboardAlpha * (1f - animation), false
					);
				}
			}
		}

		if (!OPTION_DANCE_HIDE_UI.state && GameMod.AUTO.isActive())
			GameImage.UNRANKED.getImage().drawCentered(width / 2, height * 0.077f);

		// draw replay speed button
		if (isReplay || (!OPTION_DANCE_HIDE_UI.state&& GameMod.AUTO.isActive()))
			playbackSpeed.getButton().draw();

		// draw music position bar (for replay seeking)
		if (isReplay && OPTION_REPLAY_SEEKING.state) {
			g.setColor((musicPositionBarContains(mouseX, mouseY)) ? MUSICBAR_HOVER : MUSICBAR_NORMAL);
			g.fillRoundRect(musicBarX, musicBarY, musicBarWidth, musicBarHeight, 4);
			if (!isLeadIn()) {
				g.setColor(MUSICBAR_FILL);
				float musicBarPosition = Math.min((float) trackPosition / beatmap.endTime, 1f);
				g.fillRoundRect(musicBarX, musicBarY, musicBarWidth, musicBarHeight * musicBarPosition, 4);
			}
		}

		if (isReplay) {
			displayContainer.cursor.draw(replayKeyPressed);
		} else if (GameMod.AUTO.isActive()) {
			displayContainer.cursor.draw(autoMousePressed);
			if (OPTION_DANCE_MIRROR.state) {
				for (int i = 1; i < OPTION_MERGING_SLIDERS_MIRROR_POOL.val; i++) {
					float angle = 360.0f / OPTION_MERGING_SLIDERS_MIRROR_POOL.val * i;
					g.pushTransform();
					g.rotate(width2, height2, -angle);
					mirrorCursor.draw(autoMousePressed);
					g.popTransform();
				}
			}
		} else {
			final boolean expandCursor =
				Keyboard.isKeyDown(OPTION_KEY_LEFT.keycode) ||
				Keyboard.isKeyDown(OPTION_KEY_RIGHT.keycode) ||
				(!OPTION_DISABLE_MOUSE_BUTTONS.state && (
				Mouse.isButtonDown(Input.LMB) ||
				Mouse.isButtonDown(Input.RMB)));

			displayContainer.cursor.draw(expandCursor);
		}

		super.render(g);

		if (OPTION_DANCE_ENABLE_SB.state) {
			optionsOverlay.render(g);
		}
	}

	@Override
	public void preRenderUpdate() {
		super.preRenderUpdate();

		if (OPTION_DANCE_ENABLE_SB.state) {
			optionsOverlay.preRenderUpdate();
		}

		int delta = renderDelta;

		UI.update(delta);
		Pippi.update(delta);
		if (epiImgTime > 0) {
			epiImgTime -= delta;
		}
		yugecin.opsudance.spinners.Spinner.update(delta);
		skipButton.hoverUpdate(delta, mouseX, mouseY);
		if (isReplay || GameMod.AUTO.isActive())
			playbackSpeed.getButton().hoverUpdate(delta, mouseX, mouseY);
		int trackPosition = MusicController.getPosition();
		scoreboardStarStream.update(delta);

		if (this.pauseOverlay.isActive()) {
			return;
		}

		// pause game if focus lost
		if (!Display.isActive() && !GameMod.AUTO.isActive() && !isReplay) {
			this.pauseGame();
			return;
		}

		// replays: skip intro
		if (isReplay && replaySkipTime > -1 && trackPosition >= replaySkipTime) {
			if (skipIntro())
				trackPosition = MusicController.getPosition();
		}

		// "flashlight" mod: calculate visible area radius
		updateFlashlightRadius(delta, trackPosition);

		// stop updating during song lead-in
		if (isLeadIn()) {
			leadInTime -= delta;
			if (!isLeadIn())
				MusicController.resume();
			return;
		}

		// "Easy" mod: multiple "lives"
		if (GameMod.EASY.isActive() && deathTime > -1) {
			if (data.getHealth() < 99f) {
				data.changeHealth(delta / 10f);
				data.updateDisplays(delta);
				return;
			}
			MusicController.resume();
			deathTime = -1;
		}

		// normal game update
		if (!isReplay && !gameFinished)
			addReplayFrameAndRun(mouseX, mouseY, lastKeysPressed, trackPosition);

		// watching replay
		else if (!gameFinished) {
			// out of frames, use previous data
			if (replayIndex >= replay.frames.length)
				updateGame(replayX, replayY, delta, MusicController.getPosition(), lastKeysPressed);

			// seeking to a position earlier than original track position
			if (isSeeking && replayIndex - 1 >= 1 && replayIndex < replay.frames.length &&
			    trackPosition < replay.frames[replayIndex - 1].getTime()) {
				replayIndex = 0;
				while (objectIndex >= 0) {
					gameObjects[objectIndex].reset();
					objectIndex--;
				}

				// reset game data
				resetGameData();

				// load the first timingPoint
				if (!beatmap.timingPoints.isEmpty()) {
					TimingPoint timingPoint = beatmap.timingPoints.get(0);
					if (!timingPoint.isInherited()) {
						setBeatLength(timingPoint, true);
						timingPointIndex++;
					}
				}
			}

			// update and run replay frames
			while (replayIndex < replay.frames.length && trackPosition >= replay.frames[replayIndex].getTime()) {
				ReplayFrame frame = replay.frames[replayIndex];
				replayX = frame.getScaledX();
				replayY = frame.getScaledY();
				replayKeyPressed = frame.isKeyPressed();
				lastKeysPressed = frame.getKeys();
				runReplayFrame(frame);
				replayIndex++;
			}
			mouseX = replayX;
			mouseY = replayY;

			// unmute sounds
			if (isSeeking) {
				isSeeking = false;
				SoundController.mute(false);
			}
		}

		// update in-game scoreboard
		if (!OPTION_DANCE_HIDE_UI.state && previousScores != null && trackPosition > firstObjectTime) {
			// show scoreboard if selected, and always in break
			// hide when game ends
			if ((scoreboardVisible || breakTime > 0) && !gameFinished) {
				currentScoreboardAlpha += 1f / SCOREBOARD_FADE_IN_TIME * delta;
				if (currentScoreboardAlpha > 1f)
					currentScoreboardAlpha = 1f;
			} else {
				currentScoreboardAlpha -= 1f / SCOREBOARD_FADE_IN_TIME * delta;
				if (currentScoreboardAlpha < 0f)
					currentScoreboardAlpha = 0f;
			}
		}

		data.updateDisplays(delta);

		// game finished: change state after timer expires
		if (gameFinished && !gameFinishedTimer.update(delta)) {
			if (checkpointLoaded) {
				// if checkpoint used, skip ranking screen
				onCloseRequest();
			} else {
				// go to ranking screen
				displayContainer.switchState(gameRankingState);
			}
		}
	}

	@Override
	public void update() {
		super.update();

		int trackPosition = MusicController.getPosition();

		// "auto" and "autopilot" mods: move cursor automatically
		autoMousePressed = false;
		if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
			Vec2f autoPoint;
			if (objectIndex < beatmap.objects.length - Dancer.instance.getPolyMoverFactoryMinBufferSize()) {
				Dancer d = Dancer.instance;
				d.update(trackPosition, objectIndex);
				autoPoint = new Vec2f(d.x, d.y);
				if (trackPosition < gameObjects[objectIndex].getTime()) {
					autoMousePressed = true;
				}
			} else {
				if (objectIndex < beatmap.objects.length) {
					autoPoint = gameObjects[objectIndex].getPointAt(trackPosition);
				} else {
					// last object
					autoPoint = gameObjects[objectIndex - 1].getPointAt(trackPosition);
				}
			}

			float[] sbPosition = moveStoryboardOverlay.getPoint(trackPosition);
			if (sbPosition != null) {
				autoPoint.x = sbPosition[0];
				autoPoint.y = sbPosition[1];
			}

			// set mouse coordinates
			autoMousePosition.set(autoPoint.x, autoPoint.y);
		}

		if (isReplay) {
			displayContainer.cursor.setCursorPosition(replayX, replayY);
		} else if (GameMod.AUTO.isActive()) {
			displayContainer.cursor.setCursorPosition((int) autoMousePosition.x, (int) autoMousePosition.y);
			if (OPTION_DANCE_MIRROR.state && GameMod.AUTO.isActive()) {
				mirrorCursor.setCursorPosition((int) autoMousePosition.x, (int) autoMousePosition.y);
			}
		} else if (GameMod.AUTOPILOT.isActive() && !this.pauseOverlay.isActive()) {
			displayContainer.cursor.setCursorPosition((int) autoMousePosition.x, (int) autoMousePosition.y);
		} else {
			displayContainer.cursor.setCursorPosition(mouseX, mouseY);
		}
	}

	/**
	 * Updates the game.
	 * @param mouseX the mouse x coordinate
	 * @param mouseY the mouse y coordinate
	 * @param delta the delta interval
	 * @param trackPosition the track position
	 * @param keys the keys that are pressed
	 */
	private void updateGame(int mouseX, int mouseY, int delta, int trackPosition, int keys) {
		// map complete!
		boolean complete = objectIndex >= gameObjects.length;
		if (GameMod.AUTO.isActive() && complete) {
			if (gameObjects.length > 0) {
				complete = trackPosition >= gameObjects[gameObjects.length - 1].getEndTime() + OPTION_MAP_END_DELAY.val * 100;
			}
		}
		if (complete || (MusicController.trackEnded() && objectIndex > 0)) {
			// track ended before last object was processed: force a hit result
			if (MusicController.trackEnded() && objectIndex < gameObjects.length)
				gameObjects[objectIndex].update(true, delta, mouseX, mouseY, false, trackPosition);

			// save score and replay
			if (!checkpointLoaded) {
				boolean unranked = (GameMod.AUTO.isActive() || GameMod.RELAX.isActive() || GameMod.AUTOPILOT.isActive());
				gameRankingState.setGameData(data);
				if (isReplay)
					data.setReplay(replay);
				else if (replayFrames != null) {
					// finalize replay frames with start/skip frames
					if (!replayFrames.isEmpty())
						replayFrames.getFirst().setTimeDiff(replaySkipTime * -1);
					replayFrames.addFirst(ReplayFrame.getStartFrame(replaySkipTime));
					replayFrames.addFirst(ReplayFrame.getStartFrame(0));
					Replay r = data.getReplay(replayFrames.toArray(new ReplayFrame[replayFrames.size()]), beatmap);
					if (r != null && !unranked)
						r.save();
				}
				ScoreData score = data.getScoreData(beatmap);
				data.setGameplay(!isReplay);

				// add score to database
				if (!unranked && !isReplay) {
					ScoreDB.addScore(score);
					boolean resort = false;
					final BeatmapSortOrder so = BeatmapSortOrder.current;
					if (score.score > this.beatmap.topScore) {
						this.beatmap.topGrade = score.getGrade();
						resort = so == BeatmapSortOrder.RANK;
					}
					if (BeatmapGroup.current == BeatmapGroup.RECENT) {
						beatmapList.activeGroupChanged();
					} else if (resort || so == BeatmapSortOrder.PLAYS) {
						beatmapList.resort();
						nodeList.persistentRecreate();
					}
				}
			}

			// start timer
			gameFinished = true;
			gameFinishedTimer.setTime(2500);

			return;
		}

		if (objectIndex >= gameObjects.length) {
			return;
		}

		GameObject g = gameObjects[objectIndex];
		if (g.isCircle() || g.isSlider()) {
			if (g.getTime() <= trackPosition) {
				CursorImpl.lastObjColor = g.getColor();
				CursorImpl.lastMirroredObjColor = g.getMirroredColor();
			} else {
				CursorImpl.nextObjColor = g.getColor();
				CursorImpl.nextMirroredObjColor = g.getMirroredColor();
			}
		}

		// timing points
		if (timingPointIndex < beatmap.timingPoints.size()) {
			TimingPoint timingPoint = beatmap.timingPoints.get(timingPointIndex);
			if (trackPosition >= timingPoint.getTime()) {
				setBeatLength(timingPoint, true);
				timingPointIndex++;
			}
		}

		// song beginning
		if (objectIndex == 0 && trackPosition < beatmap.objects[0].getTime())
			return;  // nothing to do here

		// break periods
		if (beatmap.breaks != null && breakIndex < beatmap.breaks.size()) {
			int breakValue = beatmap.breaks.get(breakIndex);
			if (breakTime > 0) {  // in a break period
				if (trackPosition < breakValue &&
				    trackPosition < beatmap.objects[objectIndex].getTime() - approachTime)
					return;
				else {
					// break is over
					breakTime = 0;
					breakIndex++;
				}
			} else if (trackPosition >= breakValue) {
				// start a break
				breakTime = breakValue;
				breakSound = false;
				breakIndex++;
				return;
			}
		}

		// drain health
		data.changeHealth(delta * -1 * GameData.HP_DRAIN_MULTIPLIER);
		if (!data.isAlive()) {
			// "Easy" mod
			if (GameMod.EASY.isActive() && !GameMod.SUDDEN_DEATH.isActive()) {
				deaths++;
				if (deaths < 3) {
					deathTime = trackPosition;
					MusicController.pause();
					return;
				}
			}

			// game over, force a restart
			if (!isReplay) {
				if (restartReason != RestartReason.LOSE) {
					restartReason = RestartReason.LOSE;
					failTime = System.currentTimeMillis();
					failTrackTime = MusicController.getPosition();
					MusicController.fadeOut(MUSIC_FADEOUT_TIME);
					MusicController.pitchFadeOut(MUSIC_FADEOUT_TIME);
					rotations = new IdentityHashMap<>();
					SoundController.playSound(SoundEffect.FAIL);

					this.pauseOverlay.engageLose();
					return;
				}
			}
		}

		// don't process hit results when already lost
		if (restartReason != RestartReason.LOSE) {
			// update objects (loop in unlikely event of any skipped indexes)
			boolean keyPressed = keys != ReplayFrame.KEY_NONE;
			while (objectIndex < gameObjects.length && trackPosition > beatmap.objects[objectIndex].getTime()) {
				// check if we've already passed the next object's start time
				boolean overlap = (objectIndex + 1 < gameObjects.length &&
						trackPosition > beatmap.objects[objectIndex + 1].getTime() - hitResultOffset[GameData.HIT_50]);

				// update hit object and check completion status
				if (gameObjects[objectIndex].update(overlap, delta, mouseX, mouseY, keyPressed, trackPosition)) {
					objectIndex++;  // done, so increment object index
					storyboardOverlay.updateIndex(objectIndex);
					if (objectIndex >= mirrorTo) {
						if (OPTION_DANCE_MIRROR.state) {
							OPTION_DANCE_MIRROR.toggle();
						}
					}
				} else
					break;
			}
		}
	}

	@Override
	public boolean onCloseRequest()
	{
		if (this.pauseOverlay.isActive()) {
			this.pauseOverlay.backToSongMenu();
			return false;
		}
		songMenuState.resetGameDataOnLoad();
		displayContainer.switchState(songMenuState);
		return false;
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (OPTION_DANCE_ENABLE_SB.state) {
			optionsOverlay.keyPressed(e);
			if (e.isConsumed()) {
				return;
			}
		}

		super.keyPressed(e);
		if (e.isConsumed()) {
			return;
		}

		e.consume();

		if (gameFinished) {
			return;
		}

		int trackPosition = MusicController.getPosition();

		// game keys
		if (!Keyboard.isRepeatEvent()) {
			int keys = ReplayFrame.KEY_NONE;
			if (e.keyCode == OPTION_KEY_LEFT.keycode) {
				keys = ReplayFrame.KEY_K1;
			} else if (e.keyCode == OPTION_KEY_RIGHT.keycode) {
				keys = ReplayFrame.KEY_K2;
			}
			if (keys != ReplayFrame.KEY_NONE) {
				gameKeyPressed(keys, mouseX, mouseY, trackPosition);
			}
		}

		switch (e.keyCode) {
		case KEY_ESCAPE:
			// "auto" mod or watching replay: go back to song menu
			if (GameMod.AUTO.isActive() || isReplay) {
				onCloseRequest();
				break;
			}

			this.pauseGame();
			break;
		case KEY_SPACE:
			// skip intro
			skipIntro();
			break;
		case KEY_R:
			// restart
			if (input.isControlDown()) {
				if (trackPosition < beatmap.objects[0].getTime()) {
					retries--;  // don't count this retry (cancel out later increment)
				}
				restartReason = RestartReason.USER;
				enter();
				skipIntro();
			}
			break;
		case KEY_S:
			if (input.isControlDown()) {
				this.saveCheckpoint();
			}
			break;
		case KEY_L:
			// load checkpoint
			if (input.isControlDown()) {
				int checkpoint = OPTION_CHECKPOINT.val * 1000;
				if (checkpoint == 0 || checkpoint > beatmap.endTime)
					break;  // invalid checkpoint
				loadCheckpoint(checkpoint);
				SoundController.playSound(SoundEffect.MENUHIT);
				barNotifs.send("Checkpoint loaded.");
			}
			break;
		case KEY_F:
			// change playback speed
			if (isReplay || GameMod.AUTO.isActive()) {
				playbackSpeed = playbackSpeed.next();
				MusicController.setPitch(GameMod.getSpeedMultiplier() * playbackSpeed.getModifier());
			}
			break;
		case KEY_UP:
			volumeControl.changeVolume(1);
			break;
		case KEY_DOWN:
			volumeControl.changeVolume(-1);
			break;
		case KEY_TAB:
			if (!OPTION_DANCE_HIDE_UI.state) {
				scoreboardVisible = !scoreboardVisible;
			}
			break;
		case KEY_M:
			if (OPTION_DANCE_MIRROR.state) {
				mirrorTo = objectIndex;
			} else {
				mirrorCursor.resetLocations((int) autoMousePosition.x, (int) autoMousePosition.y);
				mirrorFrom = objectIndex;
				mirrorTo = gameObjects.length;
			}
			OPTION_DANCE_MIRROR.toggle();
			break;
		case KEY_P:
			if (OPTION_DANCE_MIRROR.state) {
				mirrorTo = objectIndex;
			} else {
				mirrorCursor.resetLocations((int) autoMousePosition.x, (int) autoMousePosition.y);
				mirrorFrom = objectIndex;
				mirrorTo = mirrorFrom + 1;
			}
			OPTION_DANCE_MIRROR.toggle();
			break;
		}

		if (e.keyCode == KEY_SUBTRACT || e.keyCode == KEY_MINUS || e.chr == '-') {
			this.adjustLocalMusicOffset(-5);
		} else if (e.keyCode == KEY_EQUALS || e.keyCode == KEY_ADD || e.chr == '+') {
			this.adjustLocalMusicOffset(5);
		}
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
		if (OPTION_DANCE_ENABLE_SB.state) {
			this.optionsOverlay.mouseDragged(e);
			if (e.isConsumed()) {
				return;
			}
		}

		super.mouseDragged(e);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (OPTION_DANCE_ENABLE_SB.state) {
			optionsOverlay.mousePressed(e);
			if (e.isConsumed()) {
				return;
			}
		}

		super.mousePressed(e);
		if (e.isConsumed()) {
			return;
		}

		e.consume();

		if (gameFinished) {
			return;
		}

		final int button = e.button;
		final int x = e.x;
		final int y = e.y;

		// watching replay
		if (isReplay || GameMod.AUTO.isActive()) {
			if (button == Input.MMB) {
				return;
			}

			// skip button
			if (skipButton.contains(x, y))
				skipIntro();

			// playback speed button
			else if (playbackSpeed.getButton().contains(x, y)) {
				playbackSpeed = playbackSpeed.next();
				MusicController.setPitch(GameMod.getSpeedMultiplier() * playbackSpeed.getModifier());
			}

			// replay seeking
			else if (OPTION_REPLAY_SEEKING.state && !GameMod.AUTO.isActive() && musicPositionBarContains(x, y)) {
				SoundController.mute(true);  // mute sounds while seeking
				float pos = (y - musicBarY) / musicBarHeight * beatmap.endTime;
				MusicController.setPosition((int) pos);
				isSeeking = true;
			}
			return;
		}

		if (OPTION_DISABLE_MOUSE_BUTTONS.state) {
			return;
		}

		// mouse wheel: pause the game
		if (button == Input.MMB && !OPTION_DISABLE_MOUSE_WHEEL.state) {
			this.pauseGame();
			return;
		}

		// game keys
		int keys = ReplayFrame.KEY_NONE;
		if (button == Input.LMB)
			keys = ReplayFrame.KEY_M1;
		else if (button == Input.RMB)
			keys = ReplayFrame.KEY_M2;
		if (keys != ReplayFrame.KEY_NONE)
			gameKeyPressed(keys, x, y, MusicController.getPosition());
	}

	/**
	 * Handles a game key pressed event.
	 * @param keys the game keys pressed
	 * @param x the mouse x coordinate
	 * @param y the mouse y coordinate
	 * @param trackPosition the track position
	 */
	private void gameKeyPressed(int keys, int x, int y, int trackPosition) {
		// skip beginning
		if (skipButton.contains(x, y)) {
			if (skipIntro())
				return;  // successfully skipped
		}

		// "auto" and "relax" mods: ignore user actions
		if (GameMod.AUTO.isActive() || GameMod.RELAX.isActive())
			return;

		// send a game key press
		if (!isReplay && keys != ReplayFrame.KEY_NONE) {
			lastKeysPressed |= keys;  // set keys bits
			addReplayFrameAndRun(x, y, lastKeysPressed, trackPosition);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (OPTION_DANCE_ENABLE_SB.state) {
			optionsOverlay.mouseReleased(e);
			if (e.isConsumed()) {
				return;
			}
		}

		super.mouseReleased(e);
		if (e.isConsumed()) {
			return;
		}

		e.consume();

		if (gameFinished) {
			return;
		}

		if (OPTION_DISABLE_MOUSE_BUTTONS.state) {
			return;
		}

		if (e.button == Input.MMB) {
			return;
		}

		int keys = ReplayFrame.KEY_NONE;
		if (e.button == Input.LMB)
			keys = ReplayFrame.KEY_M1;
		else if (e.button == Input.RMB)
			keys = ReplayFrame.KEY_M2;
		if (keys != ReplayFrame.KEY_NONE)
			gameKeyReleased(keys, e.x, e.y, MusicController.getPosition());
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		if (OPTION_DANCE_ENABLE_SB.state) {
			return;
		}

		super.keyReleased(e);
		if (e.isConsumed()) {
			return;
		}

		e.consume();

		if (gameFinished) {
			return;
		}

		int keys = ReplayFrame.KEY_NONE;
		if (e.keyCode == OPTION_KEY_LEFT.keycode) {
			keys = ReplayFrame.KEY_K1;
		} else if (e.keyCode == OPTION_KEY_RIGHT.keycode) {
			keys = ReplayFrame.KEY_K2;
		}
		if (keys != ReplayFrame.KEY_NONE) {
			gameKeyReleased(keys, mouseX, mouseY, MusicController.getPosition());
		}
	}

	/**
	 * Handles a game key released event.
	 * @param keys the game keys released
	 * @param x the mouse x coordinate
	 * @param y the mouse y coordinate
	 * @param trackPosition the track position
	 */
	private void gameKeyReleased(int keys, int x, int y, int trackPosition) {
		if (!isReplay &&
			keys != ReplayFrame.KEY_NONE &&
			!isLeadIn())
		{
			lastKeysPressed &= ~keys;  // clear keys bits
			addReplayFrameAndRun(x, y, lastKeysPressed, trackPosition);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (OPTION_DANCE_ENABLE_SB.state) {
			optionsOverlay.mouseWheelMoved(e);
			if (e.isConsumed()) {
				return;
			}
		}

		super.mouseWheelMoved(e);
		if (e.isConsumed()) {
			return;
		}

		e.consume();

		if (OPTION_DISABLE_MOUSE_WHEEL.state) {
			return;
		}

		volumeControl.changeVolume(e.direction);
	}

	private void pauseGame()
	{
		this.pauseOverlay.mousePauseX = mouseX;
		this.pauseOverlay.mousePauseY = mouseY;
		this.pauseOverlay.requireMousePositionBeforeResume =
			this.firstObjectTime - SKIP_OFFSET < MusicController.getPosition() &&
			!GameMod.RELAX.isActive() &&
			!GameMod.AUTOPILOT.isActive() &&
			breakTime == 0;
		this.pauseOverlay.engagePause();
	}

	/**
	 * restarts the game, will switch to game state if not already in it
	 */
	public void restart(RestartReason reason)
	{
		this.restartReason = reason;
		if (displayContainer.isIn(this)) {
			this.enter();
			return;
		}
		displayContainer.switchState(this);
	}

	@Override
	public void enter()
	{
		overlays.clear();
		if (OPTION_DANCE_ENABLE_SB.state) {
			overlays.add(moveStoryboardOverlay);
			overlays.add(storyboardOverlay);
			storyboardOverlay.onEnter();
		}

		super.enter();

		displayContainer.drawCursor = false;

		isInGame = true;
		if (!skippedToCheckpoint) {
			UI.enter();
		}

		if (beatmap == null || beatmap.objects == null) {
			bubNotifs.send(BUB_RED, "Game was running without a beatmap");
			displayContainer.switchStateInstantly(songMenuState);
			return;
		}

		if (beatmap.objects.length == 0) {
			bubNotifs.send(BUB_ORANGE, "Beatmap has no objects");
			displayContainer.switchStateInstantly(songMenuState);
			return;
		}

		this.firstObjectTime = beatmap.objects[0].getTime();

		Display.setTitle(String.format("%s - %s", Constants.PROJECT_NAME, beatmap.toString()));

		Dancer.instance.reset();
		MoverDirection.reset(beatmap.beatmapID);

		ObjectColorOverrides.resetRainbowHue(beatmap.beatmapID);
		CursorColorManager.resetColorChange(beatmap.beatmapID);

		// free all previously cached hitobject to framebuffer mappings if some still exist
		FrameBufferCache.getInstance().freeMap();

		// grab the mouse (not working for touchscreen)
//		container.setMouseGrabbed(true);

		// update play stats
		if (restartReason == RestartReason.NEWGAME) {
			beatmap.incrementPlayCounter();
			BeatmapDB.updatePlayStatistics(beatmap);
		}

		// load epilepsy warning img
		epiImgTime = OPTION_EPILEPSY_WARNING.val * 100;
		if (epiImgTime > 0) {
			epiImg = GameImage.EPILEPSY_WARNING.getImage();
			epiImg = epiImg.getScaledCopy(width2 / epiImg.getWidth());
			epiImgX = width2 - epiImg.getWidth() / 2;
			epiImgY = height2 - epiImg.getHeight() / 2;
		}

		// load mods
		if (isReplay) {
			previousMods = GameMod.getModState();
			GameMod.loadModState(replay.mods);
		}

		data.setGameplay(true);

		// check restart state
		if (restartReason == RestartReason.NEWGAME) {
			// new game
			loadImages();
			setMapModifiers();
			retries = 0;
		} else if (restartReason == RestartReason.USER && !GameMod.AUTO.isActive()) {
			// retry
			retries++;
		} else if (restartReason == RestartReason.WATCHREPLAY || GameMod.AUTO.isActive()) {
			// replay
			retries = 0;
		}

		gameObjects = new GameObject[beatmap.objects.length];
		playbackSpeed = PlaybackSpeed.NORMAL;

		// reset game data
		resetGameData();

		// load the first timingPoint for stacking
		if (!beatmap.timingPoints.isEmpty()) {
			TimingPoint timingPoint = beatmap.timingPoints.get(0);
			if (!timingPoint.isInherited()) {
				setBeatLength(timingPoint, true);
				timingPointIndex++;
			}
		}

		// initialize object maps
		Color[] comboColors = beatmap.getComboColors();
		if (comboColors == null) {
			comboColors = SkinService.skin.getComboColors();
		}
		CursorColorManager.setComboColors(comboColors);
		ObjectColorOverrides.comboColors = comboColors;
		for (int i = 0; i < beatmap.objects.length; i++) {
			HitObject hitObject = beatmap.objects[i];

			// is this the last note in the combo?
			boolean comboEnd = false;
			if (i + 1 >= beatmap.objects.length || beatmap.objects[i + 1].isNewCombo())
				comboEnd = true;

			// pass beatLength to hit objects
			int hitObjectTime = hitObject.getTime();
			while (timingPointIndex < beatmap.timingPoints.size()) {
				TimingPoint timingPoint = beatmap.timingPoints.get(timingPointIndex);
				if (timingPoint.getTime() > hitObjectTime)
					break;
				setBeatLength(timingPoint, false);
				timingPointIndex++;
			}

			try {
				if (hitObject.isCircle()) {
					gameObjects[i] = new Circle(hitObject, data, hitObject.getComboIndex(), comboEnd);
				} else if (hitObject.isSlider()) {
					gameObjects[i] = new Slider(hitObject, data, hitObject.getComboIndex(), comboEnd);
				} else if (hitObject.isSpinner()) {
					gameObjects[i] = new Spinner(hitObject, data);
				}
			} catch (Exception e) {
				String message = String.format("Failed to create %s at index %d:\n%s", hitObject.getTypeName(), i, hitObject.toString());
				Log.error(message, e);
				bubNotifs.send(BUB_RED, message);
				gameObjects[i] = new DummyObject(hitObject);
			}
		}

		// stack calculations
		calculateStacks();

		// load the first timingPoint
		timingPointIndex = 0;
		beatLengthBase = beatLength = 1;
		if (!beatmap.timingPoints.isEmpty()) {
			TimingPoint timingPoint = beatmap.timingPoints.get(0);
			if (!timingPoint.isInherited()) {
				setBeatLength(timingPoint, true);
				timingPointIndex++;
			}
		}

		// unhide cursor for "auto" mod and replays
		if (GameMod.AUTO.isActive() || isReplay) {
			GLHelper.showNativeCursor();
		}

		// load replay frames
		if (isReplay) {
			// load initial data
			replayX = width2;
			replayY = height2;
			replayKeyPressed = false;
			replaySkipTime = -1;
			for (replayIndex = 0; replayIndex < replay.frames.length; replayIndex++) {
				ReplayFrame frame = replay.frames[replayIndex];
				if (frame.getY() < 0) {  // skip time (?)
					if (frame.getTime() >= 0 && replayIndex > 0)
						replaySkipTime = frame.getTime();
				} else if (frame.getTime() == 0) {
					replayX = frame.getScaledX();
					replayY = frame.getScaledY();
					replayKeyPressed = frame.isKeyPressed();
				} else
					break;
			}
		}

		// initialize replay-recording structures
		else {
			lastKeysPressed = ReplayFrame.KEY_NONE;
			replaySkipTime = -1;
			replayFrames = new LinkedList<>();
			replayFrames.add(new ReplayFrame(0, 0, mouseX, mouseY, 0));
		}

		for (int i = 0; i < gameObjects.length; i++) {
			gameObjects[i].updateStartEndPositions(beatmap.objects[i].getTime());
		}

		leadInTime = beatmap.audioLeadIn + approachTime;

		// fetch previous scores
		previousScores = ScoreDB.getMapScoresExcluding(beatmap, replay == null ? null : replay.getReplayFilename());
		lastRankUpdateTime = -1000;
		if (previousScores != null)
			currentRank = previousScores.length;
		scoreboardVisible = true;
		currentScoreboardAlpha = 0f;

		// using local offset?
		if (beatmap.localMusicOffset != 0)
			barNotifs.send(String.format("Using local beatmap offset (%dms)", beatmap.localMusicOffset));

		// needs to play before setting position to resume without lag later
		MusicController.play(false);
		MusicController.setPosition(0);
		MusicController.setPitch(GameMod.getSpeedMultiplier());
		MusicController.pause();

		if (gameObjects.length > 0) {
			int leadIntime = OPTION_MAP_START_DELAY.val * 100 - gameObjects[0].getTime();
			if (leadIntime > 0) {
				this.leadInTime = Math.max(leadIntime, this.leadInTime);
			}
		}
		this.leadInTime += epiImgTime;
		SoundController.mute(false);

		if (!OPTION_FALLBACK_SLIDERS.state && OPTION_MERGING_SLIDERS.state) {
			if (!OPTION_SHRINKING_SLIDERS.state) {
				knorkesliders = null; // workaround for issue-130
			}
			if (knorkesliders == null) {
				// let's create knorkesliders
				int totalpoints = 0;
				ArrayList<Slider> sliders = new ArrayList<>();
				for (GameObject o : gameObjects) {
					if (o.isSlider()) {
						final Slider s = (Slider) o;
						final int len = s.getCurvePointsCount();
						if (len > 0) {
							sliders.add(s);
							s.curveStartIndex = totalpoints;
							totalpoints += s.getCurvePointsCount();
						}
					}
				}
				if (totalpoints > 0) {
					Vec2f[] combinedcurve = new Vec2f[totalpoints];
					int idx = 0;
					for (Slider s : sliders) {
						int len = s.getCurvePointsCount();
						arraycopy(s.getCurvePoints(), 0, combinedcurve, idx, len);
						idx += len;
					}
					knorkesliders = new FakeCombinedCurve(combinedcurve);
				}
			} else {
				int startIndex = 0;
				for (GameObject gameObject : gameObjects) {
					if (gameObject.isSlider()) {
						final Slider s = (Slider) gameObject;
						s.curveStartIndex = startIndex;
						startIndex += s.getCurvePointsCount();
					}
				}
			}
		}

		Dancer.instance.setGameObjects(gameObjects);
		storyboardOverlay.setGameObjects(gameObjects);
		if (!skippedToCheckpoint) {
			storyboardOverlay.onEnter();
			storyboardOverlay.updateIndex(0);
		}

		Pippi.reset();
		mirrorFrom = 0;
		mirrorTo = gameObjects.length;

		skipButton.resetHover();
		if (isReplay || GameMod.AUTO.isActive())
			playbackSpeed.getButton().resetHover();
		MusicController.setPitch(GameMod.getSpeedMultiplier() * playbackSpeed.getModifier());
	}

	@Override
	public void leave()
	{
		super.leave();

		Display.setTitle(Constants.PROJECT_NAME);

		this.pauseOverlay.hide();

		displayContainer.cursor.reset();
		displayContainer.drawCursor = true;

		MusicController.pause();
		MusicController.setPitch(1f);
		MusicController.resume();

		if (OPTION_DANCE_ENABLE_SB.state) {
			storyboardOverlay.onLeave();
		}

		optionsOverlay.hide();

		isInGame = false;
//		container.setMouseGrabbed(false);
		skippedToCheckpoint = false;

		knorkesliders = null;

		Dancer.instance.setGameObjects(null);

		CursorImpl.lastObjColor = Color.white;
		CursorImpl.lastMirroredObjColor = Color.white;
		CursorImpl.nextObjColor = Color.white;
		CursorImpl.nextMirroredObjColor = Color.white;

		// re-hide cursor
		if (GameMod.AUTO.isActive() || isReplay) {
			GLHelper.hideNativeCursor();
		}

		// replays
		if (isReplay)
			GameMod.loadModState(previousMods);
	}

	void adjustLocalMusicOffset(int amount)
	{
		int newOffset = beatmap.localMusicOffset + amount;
		barNotifs.send(String.format("Local beatmap offset set to %dms", newOffset));
		if (beatmap.localMusicOffset != newOffset) {
			beatmap.localMusicOffset = newOffset;
			BeatmapDB.updateLocalOffset(beatmap);
		}
	}
	public void addMergedSliderPointsToRender(int from, int to) {
		knorkesliders.addRange(from, to);
	}

	/**
	 * Draws hit objects, hit results, and follow points.
	 * @param g the graphics context
	 * @param trackPosition the track position
	 */
	private void drawHitObjects(Graphics g, int trackPosition) {
		gameObjectRenderer.initForFrame();

		// draw result objects
		if (!OPTION_DANCE_HIDE_OBJECTS.state) {
			data.drawHitResults(trackPosition);
		}

		if (!OPTION_FALLBACK_SLIDERS.state && OPTION_MERGING_SLIDERS.state && knorkesliders != null) {
			knorkesliders.draw(Color.white);
			knorkesliders.initForFrame();
			/*
			if (OPTION_DANCE_MIRROR.state) {
				g.pushTransform();
				g.rotate(Options.width / 2f, Options.height / 2f, 180f);
				knorkesliders.draw(Color.white, this.slidercurveFrom, this.slidercurveTo);
				g.popTransform();
			}
			*/
		}

		// include previous object in follow points
		int lastObjectIndex = -1;
		if (objectIndex > 0 && objectIndex < beatmap.objects.length &&
		    trackPosition < beatmap.objects[objectIndex].getTime() && !beatmap.objects[objectIndex - 1].isSpinner())
			lastObjectIndex = objectIndex - 1;


		final boolean loseState = (restartReason == RestartReason.LOSE);

		// get hit objects in reverse order, or else overlapping objects are unreadable
		Stack<Integer> stack = new Stack<>();
		for (int index = objectIndex; index < gameObjects.length && beatmap.objects[index].getTime() < trackPosition + approachTime; index++) {
			final int LOSE_LASTOBJ_TIME = failTrackTime + LOSE_FADEOUT_TIME - 500;
			if (loseState &&
				beatmap.objects[index].getTime() > LOSE_LASTOBJ_TIME)
			{
				break;
			}

			stack.add(index);

			// draw follow points
			if (!OPTION_SHOW_FOLLOW_POINTS.state || loseState || !OPTION_DANCE_HIDE_OBJECTS.state)
				continue;
			if (beatmap.objects[index].isSpinner()) {
				lastObjectIndex = -1;
				continue;
			}
			if (lastObjectIndex != -1 && !beatmap.objects[index].isNewCombo()) {
				// calculate points
				final int followPointInterval = height / 14;
				int lastObjectEndTime = gameObjects[lastObjectIndex].getEndTime() + 1;
				int objectStartTime = beatmap.objects[index].getTime();
				Vec2f startPoint = gameObjects[lastObjectIndex].getPointAt(lastObjectEndTime);
				Vec2f endPoint = gameObjects[index].getPointAt(objectStartTime);
				float xDiff = endPoint.x - startPoint.x;
				float yDiff = endPoint.y - startPoint.y;
				float dist = (float) Math.hypot(xDiff, yDiff);
				int numPoints = (int) ((dist - GameImage.HITCIRCLE.getWidth()) / followPointInterval);
				if (numPoints > 0) {
					// set the image angle
					Image followPoint = GameImage.FOLLOWPOINT.getImage();
					float angle = (float) Math.toDegrees(Math.atan2(yDiff, xDiff));
					followPoint.setRotation(angle);

					// draw points
					float progress = 0f, alpha = 1f;
					if (lastObjectIndex < objectIndex)
						progress = (float) (trackPosition - lastObjectEndTime) / (objectStartTime - lastObjectEndTime);
					else {
						alpha = Utils.clamp((1f - ((objectStartTime - trackPosition) / (float) approachTime)) * 2f, 0, 1);
						followPoint.setAlpha(alpha);
					}

					float step = 1f / (numPoints + 1);
					float t = step;
					for (int i = 0; i < numPoints; i++) {
						float x = startPoint.x + xDiff * t;
						float y = startPoint.y + yDiff * t;
						float nextT = t + step;
						if (lastObjectIndex < objectIndex) {  // fade the previous trail
							if (progress < nextT) {
								if (progress > t)
									followPoint.setAlpha(1f - ((progress - t + step) / (step * 2f)));
								else if (progress > t - step)
									followPoint.setAlpha(1f - ((progress - (t - step)) / (step * 2f)));
								else
									followPoint.setAlpha(1f);
								followPoint.drawCentered(x, y);
							}
						} else
							followPoint.drawCentered(x, y);
						t = nextT;
					}
					followPoint.setAlpha(1f);
				}
			}
			lastObjectIndex = index;
		}

		// draw hit objects
		while (!stack.isEmpty()){
			int idx = stack.pop();
			GameObject gameObj = gameObjects[idx];

			// normal case
			if (!loseState) {
				if (!OPTION_DANCE_HIDE_OBJECTS.state) {
					gameObj.draw(g, trackPosition, 0f);
					if (OPTION_DANCE_MIRROR.state && GameMod.AUTO.isActive() && idx < mirrorTo && idx >= mirrorFrom) {
						for (int i = 1; i < OPTION_MERGING_SLIDERS_MIRROR_POOL.val; i++) {
							float angle = 360.0f / OPTION_MERGING_SLIDERS_MIRROR_POOL.val * i;
							g.pushTransform();
							g.rotate(width2, height2, -angle);
							gameObj.draw(g, trackPosition, angle);
							g.popTransform();
						}
					}
				}
			}
			// death: make objects "fall" off the screen
			else {
				// time the object began falling
				int objTime = Math.max(beatmap.objects[idx].getTime() - approachTime, failTrackTime);
				float dt = (trackPosition - objTime) / (float) (MUSIC_FADEOUT_TIME);

				// would the object already be visible?
				if (dt <= 0)
					continue;

				// generate rotation speeds for each objects
				final float rotSpeed;
				if (rotations.containsKey(gameObj)) {
					rotSpeed = rotations.get(gameObj);
				} else {
					rotSpeed = (float) (2.0f * (Math.random() - 0.5f) * MAX_ROTATION);
					rotations.put(gameObj, rotSpeed);
				}

				g.pushTransform();

				// translate and rotate the object
				g.translate(0, dt * dt * height);
				Vec2f rotationCenter = gameObj.getPointAt((beatmap.objects[idx].getTime() + beatmap.objects[idx].getEndTime()) / 2);
				g.rotate(rotationCenter.x, rotationCenter.y, rotSpeed * dt);
				gameObj.draw(g, trackPosition, 0f);

				g.popTransform();
			}
		}
	}

	/**
	 * Loads all required data from a beatmap.
	 * @param beatmap the beatmap to load
	 */
	public void loadBeatmap(Beatmap beatmap) {
		this.beatmap = beatmap;
		if (beatmap.breaks == null) {
			BeatmapDB.load(beatmap, BeatmapDB.LOAD_ARRAY);
		}
		BeatmapParser.parseHitObjects(beatmap);
		HitSound.setDefaultSampleSet(beatmap.sampleSet);
	}

	/**
	 * Resets all game data and structures.
	 */
	public void resetGameData() {
		if (data != null) {
			data.clear();
		}
		objectIndex = 0;
		breakIndex = 0;
		breakTime = 0;
		breakSound = false;
		timingPointIndex = 0;
		beatLengthBase = beatLength = 1;
		countdownReadySound = false;
		countdown3Sound = false;
		countdown1Sound = false;
		countdown2Sound = false;
		countdownGoSound = false;
		checkpointLoaded = false;
		deaths = 0;
		deathTime = -1;
		replayFrames = null;
		lastReplayTime = 0;
		autoMousePosition = new Vec2f();
		autoMousePressed = false;
		flashlightRadius = height * 2 / 3;
		if (scoreboardStarStream != null) {
			scoreboardStarStream.clear();
		}
		gameFinished = false;
		gameFinishedTimer.setTime(0);

		System.gc();
	}

	/**
	 * Skips the beginning of a track.
	 * @return {@code true} if skipped, {@code false} otherwise
	 */
	private synchronized boolean skipIntro() {
		int trackPosition = MusicController.getPosition();
		if (objectIndex == 0 && (trackPosition < firstObjectTime - SKIP_OFFSET) || isLeadIn()) {
			if (isLeadIn()) {
				leadInTime = 0;
				epiImgTime = 0;
				MusicController.resume();
			}
			MusicController.setPosition(Math.max(0, firstObjectTime - SKIP_OFFSET));
			MusicController.setPitch(GameMod.getSpeedMultiplier() * playbackSpeed.getModifier());
			replaySkipTime = (isReplay) ? -1 : trackPosition;
			if (isReplay) {
				replayX = (int) skipButton.getX();
				replayY = (int) skipButton.getY();
			}
			SoundController.playSound(SoundEffect.MENUHIT);
			return true;
		}
		return false;
	}

	/**
	 * Loads all game images.
	 */
	private void loadImages() {
		// set images
		File parent = beatmap.getFile().getParentFile();
		for (GameImage img : GameImage.values()) {
			if (img.isBeatmapSkinnable()) {
				img.setDefaultImage();
				img.setBeatmapSkinImage(parent);
			}
		}

		// skip button
		if (GameImage.SKIP.getImages() != null) {
			Animation skip = GameImage.SKIP.getAnimation(120);
			skipButton = new MenuButton(skip, width - skip.getWidth() / 2f, height - (skip.getHeight() / 2f));
		} else {
			Image skip = GameImage.SKIP.getImage();
			skipButton = new MenuButton(skip, width - skip.getWidth() / 2f, height - (skip.getHeight() / 2f));
		}
		skipButton.setHoverAnimationDuration(350);
		skipButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
		skipButton.setHoverExpand(1.1f, MenuButton.Expand.UP_LEFT);

		// load other images...
		data.loadImages();
	}

	/**
	 * Set map modifiers.
	 */
	private void setMapModifiers() {
		// fixed difficulty overrides
		float circleSize = OPTION_FIXED_CS.val / 10f;
		float approachRate = OPTION_FIXED_AR.val / 10f;
		float overallDifficulty = OPTION_FIXED_OD.val / 10f;
		float HPDrainRate = OPTION_FIXED_HP.val / 10f;

		// map-based properties, re-initialized each game
		float multiplier = GameMod.getDifficultyMultiplier();
		if (circleSize == 0f) {
			circleSize = Math.min(beatmap.circleSize * multiplier, 10f);
		}
		if (approachRate == 0f) {
			approachRate = Math.min(beatmap.approachRate * multiplier, 10f);
		}
		if (overallDifficulty == 0f) {
			overallDifficulty = Math.min(beatmap.overallDifficulty * multiplier, 10f);
		}
		if (HPDrainRate == 0f) {
			HPDrainRate = Math.min(beatmap.HPDrainRate * multiplier, 10f);
		}

		// Stack modifier scales with hit object size
		// StackOffset = HitObjectRadius / 10
		//int diameter = (int) (104 - (circleSize * 8));
		float diameter = 108.848f - (circleSize * 8.9646f);
		HitObject.setStackOffset(diameter * STACK_OFFSET_MODIFIER);

		// initialize objects
		gameObjectRenderer.initForGame(data, diameter);
		Slider.init(gameObjectRenderer.circleDiameter, beatmap);
		Spinner.init(overallDifficulty);
		Color sliderBorderColor = SkinService.skin.getSliderBorderColor();
		if (!OPTION_IGNORE_BEATMAP_SKINS.state && beatmap.getSliderBorderColor() != null) {
			sliderBorderColor = beatmap.getSliderBorderColor();
		}
		Curve.init(diameter, sliderBorderColor);

		// approachRate (hit object approach time)
		if (approachRate < 5)
			approachTime = (int) (1800 - (approachRate * 120));
		else
			approachTime = (int) (1200 - ((approachRate - 5) * 150));

		// overallDifficulty (hit result time offsets)
		hitResultOffset = new int[GameData.HIT_MAX];
		hitResultOffset[GameData.HIT_300]  = (int) (79.5f - (overallDifficulty * 6));
		hitResultOffset[GameData.HIT_100]  = (int) (139.5f - (overallDifficulty * 8));
		hitResultOffset[GameData.HIT_50]   = (int) (199.5f - (overallDifficulty * 10));
		hitResultOffset[GameData.HIT_MISS] = (int) (500 - (overallDifficulty * 10));
		//final float mult = 0.608f;
		//hitResultOffset[GameData.HIT_300]  = (int) ((128 - (overallDifficulty * 9.6)) * mult);
		//hitResultOffset[GameData.HIT_100]  = (int) ((224 - (overallDifficulty * 12.8)) * mult);
		//hitResultOffset[GameData.HIT_50]   = (int) ((320 - (overallDifficulty * 16)) * mult);
		//hitResultOffset[GameData.HIT_MISS] = (int) ((1000 - (overallDifficulty * 10)) * mult);
		data.setHitResultOffset(hitResultOffset);

		// HPDrainRate (health change)
		data.setDrainRate(HPDrainRate);

		// difficulty multiplier (scoring)
		data.calculateDifficultyMultiplier(beatmap.HPDrainRate, beatmap.circleSize, beatmap.overallDifficulty);

		// hit object fade-in time (TODO: formula)
		fadeInTime = Math.min(375, (int) (approachTime / 2.5f));

		// fade times ("Hidden" mod)
		// TODO: find the actual formulas for this
		hiddenDecayTime = (int) (approachTime / 3.6f);
		hiddenTimeDiff = (int) (approachTime / 3.3f);
	}

	/**
	 * Returns whether or not the track is in the lead-in time state.
	 */
	public boolean isLeadIn() { return leadInTime > 0; }

	public boolean isBreak()
	{
		return breakTime > 0;
	}

	public boolean isLeadInOrBreak()
	{
		return leadInTime + breakTime > 0;
	}

	/**
	 * Returns the object approach time, in milliseconds.
	 */
	public int getApproachTime() { return approachTime; }

	/**
	 * Returns the amount of time for hit objects to fade in, in milliseconds.
	 */
	public int getFadeInTime() { return fadeInTime; }

	/**
	 * Returns the object decay time in the "Hidden" mod, in milliseconds.
	 */
	public int getHiddenDecayTime() { return hiddenDecayTime; }

	/**
	 * Returns the time before the hit object time by which the objects have
	 * completely faded in the "Hidden" mod, in milliseconds.
	 */
	public int getHiddenTimeDiff() { return hiddenTimeDiff; }

	/**
	 * Returns an array of hit result offset times, in milliseconds (indexed by GameData.HIT_* constants).
	 */
	public int[] getHitResultOffsets() { return hitResultOffset; }

	/**
	 * Returns the beat length.
	 */
	public float getBeatLength() { return beatLength; }

	public float getBeatLengthBase() {
		return beatLengthBase;
	}

	/**
	 * Sets the beat length fields based on a given timing point.
	 * @param timingPoint the timing point
	 * @param setSampleSet whether to set the hit sample set based on the timing point
	 */
	private void setBeatLength(TimingPoint timingPoint, boolean setSampleSet) {
		if (!timingPoint.isInherited())
			beatLengthBase = beatLength = timingPoint.getBeatLength();
		else
			beatLength = beatLengthBase * timingPoint.getSliderMultiplier();
		if (setSampleSet) {
			HitSound.setDefaultSampleSet(timingPoint.getSampleType());
			SoundController.setSampleVolume(timingPoint.getSampleVolume());
		}
	}

	/**
	 * Returns the slider multiplier given by the current timing point.
	 */
	public float getTimingPointMultiplier() { return beatLength / beatLengthBase; }

	/**
	 * Sets a replay to view, or resets the replay if null.
	 * @param replay the replay
	 */
	public void setReplay(Replay replay) {
		if (replay == null) {
			this.isReplay = false;
			this.replay = null;
			return;
		}

		if (replay.frames == null) {
			bubNotifs.send(BUB_ORANGE, "Attempting to set a replay with no frames.");
			return;
		}

		this.isReplay = true;
		this.replay = replay;
	}

	/**
	 * Adds a replay frame to the list, if possible, and runs it.
	 * @param x the cursor x coordinate
	 * @param y the cursor y coordinate
	 * @param keys the keys pressed
	 * @param time the time of the replay Frame
	 */
	public synchronized void addReplayFrameAndRun(int x, int y, int keys, int time){
		// "auto" and "autopilot" mods: use automatic cursor coordinates
		if (GameMod.AUTO.isActive() || GameMod.AUTOPILOT.isActive()) {
			x = (int) autoMousePosition.x;
			y = (int) autoMousePosition.y;
		}

		ReplayFrame frame = addReplayFrame(x, y, keys, time);
		if (frame != null)
			runReplayFrame(frame);
	}

	/**
	 * Runs a replay frame.
	 * @param frame the frame to run
	 */
	private void runReplayFrame(ReplayFrame frame){
		int keys = frame.getKeys();
		int replayX = frame.getScaledX();
		int replayY = frame.getScaledY();
		int deltaKeys = (keys & ~lastReplayKeys);  // keys that turned on
		if (deltaKeys != ReplayFrame.KEY_NONE)  // send a key press
			sendGameKeyPress(deltaKeys, replayX, replayY, frame.getTime());
		else if (keys != lastReplayKeys)
			;  // do nothing
		else
			updateGame(replayX, replayY, frame.getTimeDiff(), frame.getTime(), keys);
		lastReplayKeys = keys;
	}

	/**
	 * Sends a game key press and updates the hit objects.
	 * @param trackPosition the track position
	 * @param x the cursor x coordinate
	 * @param y the cursor y coordinate
	 * @param keys the keys that are pressed
	 */
	private void sendGameKeyPress(int keys, int x, int y, int trackPosition) {
		if (objectIndex >= gameObjects.length)  // nothing to do here
			return;

		HitObject hitObject = beatmap.objects[objectIndex];

		// circles
		if (hitObject.isCircle() && gameObjects[objectIndex].mousePressed(x, y, trackPosition))
			objectIndex++;  // circle hit

		// sliders
		else if (hitObject.isSlider())
			gameObjects[objectIndex].mousePressed(x, y, trackPosition);
	}

	/**
	 * Adds a replay frame to the list, if possible.
	 * @param x the cursor x coordinate
	 * @param y the cursor y coordinate
	 * @param keys the keys pressed
	 * @param time the time of the replay frame
	 * @return a ReplayFrame representing the data
	 */
	private ReplayFrame addReplayFrame(int x, int y, int keys, int time) {
		int timeDiff = time - lastReplayTime;
		lastReplayTime = time;
		int cx = (int) ((x - HitObject.getXOffset()) / HitObject.getXMultiplier());
		int cy = (int) ((y - HitObject.getYOffset()) / HitObject.getYMultiplier());
		ReplayFrame frame = new ReplayFrame(timeDiff, time, cx, cy, keys);
		if (replayFrames != null)
			replayFrames.add(frame);
		return frame;
	}

	/**
	 * Updates the current visible area radius (if the "flashlight" mod is enabled).
	 * @param delta the delta interval
	 * @param trackPosition the track position
	 */
	private void updateFlashlightRadius(int delta, int trackPosition) {
		if (!GameMod.FLASHLIGHT.isActive())
			return;

		boolean firstObject = (objectIndex == 0 && trackPosition < beatmap.objects[0].getTime());
		if (isLeadIn()) {
			// lead-in: expand area
			float progress = Math.max((float) (leadInTime - beatmap.audioLeadIn) / approachTime, 0f);
			flashlightRadius = width - (int) ((width - (height * 2 / 3)) * progress);
		} else if (firstObject) {
			// before first object: shrink area
			int timeDiff = beatmap.objects[0].getTime() - trackPosition;
			flashlightRadius = width;
			if (timeDiff < approachTime) {
				float progress = (float) timeDiff / approachTime;
				flashlightRadius -= (width - (height * 2 / 3)) * (1 - progress);
			}
		} else {
			// gameplay: size based on combo
			int targetRadius;
			int combo = data.getComboStreak();
			if (combo < 100)
				targetRadius = height * 2 / 3;
			else if (combo < 200)
				targetRadius = height2;
			else
				targetRadius = height / 3;
			if (beatmap.breaks != null && breakIndex < beatmap.breaks.size() && breakTime > 0) {
				// breaks: expand at beginning, shrink at end
				flashlightRadius = targetRadius;
				int endTime = beatmap.breaks.get(breakIndex);
				int breakLength = endTime - breakTime;
				if (breakLength > approachTime * 3) {
					float progress = 1f;
					if (trackPosition - breakTime < approachTime)
						progress = (float) (trackPosition - breakTime) / approachTime;
					else if (endTime - trackPosition < approachTime)
						progress = (float) (endTime - trackPosition) / approachTime;
					flashlightRadius += (width - flashlightRadius) * progress;
				}
			} else if (flashlightRadius != targetRadius) {
				// radius size change
				float radiusDiff = height * delta / 2000f;
				if (flashlightRadius > targetRadius) {
					flashlightRadius -= radiusDiff;
					if (flashlightRadius < targetRadius)
						flashlightRadius = targetRadius;
				} else {
					flashlightRadius += radiusDiff;
					if (flashlightRadius > targetRadius)
						flashlightRadius = targetRadius;
				}
			}
		}
	}

	/**
	 * Performs stacking calculations on all hit objects, and updates their
	 * positions if necessary.
	 * @author peppy (https://gist.github.com/peppy/1167470)
	 */
	private void calculateStacks() {
		// reverse pass for stack calculation
		for (int i = gameObjects.length - 1; i > 0; i--) {
			HitObject hitObjectI = beatmap.objects[i];

			// already calculated
			if (hitObjectI.getStack() != 0 || hitObjectI.isSpinner())
				continue;

			// search for hit objects in stack
			for (int n = i - 1; n >= 0; n--) {
				HitObject hitObjectN = beatmap.objects[n];
				if (hitObjectN.isSpinner())
					continue;

				// check if in range stack calculation
				float timeI = hitObjectI.getTime() - (STACK_TIMEOUT * beatmap.stackLeniency);
				float timeN = hitObjectN.isSlider() ? gameObjects[n].getEndTime() : hitObjectN.getTime();
				if (timeI > timeN)
					break;

				// possible special case: if slider end in the stack,
				// all next hit objects in stack move right down
				if (hitObjectN.isSlider()) {
					Vec2f p1 = gameObjects[i].getPointAt(hitObjectI.getTime());
					Vec2f p2 = gameObjects[n].getPointAt(gameObjects[n].getEndTime());
					float distance = Utils.distance(p1.x, p1.y, p2.x, p2.y);

					// check if hit object part of this stack
					if (distance < STACK_LENIENCE * HitObject.getXMultiplier()) {
						int offset = hitObjectI.getStack() - hitObjectN.getStack() + 1;
						for (int j = n + 1; j <= i; j++) {
							HitObject hitObjectJ = beatmap.objects[j];
							p1 = gameObjects[j].getPointAt(hitObjectJ.getTime());
							distance = Utils.distance(p1.x, p1.y, p2.x, p2.y);

							// hit object below slider end
							if (distance < STACK_LENIENCE * HitObject.getXMultiplier())
								hitObjectJ.setStack(hitObjectJ.getStack() - offset);
						}
						break;  // slider end always start of the stack: reset calculation
					}
				}

				// not a special case: stack moves up left
				float distance = Utils.distance(
						hitObjectI.getX(), hitObjectI.getY(),
						hitObjectN.getX(), hitObjectN.getY()
				);
				if (distance < STACK_LENIENCE) {
					hitObjectN.setStack(hitObjectI.getStack() + 1);
					hitObjectI = hitObjectN;
				}
			}
		}

		// update hit object positions
		for (int i = 0; i < gameObjects.length; i++) {
			if (beatmap.objects[i].getStack() != 0)
				gameObjects[i].updatePosition();
		}
	}

	/**
	 * Returns true if the coordinates are within the music position bar bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	private boolean musicPositionBarContains(float cx, float cy) {
		return ((cx > musicBarX && cx < musicBarX + musicBarWidth) &&
		        (cy > musicBarY && cy < musicBarY + musicBarHeight));
	}

	public boolean isLosing()
	{
		return this.restartReason == RestartReason.LOSE;
	}

	@Override
	protected void writeStateErrorDump(StringWriter dump)
	{
		dump.write("> Game dump\n");
		dump.write("beatmap: ");
		Beatmap map = this.beatmap;
		if (map != null) {
			dump.write(" setid ");
			dump.write(String.valueOf(map.beatmapSetID));
			dump.write(" beatmapid ");
			dump.write(String.valueOf(map.beatmapID));
			dump.write(" name ");
			dump.write(map.artist);
			dump.write(" - ");
			dump.write(map.title);
			dump.write(" [");
			dump.write(map.version);
			dump.write("]\n");
		} else {
			dump.write("null\n");
		}
		dump.write("objectidx: " + this.objectIndex + "\n");
		dump.write("time: " + MusicController.getPosition() + "ms\n");
	}
}
