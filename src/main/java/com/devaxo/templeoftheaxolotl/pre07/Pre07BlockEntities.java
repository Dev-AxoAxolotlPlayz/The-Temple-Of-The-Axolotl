package com.devaxo.templeoftheaxolotl.pre07;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.registries.*;
public final class Pre07BlockEntities {
 public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,TempleOfTheAxolotl.MODID);
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CelestialCacheEntity>> CACHE=TYPES.register("celestial_cache",()->BlockEntityType.Builder.of(CelestialCacheEntity::new,Pre07Blocks.CELESTIAL_CACHE.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TrialTrapSpawnerEntity>> TRAP=TYPES.register("trial_trap_spawner",()->BlockEntityType.Builder.of(TrialTrapSpawnerEntity::new,Pre07Blocks.TRIAL_TRAP_SPAWNER.get()).build(null));
}
