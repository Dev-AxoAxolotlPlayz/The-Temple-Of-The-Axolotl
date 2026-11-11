package com.devaxo.templeoftheaxolotl.block;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AbyssalChestEntities {
    private AbyssalChestEntities() {}
    public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AbyssalChestEntity>> CHEST=TYPES.register("abyssal_chest",()->BlockEntityType.Builder.of(AbyssalChestEntity::new,ModBlocks.ABYSSAL_CHEST.get()).build(null));
}
