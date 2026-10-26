package com.devaxo.templeoftheaxolotl.eclipse;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EclipseBlockEntities {
    private EclipseBlockEntities() {}
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TempleOfTheAxolotl.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EclipseAltarEntity>> ALTAR =
            TYPES.register("celestial_altar", () -> BlockEntityType.Builder.of(
                    EclipseAltarEntity::new, ModBlocks.CELESTIAL_ANVIL_BLOCK.get()).build(null));
}
