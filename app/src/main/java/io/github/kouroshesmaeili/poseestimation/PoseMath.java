package io.github.kouroshesmaeili.poseestimation;

/** Pure geometry utilities used by the pose overlay. */
public final class PoseMath {
    private static final double MIN_VECTOR_NORM = 1.0e-9;

    private PoseMath() {
        // Utility class.
    }

    /**
     * Returns the angle ABC in degrees, or {@link Double#NaN} when either arm has zero length.
     */
    public static double angleDegrees(Vector3 pointA, Vector3 vertexB, Vector3 pointC) {
        double abX = pointA.x - vertexB.x;
        double abY = pointA.y - vertexB.y;
        double abZ = pointA.z - vertexB.z;
        double cbX = pointC.x - vertexB.x;
        double cbY = pointC.y - vertexB.y;
        double cbZ = pointC.z - vertexB.z;

        double abNorm = Math.sqrt(abX * abX + abY * abY + abZ * abZ);
        double cbNorm = Math.sqrt(cbX * cbX + cbY * cbY + cbZ * cbZ);
        if (abNorm < MIN_VECTOR_NORM || cbNorm < MIN_VECTOR_NORM) {
            return Double.NaN;
        }

        double cosine = (abX * cbX + abY * cbY + abZ * cbZ) / (abNorm * cbNorm);
        double clampedCosine = Math.max(-1.0, Math.min(1.0, cosine));
        return Math.toDegrees(Math.acos(clampedCosine));
    }

    /** Immutable point or vector in ML Kit's relative 3D coordinate space. */
    public static final class Vector3 {
        public final double x;
        public final double y;
        public final double z;

        public Vector3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
