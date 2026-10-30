package com.devaxo.templeoftheaxolotl.water;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * Shared-corner sampling for the VANILLA chunk-baked water top mesh.
 * No new render layer, no shader, no changes to water blocks or boat physics.
 *
 * IMPORTANT: This mesh animates only on chunk rebuilds. This is a bounded test,
 * not a true per-frame GPU water animation.
 */
public final class WaterSurfaceVisuals {
    private static final int MAX_DEPTH = 64;
    private static volatile long visualGameTicks = 0L;

    public static void setVisualGameTicks(long ticks) { visualGameTicks = ticks; }
    private static final int SHORE_RADIUS = 3;
    private WaterSurfaceVisuals() {}

    public static boolean isExposedWater(BlockAndTintGetter level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER)
                && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    private static int waterDepth(BlockAndTintGetter level, BlockPos.MutableBlockPos scan,
                                  int x, int y, int z) {
        int depth = 0;
        for (int i = 0; i < MAX_DEPTH; i++) {
            scan.set(x, y - i, z);
            if (!level.getFluidState(scan).is(FluidTags.WATER)) break;
            depth++;
        }
        return depth;
    }

    /**
     * All water tiles meeting at a corner must calculate exactly the same
     * height. Therefore use the world-grid corner and the four columns
     * sharing it, never the current tile's center or per-tile depth.
     *
     * The caller must supply world coordinates for a top-face vertex.
     */
    public static double sharedCornerOffset(BlockAndTintGetter level, BlockPos surface,
                                             double worldX, double worldZ) {
        // Vanilla top faces use integer world-grid corners. Reject unexpected
        // non-corner vertices rather than distorting flowing-water geometry.
        int cornerX = (int) Math.round(worldX);
        int cornerZ = (int) Math.round(worldZ);
        if (Math.abs(worldX - cornerX) > 0.02 || Math.abs(worldZ - cornerZ) > 0.02)
            return 0.0;

        int y = surface.getY();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double depthSum = 0.0;
        int count = 0;
        // Exactly four columns touch a world-grid corner.
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                int x = cornerX + dx;
                int z = cornerZ + dz;
                cursor.set(x, y, z);
                if (!isExposedWater(level, cursor)) continue;
                depthSum += waterDepth(level, cursor, x, y, z);
                count++;
            }
        }
        if (count == 0) return 0.0;
        double depth = depthSum / count;
        if (depth <= 2.0) return 0.0;

        double shore = shorelineFactor(level, y, cornerX, cornerZ);
        return WaterWaveMath.surfaceOffset(cornerX, cornerZ, visualGameTicks, depth) * shore;
    }

    /** Shared corner amplitude for the shader marker; independent of animation time. */
    public static double sharedCornerAmplitude(BlockAndTintGetter level, BlockPos surface,
                                                double worldX, double worldZ) {
        int cornerX = (int)Math.round(worldX), cornerZ = (int)Math.round(worldZ);
        if (Math.abs(worldX - cornerX) > 0.02 || Math.abs(worldZ - cornerZ) > 0.02) return 0.0;
        int y = surface.getY();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double depth = 0.0;
        int count = 0;
        for (int dx = -1; dx <= 0; dx++) for (int dz = -1; dz <= 0; dz++) {
            int x = cornerX + dx, z = cornerZ + dz;
            cursor.set(x, y, z);
            if (!isExposedWater(level, cursor)) continue;
            depth += waterDepth(level, cursor, x, y, z);
            count++;
        }
        if (count == 0) return 0.0;
        return WaterWaveMath.amplitudeForDepth(depth / count) * shorelineFactor(level, y, cornerX, cornerZ);
    }

    /**
     * Only solid terrain anchors a shoreline. An exposed water/air boundary
     * is not treated as a solid bank. Fade over three blocks.
     */
    private static double shorelineFactor(BlockAndTintGetter level, int y,
                                           int cornerX, int cornerZ) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double nearest = SHORE_RADIUS + 1.0;
        for (int dx = -SHORE_RADIUS; dx <= SHORE_RADIUS; dx++) {
            for (int dz = -SHORE_RADIUS; dz <= SHORE_RADIUS; dz++) {
                double distance = Math.hypot(dx, dz);
                if (distance >= nearest) continue;
                cursor.set(cornerX + dx, y, cornerZ + dz);
                if (!level.getFluidState(cursor).is(FluidTags.WATER)
                        && level.getBlockState(cursor).isCollisionShapeFullBlock(level, cursor)) {
                    nearest = distance;
                }
            }
        }
        double t = Math.max(0.0, Math.min(1.0, (nearest - 0.5) / 2.5));
        return t * t * (3.0 - 2.0 * t);
    }
}
