package com.devaxo.templeoftheaxolotl.water;

/** Deterministic wave and rigid-hull sampling, independent of the game renderer. */
public final class WaterSurfacePhysics {
    private WaterSurfacePhysics() {}
    public record HullSample(double centerHeight, double frontHeight, double rearHeight,
                             double leftHeight, double rightHeight, double pitchDegrees,
                             double rollDegrees) {}

    /** Coordinates use Minecraft's yaw convention: yaw=0 faces positive Z. */
    public static HullSample sample(double centerX, double centerZ, double yawDegrees,
                                    double ticks, double depth, double hullLength,
                                    double hullWidth, double stillSurfaceY) {
        if (!(hullLength > 0.0) || !(hullWidth > 0.0))
            throw new IllegalArgumentException("Hull dimensions must be positive");
        double radians = Math.toRadians(yawDegrees);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double rightX = Math.cos(radians);
        double rightZ = Math.sin(radians);
        double halfL = hullLength * 0.5;
        double halfW = hullWidth * 0.5;
        double center = height(centerX, centerZ, ticks, depth, stillSurfaceY);
        double front = height(centerX + forwardX * halfL, centerZ + forwardZ * halfL, ticks, depth, stillSurfaceY);
        double rear = height(centerX - forwardX * halfL, centerZ - forwardZ * halfL, ticks, depth, stillSurfaceY);
        double left = height(centerX - rightX * halfW, centerZ - rightZ * halfW, ticks, depth, stillSurfaceY);
        double right = height(centerX + rightX * halfW, centerZ + rightZ * halfW, ticks, depth, stillSurfaceY);
        double pitch = Math.toDegrees(Math.atan2(front - rear, hullLength));
        double roll = Math.toDegrees(Math.atan2(right - left, hullWidth));
        return new HullSample(center, front, rear, left, right, pitch, roll);
    }

    public static double height(double x, double z, double ticks, double depth, double stillSurfaceY) {
        return stillSurfaceY + WaterWaveMath.surfaceOffset(x, z, ticks, depth);
    }

    /** Smooth damped acceleration; caller must integrate velocity with collision checks. */
    public static double buoyancyAcceleration(double actualY, double targetY,
                                               double verticalSpeed, double spring,
                                               double damping, double maxAcceleration) {
        if (!Double.isFinite(actualY) || !Double.isFinite(targetY) || !Double.isFinite(verticalSpeed)) return 0.0;
        double raw = (targetY - actualY) * spring - verticalSpeed * damping;
        return Math.max(-maxAcceleration, Math.min(maxAcceleration, raw));
    }
}
