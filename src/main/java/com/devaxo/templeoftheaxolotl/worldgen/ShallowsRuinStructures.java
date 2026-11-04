package com.devaxo.templeoftheaxolotl.worldgen;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Dedicated NBT-template structure type. No jigsaw or template pool. */
public final class ShallowsRuinStructures {
    private ShallowsRuinStructures() {}

    public static final DeferredRegister<StructureType<?>> STRUCTURES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, TempleOfTheAxolotl.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<ShallowsRuinStructure>> RUIN =
            STRUCTURES.register("shallows_ruin", () -> () -> ShallowsRuinStructure.CODEC);
}
