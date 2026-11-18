package com.devaxo.templeoftheaxolotl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

/** Incremental 512x512 underwater encounter perimeter; never fills the arena volume. */
public final class EldritchArenaBuilder {
    private EldritchArenaBuilder() {}
    private static final String PROGRESS = "TotaArenaPerimeterProgress";
    private static final int MIN = -256, MAX = 255;
    private static final int SIDE = MAX - MIN + 1;
    private static final int TOTAL = SIDE * 4;

    public static void tick(ServerLevel level, Entity boss) {
        if (!level.dimension().location().getPath().equals("eldritch_waters")) return;
        var tag = boss.getPersistentData();
        int progress = tag.getInt(PROGRESS);
        if (progress >= TOTAL) return;
        // Four boundary columns per tick: no 180,000-block burst on portal entry.
        int end = Math.min(TOTAL, progress + 4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = progress; i < end; i++) {
            int side = i / SIDE, offset = i % SIDE;
            int x = side == 0 ? MIN + offset : side == 1 ? MAX : side == 2 ? MAX - offset : MIN;
            int z = side == 0 ? MIN : side == 1 ? MIN + offset : side == 2 ? MAX : MAX - offset;
            // The dimension's flat generator supplies the bedrock base and water.
            // Keep walls above the seabed and below the water surface (Y=89).
            for (int y = 5; y <= 89; y++) {
                pos.set(x, y, z);
                if (!level.getBlockState(pos).is(Blocks.BEDROCK))
                    level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 2);
            }
        }
        tag.putInt(PROGRESS, end);
    }
}
