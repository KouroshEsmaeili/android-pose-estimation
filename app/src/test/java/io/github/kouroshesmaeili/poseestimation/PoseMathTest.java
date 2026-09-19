package io.github.kouroshesmaeili.poseestimation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PoseMathTest {
    private static final double TOLERANCE = 1.0e-9;

    @Test
    public void angleDegrees_returnsRightAngle() {
        double angle = PoseMath.angleDegrees(
                point(1.0, 0.0, 0.0),
                point(0.0, 0.0, 0.0),
                point(0.0, 1.0, 0.0)
        );

        assertEquals(90.0, angle, TOLERANCE);
    }

    @Test
    public void angleDegrees_returnsStraightAngle() {
        double angle = PoseMath.angleDegrees(
                point(-1.0, 0.0, 0.0),
                point(0.0, 0.0, 0.0),
                point(1.0, 0.0, 0.0)
        );

        assertEquals(180.0, angle, TOLERANCE);
    }

    @Test
    public void angleDegrees_supportsDepth() {
        double angle = PoseMath.angleDegrees(
                point(0.0, 0.0, 1.0),
                point(0.0, 0.0, 0.0),
                point(1.0, 0.0, 0.0)
        );

        assertEquals(90.0, angle, TOLERANCE);
    }

    @Test
    public void angleDegrees_returnsNaNForZeroLengthArm() {
        PoseMath.Vector3 vertex = point(1.0, 2.0, 3.0);

        double angle = PoseMath.angleDegrees(vertex, vertex, point(2.0, 2.0, 3.0));

        assertTrue(Double.isNaN(angle));
    }

    private static PoseMath.Vector3 point(double x, double y, double z) {
        return new PoseMath.Vector3(x, y, z);
    }
}
