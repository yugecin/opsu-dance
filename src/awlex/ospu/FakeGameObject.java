package awlex.ospu;

/**
 * Created by Awlex on 10.10.2016.
 */

import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.curves.Vec2f;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import static yugecin.opsudance.core.InstanceContainer.displayContainer;

/**
 * This class is just a dummy {@link GameObject} to place in the middle of 2 GameObjects.
 * Might as well ignore it
 */
public class FakeGameObject extends GameObject {

    //Time between the 2 constructor objects
    private int halfTime;

    public FakeGameObject() {
        this.start = new Vec2f();
        this.end = new Vec2f();
        this.start.x = this.end.x = displayContainer.width / 2;
        this.start.y = this.end.y = displayContainer.height / 2;
    }

    public FakeGameObject(GameObject start, GameObject end) {
        halfTime = start.getEndTime() + (end.getTime() - start.getEndTime()) / 2;
        this.start = new Vec2f();
        this.end = new Vec2f();
        this.start.x = this.end.x = (start.end.x + end.start.x) / 2;
        this.start.y = this.end.y = (start.end.y + end.start.y) / 2;
    }

    @Override
    public void draw(Graphics g, int trackPosition, boolean mirrored) {

    }

    @Override
    public boolean update(boolean overlap, int delta, int mouseX, int mouseY, boolean keyPressed, int trackPosition) {
        return false;
    }

    @Override
    public boolean mousePressed(int x, int y, int trackPosition) {
        return false;
    }

    @Override
    public Vec2f getPointAt(int trackPosition) {
        return null;
    }

    @Override
    public int getEndTime() {
        return halfTime;
    }

    @Override
    public int getTime() {
        return halfTime;
    }

    @Override
    public void updatePosition() {

    }

    @Override
    public void reset() {
    }

    @Override
    public boolean isCircle() {
        return false;
    }

    @Override
    public boolean isSlider() {
        return false;
    }

    @Override
    public boolean isSpinner() {
        return false;
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public Color getMirroredColor() {
        return null;
    }

    public void setTime(int time) {
        this.halfTime = time;
    }

}