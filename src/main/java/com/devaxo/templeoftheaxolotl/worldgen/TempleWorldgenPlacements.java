package com.devaxo.templeoftheaxolotl.worldgen;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TempleWorldgenPlacements {
    private TempleWorldgenPlacements() {}

    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, TempleOfTheAxolotl.MODID);

    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<TempleSpawnExcludedPlacement>>
            SPAWN_EXCLUDED = PLACEMENTS.register("spawn_excluded", () -> () -> TempleSpawnExcludedPlacement.CODEC);
}
