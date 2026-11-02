package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Incremental, conservative Second Fall terrain conversion. Never mines or drops blocks. */
final class SecondFallCorruption {
    private SecondFallCorruption() {}
    private static final int RADIUS = 24;
    private static final int MAX_ATTEMPTS = 18000;

    static int advance(ServerLevel level, BlockPos origin, int cursor) {
        int end = Math.min(MAX_ATTEMPTS, cursor + 160);
        for (int i = cursor; i < end; i++) {
            // Stable irregular spread, without large world scans in a single tick.
            int x = Math.floorMod(i * 7919 + 17, 2 * RADIUS + 1) - RADIUS;
            int z = Math.floorMod(i * 3571 + (i / 49) * 13, 2 * RADIUS + 1) - RADIUS;
            double distance = Math.sqrt(x*x + z*z);
            int hash = mix(x, z);
            if (distance > RADIUS * (0.83 + (hash & 255) / 1500.0)) continue;
            if (!level.getChunkSource().hasChunk(origin.getX() + x >> 4, origin.getZ() + z >> 4)) continue;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    origin.getX() + x, origin.getZ() + z) - 1;
            if (Math.abs(y - origin.getY()) > 16) continue;
            BlockPos surface = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
            // Chunk was checked before the heightmap query; do not load distant chunks.
            // Convert a shallow, broken crust only; no caves, player builds, inventories or machinery.
            for (int depth = 0; depth < 3; depth++) {
                BlockPos pos = surface.below(depth);
                BlockState state = level.getBlockState(pos);
                if (state.hasBlockEntity() || level.getBlockEntity(pos) != null) continue;
                if (!isNatural(state)) continue;
                // Do not change supports immediately below protected / non-natural structures.
                if (depth == 0 && !isSafeAbove(level, pos.above())) continue;
                int roll = Math.floorMod(mix(x + depth * 47, z - depth * 19), 100);
                BlockState replacement = roll < 55 ? Blocks.BLACKSTONE.defaultBlockState()
                        : roll < 67 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                        : roll < 77 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : roll < 85 ? Blocks.NETHER_BRICKS.defaultBlockState()
                        : roll < 92 ? Blocks.RED_NETHER_BRICKS.defaultBlockState()
                        : roll < 97 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : Blocks.GILDED_BLACKSTONE.defaultBlockState();
                // Flag 2 sends changes to clients without ordinary neighbor update propagation.
                level.setBlock(pos, replacement, 2);
            }
        }
        return end;
    }

    private static boolean isNatural(BlockState s) {
        return s.is(Blocks.STONE) || s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK)
                || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE)
                || s.is(Blocks.DEEPSLATE) || s.is(Blocks.TUFF) || s.is(Blocks.COBBLED_DEEPSLATE);
    }
    private static boolean isSafeAbove(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.SNOW) || s.is(Blocks.DEAD_BUSH);
    }
    private static int mix(int x, int z) {
        int h = x * 0x1f123bb5 ^ z * 0x5f356495;
        h ^= h >>> 16; h *= 0x7feb352d; h ^= h >>> 15;
        return h;
    }
}
