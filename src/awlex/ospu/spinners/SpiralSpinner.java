package awlex.ospu.spinners;

import itdelatrisu.opsu.Utils;
import yugecin.opsudance.spinners.Spinner;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * Created by Alex Wieser on 09.10.2016.
 * WHO DO YOU THINK I AM?
 */
public class SpiralSpinner extends Spinner {

    /**
     * How many points the Spinner uses.
     * if there are not enough points the spinner looks angular.
     * if there are too many points the spinner doesn't operate smooth
     */
    private final int SIZE = 100;

    /**
     * The density of the spinner
     * Determines how many times the spinner will around the center.
     * if the value is to high the spinner will become angular.
     * if this value goes under 2 the spinner won't be complete
     */
    private final int DENSITY = 10;

    /**
     * How much the spinner will be rotated.
     * Negative = clockwise
     * Positive = counter clockwise
     */
    private final double DELTA = -Math.PI / 20;
    private int MAX_RAD;
    private int index;
    double[][] points;
    boolean down;

    @Override
    public void init() {
        points = new double[SIZE][];
        double ang;
        double rad;
        for (int i = 0; i < SIZE / 2; i++) {
            MAX_RAD = (int) (height * .35);
            ang = (DENSITY * (Math.PI / SIZE) * i);
            rad = (MAX_RAD / (SIZE / 2)) * i;
            points[SIZE / 2 - 1 - i] = new double[]{
                    width2 + rad * Math.cos(ang),
                    height2 + rad * Math.sin(ang)
            };
            points[SIZE / 2 + i] = new double[]{
                    width2 + rad * (Math.cos(ang) * Math.cos(Math.PI) - Math.sin(ang) * Math.sin(Math.PI)),
                    height2 + rad * -Math.sin(ang)
            };
        }
    }

    @Override
    public String toString() {
        return "Spiralspinner";
    }

    @Override
    public double[] getPoint() {
        //if (waitForDelay()) {
            if (down) {
                if (--index == 0)
                    down = !down;
            } else if (++index == SIZE - 1)
                down = !down;

            if (!down && index == 1) {
                rotatePointAroundCenter(points[0], DELTA);
            } else if (down && index == SIZE - 2) {
                rotatePointAroundCenter(points[SIZE - 1], DELTA);
            }
        //}
        rotatePointAroundCenter(points[index], DELTA);
        return points[index];
    }

    private void rotatePointAroundCenter(double[] point, double beta) {
        double angle = Math.atan2(point[1] - height2, point[0] - width2);
        double rad = Utils.distance(point[0], point[1], width2, height2);

        //rotationMatrix
        point[0] = width2 + rad * (Math.cos(angle) * Math.cos(beta) - Math.sin(angle) * Math.sin(beta));
        point[1] = height2 + rad * (Math.cos(angle) * Math.sin(beta) + Math.sin(angle) * Math.cos(beta));
    }


}
