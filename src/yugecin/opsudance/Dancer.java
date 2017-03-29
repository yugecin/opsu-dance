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
package yugecin.opsudance;

import awlex.ospu.movers.factories.CenterSpiralMoverFactory;
import awlex.ospu.movers.factories.SpiralMoverFactory;
import awlex.ospu.polymover.factory.ArcFactory;
import awlex.ospu.polymover.factory.PolyMoverFactory;
import awlex.ospu.spinners.SpiralSpinner;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.objects.DummyObject;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.objects.curves.Vec2f;
import yugecin.opsudance.movers.LinearMover;
import yugecin.opsudance.movers.Mover;
import yugecin.opsudance.movers.QuadraticBezierMover;
import yugecin.opsudance.movers.factories.*;
import yugecin.opsudance.movers.slidermovers.DefaultSliderMoverController;
import yugecin.opsudance.movers.slidermovers.InheritedSliderMoverController;
import yugecin.opsudance.movers.slidermovers.SliderMoverController;
import yugecin.opsudance.render.GameObjectRenderer;
import yugecin.opsudance.spinners.*;

import java.awt.*;

import static yugecin.opsudance.options.Options.*;

public class Dancer {

	public static MoverFactory[] moverFactories = new MoverFactory[] {
		new AutoMoverFactory(),
		new AutoEllipseMoverFactory(),
		new CircleMoverFactory(),
		new HalfCircleMoverFactory(),
		new HalfEllipseMoverFactory(),
		new HalfLowEllipseMoverFactory(),
		new JumpMoverFactory(),
		new LinearMoverFactory(),
		new QuartCircleMoverFactory(),
		new SpiralMoverFactory(),
		new CenterSpiralMoverFactory(),
		//new LinearFactory(),
		new ArcFactory(),
		new QuadraticBezierMoverFactory(),
		new ExgonMoverFactory(),
	};

	public static Spinner[] spinners = new Spinner[] {
		new RektSpinner(),
		new BeamSpinner(),
		new CircleSpinner(),
		new DonutSpinner(),
		new CubeSpinner(),
		new HalfCircleSpinner(),
		new IlluminatiSpinner(),
		new LessThanThreeSpinner(),
		new RektCircleSpinner(),
		new ApproachCircleSpinner(),
		new SpiralSpinner(),
		new FivePointStarSpinner(),
		new FivePointStarApproachSpinner(),
	};

	public static SliderMoverController[] sliderMovers = new SliderMoverController[] {
		new DefaultSliderMoverController(),
		new InheritedSliderMoverController(),
	};

	public static Dancer instance = new Dancer();

	public static boolean multipoint = false;
	public static ObjectColorOverrides colorOverride = ObjectColorOverrides.NONE;
	public static ObjectColorOverrides colorMirrorOverride = ObjectColorOverrides.NONE;
	public static CursorColorOverrides cursorColorOverride = CursorColorOverrides.NONE;
	public static CursorColorOverrides cursorColorMirrorOverride = CursorColorOverrides.NONE;
	public static MoverDirection moverDirection = MoverDirection.RANDOM;

	private int dir;
	public static final GameObject d = new DummyObject();

	private GameObject[] gameObjects;
	private int objectIndex;

	private MoverFactory moverFactory;
	private Mover mover;
	private Spinner spinner;
	public static SliderMoverController sliderMoverController;

	private int moverFactoryIndex;
	private int spinnerIndex;

	public float x;
	public float y;

	private boolean isCurrentLazySlider;

	public Dancer() {
		moverFactory = moverFactories[0];
		spinner = spinners[0];
		sliderMoverController = sliderMovers[0];
	}

	public void reset() {
		isCurrentLazySlider = false;
		objectIndex = -1;
		dir = 1;
		for (Spinner s : spinners) {
			s.init();
		}
		QuadraticBezierMover.reset();
	}

	public int getSpinnerIndex() {
		return spinnerIndex;
	}

	public void setSpinnerIndex(int spinnerIndex) {
		if (spinnerIndex < 0 || spinnerIndex >= spinners.length) {
			spinnerIndex = 0;
		}
		this.spinnerIndex = spinnerIndex;
		spinner = spinners[spinnerIndex];
	}

	public int getMoverFactoryIndex() {
		return moverFactoryIndex;
	}

	public void setMoverFactoryIndex(int moverFactoryIndex) {
		if (moverFactoryIndex < 0 || moverFactoryIndex >= moverFactories.length) {
			moverFactoryIndex = 0;
		}
		this.moverFactoryIndex = moverFactoryIndex;
		moverFactory = moverFactories[moverFactoryIndex];
		multipoint = moverFactory instanceof MoverFactory.MultiPoint;
		// to prevent crashes when changing mover in storyboard, create mover now
		createNewMover();
	}

	public int getPolyMoverFactoryMinBufferSize() {
		if (!multipoint) {
			return 0;
		}
		return ((PolyMoverFactory) moverFactory).getMinBufferSize();
	}

