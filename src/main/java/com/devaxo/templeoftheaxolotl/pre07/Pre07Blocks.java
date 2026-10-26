package com.devaxo.templeoftheaxolotl.pre07;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;
public final class Pre07Blocks {
 public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TempleOfTheAxolotl.MODID);
 public static final DeferredBlock<Block> CELESTIAL_CACHE = BLOCKS.register("celestial_cache", () -> new CelestialCacheBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENDER_CHEST)));
 public static final DeferredBlock<Block> TRIAL_TRAP_SPAWNER = BLOCKS.register("trial_trap_spawner", () -> new TrialTrapSpawnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
 private Pre07Blocks() {}
}
