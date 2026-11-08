package com.devaxo.templeoftheaxolotl.abyss;

import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Ocean-floor-anchored submerged Merolotl ruins. Natural worldgen integration remains separate. */
public final class MerolotlRuins {
    private MerolotlRuins() {}

    // This generator is deliberately Overworld-only. Vanilla Overworld sea level is Y=63.
    private static final int SEA_LEVEL = 63;
    private static final int CENTER_SEARCH_RADIUS = 24;
    private static final int MIN_WATER_DEPTH = 28;
    private static final int MIN_ISLAND_SPACING = 19;

    private static int seabed(ServerLevel level, int x, int z) {
        // OCEAN_FLOOR is the top of the seabed + 1, i.e. the first water block.
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
    }

    private static boolean deepOcean(ServerLevel level, int x, int z) {
        BlockPos probe = new BlockPos(x, SEA_LEVEL - 2, z);
        return level.getBiome(probe).is(BiomeTags.IS_OCEAN)
                && level.getFluidState(probe).is(FluidTags.WATER)
                && SEA_LEVEL - seabed(level, x, z) >= MIN_WATER_DEPTH;
    }

    /** Validate the entire island footprint, not just its center. */
    private static boolean fits(ServerLevel level, int x, int y, int z, boolean central) {
        int radius = central ? 7 : 6;
        // The village builder extends downward from y by approximately 12 blocks.
        int lowest = y - 13;
        // Keep roofs and the central Watergate comfortably submerged.
        if (y + 13 >= SEA_LEVEL) return false;
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                int sx = x + dx, sz = z + dz;
                if (!deepOcean(level, sx, sz)) return false;
                if (seabed(level, sx, sz) >= lowest) return false;
                if (!level.getFluidState(new BlockPos(sx, y + 5, sz)).is(FluidTags.WATER)) return false;
            }
        }
        return true;
    }

    public static int preview(ServerLevel level, BlockPos near) {
        if (!level.dimension().equals(Level.OVERWORLD)) return 0;
        Random random = new Random(level.getRandom().nextLong());

        // Find the deepest usable basin near the requested point. Reject sites where
        // the central island cannot fit without cutting into terrain.
        BlockPos center = null;
        int deepest = Integer.MAX_VALUE;
        for (int dx = -CENTER_SEARCH_RADIUS; dx <= CENTER_SEARCH_RADIUS; dx += 4) {
            for (int dz = -CENTER_SEARCH_RADIUS; dz <= CENTER_SEARCH_RADIUS; dz += 4) {
                int x = near.getX() + dx, z = near.getZ() + dz;
                if (!deepOcean(level, x, z)) continue;
                int floor = seabed(level, x, z);
                int y = floor + 15;
                if (floor < deepest && fits(level, x, y, z, true)) {
                    deepest = floor;
                    center = new BlockPos(x, y, z);
                }
            }
        }
        if (center == null) return 0;

        // Candidate houses are placed by actual clearance, rather than forced into
        // a ring. Skip shallow shelves, terrain intersections, and cramped positions.
        int target = 5 + random.nextInt(6);
        List<BlockPos> islands = new ArrayList<>();
        islands.add(center);
        for (int attempt = 0; attempt < 120 && islands.size() < target; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int radius = 20 + random.nextInt(33);
            int x = center.getX() + (int)Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int)Math.round(Math.sin(angle) * radius);
            if (!deepOcean(level, x, z)) continue;
            int floor = seabed(level, x, z);
            // Buildings follow the local seabed but stay near the central platform.
            int y = floor + 15;
            if (Math.abs(y - center.getY()) > 12 || !fits(level, x, y, z, false)) continue;
            boolean overlaps = false;
            for (BlockPos other : islands) {
                long ddx = (long)x - other.getX(), ddz = (long)z - other.getZ();
                if (ddx * ddx + ddz * ddz < MIN_ISLAND_SPACING * MIN_ISLAND_SPACING) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) islands.add(new BlockPos(x, y, z));
        }

        // All positions are validated before any blocks are placed.
        for (int i = 0; i < islands.size(); i++) {
            BlockPos at = islands.get(i);
            AbyssMerfolkVillages.ruinedIsland(level, at.getX(), at.getY(), at.getZ(),
                    i == 0, random.nextInt(), random.nextInt());
        }

        // Protected central Watergate island and jukebox; do this after ruin damage.
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (dx * dx + dz * dz <= 20) {
                level.setBlock(center.offset(dx, 9, dz), Blocks.DARK_PRISMARINE.defaultBlockState(), 3);
            }
        }
        Direction facing = Direction.NORTH;
        BlockPos controller = center.offset(0, 10, 0);
        level.setBlock(controller, ModBlocks.ABYSSAL_WATERGATE.get().defaultBlockState()
                .setValue(WatergateBlock.FACING, facing)
                .setValue(WatergateBlock.WATERLOGGED,
                        level.getFluidState(controller).is(FluidTags.WATER)), 3);
        level.setBlock(controller.relative(facing, 2), Blocks.JUKEBOX.defaultBlockState(), 3);
        return islands.size();
    }
}
