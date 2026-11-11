package com.devaxo.templeoftheaxolotl.axolite;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;

/** Replace 10% of vanilla redstone/lapis ore blocks, without touching already-generated chunks. */
public final class AxoliteWorldgen {
    private AxoliteWorldgen() {}
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> REPLACE_ORES = FEATURES.register("replace_axolite_ores", () -> new Feature<NoneFeatureConfiguration>(NoneFeatureConfiguration.CODEC) {
        @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            BlockPos origin = context.origin();
            var level = context.level();
            var random = context.random();
            // Placement origins are not guaranteed to be chunk corners.
            // Keep the scan inside the chunk that owns this feature invocation.
            int chunkX = Math.floorDiv(origin.getX(), 16) * 16;
            int chunkZ = Math.floorDiv(origin.getZ(), 16) * 16;
            int changed = 0;
            int minY = level.getMinBuildHeight(), maxY = Math.min(level.getMaxBuildHeight(), 96);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = chunkX; x < chunkX + 16; x++)
                for (int z = chunkZ; z < chunkZ + 16; z++)
                    for (int y = minY; y < maxY; y++) {
                        pos.set(x,y,z);
                        BlockState state = level.getBlockState(pos);
                        boolean stone = state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.LAPIS_ORE);
                        boolean deep = state.is(Blocks.DEEPSLATE_REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE);
                        if ((stone || deep) && random.nextFloat() < 0.10F) {
                            level.setBlock(pos, (deep ? ModBlocks.DEEPSLATE_AXOLITE_ORE : ModBlocks.AXOLITE_ORE).get().defaultBlockState(), 2);
                            changed++;
                        }
                    }
            return changed > 0;
        }
    });
}
