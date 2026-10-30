package com.devaxo.templeoftheaxolotl.water;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;

/**
 * Bounded, allocation-light water-column depth sampling for Phase 6.4.
 * Caller supplies the actual water surface block Y (not the entity's Y).
 * Does not load chunks or cache world data. Safe on either logical side.
 */
public final class WaterDepthSampler {
    private WaterDepthSampler() {}

    /** Maximum blocks examined per column; deeper water is capped for performance. */
    public static final int MAX_SCAN_DEPTH = 64;
    private static final int SAMPLE_RADIUS = 2;

    /**
     * Counts consecutive water blocks downward from surfaceY, including surfaceY.
     * Returns zero if the surface block is not water or its chunk is unavailable.
     * A waterlogged non-water block does not count as an open water column.
     */
    public static int columnDepth(Level level, int x, int surfaceY, int z) {
        if (level == null || !level.hasChunk(x >> 4, z >> 4)) return 0;
        int bottom = level.getMinBuildHeight();
        if (surfaceY < bottom || surfaceY >= level.getMaxBuildHeight()) return 0;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int depth = 0;
        for (int y = surfaceY; y >= bottom && depth < MAX_SCAN_DEPTH; y--) {
            pos.set(x, y, z);
            if (!level.getBlockState(pos).getFluidState().is(FluidTags.WATER)
                    || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                break;
            }
            depth++;
        }
        return depth;
    }

    /**
     * Weighted 5x5 neighborhood average, using a triangular kernel.
     * Missing chunks are omitted and weights renormalized. This is spatial
     * smoothing, not a world-changing operation.
     */
    public static double smoothedDepth(Level level, int x, int surfaceY, int z) {
        int center = columnDepth(level, x, surfaceY, z);
        if (center == 0) return 0.0;
        double weighted = 0.0;
        int totalWeight = 0;
        for (int dx = -SAMPLE_RADIUS; dx <= SAMPLE_RADIUS; dx++) {
            for (int dz = -SAMPLE_RADIUS; dz <= SAMPLE_RADIUS; dz++) {
                int sx = x + dx;
                int sz = z + dz;
                if (!level.hasChunk(sx >> 4, sz >> 4)) continue;
                int weight = (SAMPLE_RADIUS + 1 - Math.abs(dx))
                        * (SAMPLE_RADIUS + 1 - Math.abs(dz));
                weighted += weight * columnDepth(level, sx, surfaceY, sz);
                totalWeight += weight;
            }
        }
        return totalWeight == 0 ? center : weighted / totalWeight;
    }

    /** Shared convenience entry point for later rendering and boat integration. */
    public static double waveOffset(Level level, double x, int surfaceY, double z, double gameTicks) {
        return WaterWaveMath.surfaceOffset(x, z, gameTicks,
                smoothedDepth(level, (int) Math.floor(x), surfaceY, (int) Math.floor(z)));
    }
}
