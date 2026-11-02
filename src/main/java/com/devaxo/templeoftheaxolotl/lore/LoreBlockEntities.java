package com.devaxo.templeoftheaxolotl.lore;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LoreBlockEntities {
    private LoreBlockEntities() {}
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoreBlockEntity>> LORE =
            TYPES.register("lore_block", () -> BlockEntityType.Builder.of(LoreBlockEntity::new,
                    ModBlocks.LORE_BLOCK.get(), ModBlocks.BROKEN_LORE_BLOCK.get()).build(null));
}
