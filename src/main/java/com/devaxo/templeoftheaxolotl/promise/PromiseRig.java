package com.devaxo.templeoftheaxolotl.promise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, geometry-name-based chain definitions for the eight authored tentacles. */
public final class PromiseRig {
    private PromiseRig() {}

    public record Chain(boolean left, int row, List<String> bones) {
        public Chain { bones = List.copyOf(bones); }
        public int size() { return bones.size(); }
        public String root() { return bones.get(0); }
    }

    public static final List<Chain> CHAINS = build();

    private static List<Chain> build() {
        List<Chain> result = new ArrayList<>(8);
        for (boolean left : new boolean[]{true, false}) {
            String side = left ? "Left" : "Right";
            for (int row = 1; row <= 4; row++) {
                List<String> bones = new ArrayList<>(11);
                bones.add(side + "_Root_" + row);
                for (int n = (row - 1) * 7 + 1; n <= row * 7; n++)
                    bones.add("U1_" + side + "_" + n);
                for (int n = (row - 1) * 3 + 1; n <= row * 3; n++)
                    bones.add("U1_" + side + "_Tip_" + n);
                result.add(new Chain(left, row, bones));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Outward walking clearance, applied ONLY to middle-row root bones. */
    public static double walkingRootOffset(Chain chain, double backwardSwingFraction) {
        if (chain.row() != 2 && chain.row() != 3) return 0;
        double fraction = Math.max(0, Math.min(1, backwardSwingFraction));
        return (chain.left() ? -7.5 : 7.5) * fraction;
    }

    /** Dormant-mode only: final five bones open for standard item use. */
    public static boolean opensForItemUse(Chain chain, int boneIndex, boolean tridentCharge) {
        if (tridentCharge && chain.row() == 1) return boneIndex >= chain.size() - 6;
        if (chain.row() == 2 || chain.row() == 3) return boneIndex >= chain.size() - 5;
        return false;
    }

    /** Signed inward bend limits in the exported Blockbench coordinate convention. */
    public static double clampY(Chain chain, double degrees) {
        return chain.left() ? Math.max(0, Math.min(80, degrees))
                : Math.max(-80, Math.min(0, degrees));
    }

    public static double clampX(double degrees) {
        return Math.max(-7.5, Math.min(7.5, degrees));
    }

    /** Root-only Z limits; non-root joints must have zero local Z rotation. */
    public static double clampZ(Chain chain, int boneIndex, double degrees) {
        if (boneIndex != 0) return 0;
        double min = -25, max = 25;
        if (chain.row() == 1) { min = chain.left() ? -15 : -45; max = chain.left() ? 45 : 15; }
        if (chain.row() == 4) { min = chain.left() ? -45 : -15; max = chain.left() ? 15 : 45; }
        return Math.max(min, Math.min(max, degrees));
    }
}
