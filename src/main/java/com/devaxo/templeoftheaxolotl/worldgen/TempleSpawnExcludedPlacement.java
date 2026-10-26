package com.devaxo.templeoftheaxolotl.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;

/**
 * Retains vanilla random-spread placement but filters candidates against the
 * actual Overworld shared spawn position. This is a square, not a radius.
 */
public final class TempleSpawnExcludedPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<TempleSpawnExcludedPlacement> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.INT.fieldOf("spacing").forGetter(TempleSpawnExcludedPlacement::spacing),
                    com.mojang.serialization.Codec.INT.fieldOf("separation").forGetter(TempleSpawnExcludedPlacement::separation),
                    RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(TempleSpawnExcludedPlacement::spreadType),
                    com.mojang.serialization.Codec.INT.fieldOf("salt").forGetter(TempleSpawnExcludedPlacement::configuredSalt)
            ).apply(instance, TempleSpawnExcludedPlacement::new)
    );

    // Temple is 49x49. Add 24 blocks around its center for its full footprint.
    private static final int HALF_FOOTPRINT = 24;
    private static final int INNER_BOUND = 1000;
    private static final int OUTER_BOUND = 15000;
    private final int configuredSalt;

    public TempleSpawnExcludedPlacement(int spacing, int separation, RandomSpreadType spreadType, int salt) {
        super(spacing, separation, spreadType, salt);
        this.configuredSalt = salt;
    }

    public int configuredSalt() { return configuredSalt; }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        if (!super.isPlacementChunk(state, chunkX, chunkZ)) return false;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.overworld() == null) return false;

        BlockPos spawn = server.overworld().getSharedSpawnPos();
        // Chunk center; jigsaw's precise origin may vary within the chunk.
        // 16-block extra padding prevents the template from crossing the boundary.
        ChunkPos candidate = new ChunkPos(chunkX, chunkZ);
        long dx = Math.abs((long) candidate.getMiddleBlockX() - spawn.getX());
        long dz = Math.abs((long) candidate.getMiddleBlockZ() - spawn.getZ());
        long padding = HALF_FOOTPRINT + 16L;

        // Reject if ANY part of the temple could intersect the central square.
        if (dx < INNER_BOUND + padding && dz < INNER_BOUND + padding) return false;
        // Reject if the footprint could cross the outer worldgen square.
        return dx <= OUTER_BOUND - padding && dz <= OUTER_BOUND - padding;
    }

    @Override
    public StructurePlacementType<?> type() {
        return TempleWorldgenPlacements.SPAWN_EXCLUDED.get();
    }
}
