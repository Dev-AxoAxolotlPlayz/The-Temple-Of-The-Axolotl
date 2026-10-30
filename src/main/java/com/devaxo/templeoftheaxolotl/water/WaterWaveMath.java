package com.devaxo.templeoftheaxolotl.water;

/**
 * Deterministic surface-wave mathematics shared by future client rendering and
 * server-side vehicle movement. No world access, allocations, or client classes.
 * Coordinates are in blocks; time is in Minecraft ticks (partial ticks allowed).
 */
public final class WaterWaveMath {
    private WaterWaveMath() {}

    /** Target maximum displacement in blocks at a sampled water-column depth. */
    public static double amplitudeForDepth(double depth) {
        if (!Double.isFinite(depth) || depth <= 2.0) return 0.0;
        if (depth < 3.0) return 0.3 * smoothstep(depth - 2.0);
        if (depth <= 5.0) return 0.3;
        // Smooth the transition to the next 2-block depth band.
        double extra = (depth - 5.0) / 2.0;
        int fullBands = (int) Math.floor(extra);
        double fraction = extra - fullBands;
        return 0.3 + 0.1 * (fullBands + smoothstep(fraction));
    }

    private static double smoothstep(double x) {
        x = Math.max(0.0, Math.min(1.0, x));
        return x * x * (3.0 - 2.0 * x);
    }

    /**
     * Surface offset in blocks. Pass a spatially smoothed water depth here;
     * sampling/smoothing terrain is deliberately a separate responsibility.
     */
    public static double surfaceOffset(double x, double z, double gameTicks, double smoothedDepth) {
        double amplitude = amplitudeForDepth(smoothedDepth);
        if (amplitude == 0.0) return 0.0;
        double a = Math.sin(0.15 * x + 0.065 * z - 0.045 * gameTicks);
        double b = Math.sin(-0.095 * x + 0.12 * z - 0.032 * gameTicks + 1.4);
        double c = Math.sin(0.055 * x - 0.17 * z - 0.052 * gameTicks + 2.1);
        return amplitude * (0.5 * a + 0.3 * b + 0.2 * c);
    }
}
