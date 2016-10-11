package awlex.ospu.movers.factories;

import awlex.ospu.FakeGameObject;
import awlex.ospu.movers.CombinedSpiralMover;
import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.Mover;
import yugecin.opsudance.movers.factories.MoverFactory;

/**
 * Created by Alex Wieser on 10.10.2016.
 */
public class CenterSpiralMoverFactory implements MoverFactory {

    private static FakeGameObject middle;
    @Override
    public Mover create(GameObject start, GameObject end, int dir) {
        if (middle == null)
            middle = new FakeGameObject();
        middle.setTime(start.getEndTime() + (end.getTime() - start.getEndTime()) / 2);
        return new CombinedSpiralMover(middle, start, end, dir);
    }

    @Override
    public String toString() {
        return "CentralSpiralSpin";
    }
}
