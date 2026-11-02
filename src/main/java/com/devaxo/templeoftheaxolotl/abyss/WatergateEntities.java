package com.devaxo.templeoftheaxolotl.abyss;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WatergateEntities {
    private WatergateEntities() { }
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WatergateEntity>> WATERGATE = TYPES.register("abyssal_watergate", () ->
            BlockEntityType.Builder.of(WatergateEntity::new, ModBlocks.ABYSSAL_WATERGATE.get()).build(null));
}