	public void setGameObjects(GameObject[] objs) {
		this.gameObjects = objs;
	}

	public void setObjectIndex(int objectIndex) {
		this.objectIndex = objectIndex;
		// storyboard
		createNewMover();
	}

	public void update(int time, int objectIndex) {
		GameObject p;
		if (objectIndex == 0) {
			p = d;
		} else {
			p = gameObjects[objectIndex - 1];
		}
		GameObject c = gameObjects[objectIndex];
		if (!multipoint) {
			GameObject[] e = sliderMoverController.process(p, c, time);
			p = e[0];
			c = e[1];
		}
		if (this.objectIndex != objectIndex || c != gameObjects[objectIndex]) {
			this.objectIndex = objectIndex;
			if (objectIndex == 0) {
				if (c.isSpinner()) {
					double[] spinnerStartPoint = spinner.getPoint();
					c.start.set((float) spinnerStartPoint[0], (float) spinnerStartPoint[1]);
				}
			}
			isCurrentLazySlider = false;
			// detect lazy sliders, should work pretty good
			if (c.isSlider() && OPTION_DANCE_LAZY_SLIDERS.state && Utils.distance(c.start.x, c.start.y, c.end.x, c.end.y) <= GameObjectRenderer.instance.getCircleDiameter() * 0.8f) {
				Slider s = (Slider) c;
				Vec2f mid = s.getCurve().pointAt(1f);
				if (s.getRepeats() == 1 || Utils.distance(c.start.x, c.start.y, mid.x, mid.y) <= GameObjectRenderer.instance.getCircleDiameter() * 0.8f) {
					mid = s.getCurve().pointAt(0.5f);
					if (Utils.distance(c.start.x, c.start.y, mid.x, mid.y) <= GameObjectRenderer.instance.getCircleDiameter() * 0.8f) {
						isCurrentLazySlider = true;
					}
				}
			}
			dir = moverDirection.getDirection(dir);
			if (c.isSpinner()) {
				double[] spinnerStartPoint = spinner.getPoint();
				c.start = new Vec2f((float) spinnerStartPoint[0], (float) spinnerStartPoint[1]);
			}

			// specific mover stuff
			if (p.isSlider() && sliderMoverController instanceof DefaultSliderMoverController) {
				Vec2f st = p.getPointAt(p.getEndTime() - 10);
				Vec2f en = p.getPointAt(p.getEndTime());
				//double atan = Math.atan2(en.y - st.y, en.x - st.x);
				double distance = Utils.distance(st.x, st.y, en.x, en.y);
				QuadraticBezierMover.p = new Point((int) st.x, (int) st.y);
				QuadraticBezierMover.setPrevspeed(distance, 10);
			}

			createNewMover();
		}

		if (time < c.getTime()) {
			if (!p.isSpinner() || !c.isSpinner()) {
				double[] point;
				if (multipoint) {
					point = ((PolyMoverFactory) moverFactory).getPointAt(time);
				} else {
					point = mover.getPointAt(time);
				}
				x = (float) point[0];
				y = (float) point[1];
			}
		} else {
			if (c.isSpinner()) {
				Spinner.PROGRESS = (double) (time - c.getTime()) / (double) (c.getEndTime() - c.getTime());
				double[] point = spinner.getPoint();
				x = (float) point[0];
				y = (float) point[1];
				c.end = new Vec2f(x, y);
			} else {
				Vec2f point = c.getPointAt(time);
				if (isCurrentLazySlider) {
					point = c.start;
				}
				x = point.x;
				y = point.y;
			}
		}
		Pippi.dance(time, c, isCurrentLazySlider);
		x = Utils.clamp(x, 10, width - 10);
		y = Utils.clamp(y, 10, height - 10);
	}

	private void createNewMover() {
		if (gameObjects == null) {
			return;
		}
		if (objectIndex < 0) {
			objectIndex = 0;
		}
		GameObject c = gameObjects[objectIndex];
		if (multipoint) {
			PolyMoverFactory pmf = (PolyMoverFactory) moverFactory;
			if (pmf.isInitialized() && pmf.getLatestIndex() < objectIndex + pmf.getLatestIndex() - 1) {
				pmf.update(gameObjects[objectIndex + pmf.getMaxBufferSize() - 1]);
			} else {
				pmf.create(gameObjects, objectIndex - 1);
			}
			return;
		}
		GameObject p = d;
		if (objectIndex > 0) {
			p = gameObjects[objectIndex - 1];
		}
		GameObject[] e = sliderMoverController.process(p, c, MusicController.getPosition());
		p = e[0];
		c = e[1];
		if (mover == null || p == d) {
			mover = new LinearMover(d, c, dir);
		} else if (mover.getEnd() != c) {
			mover = moverFactory.create(p, c, dir);
		}
	}

}
