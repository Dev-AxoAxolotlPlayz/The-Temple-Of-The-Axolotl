package com.devaxo.templeoftheaxolotl.worldgen;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ShallowsRuinPieceTypes {
    private ShallowsRuinPieceTypes() {}

    public static final DeferredRegister<StructurePieceType> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, TempleOfTheAxolotl.MODID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> RUIN =
            TYPES.register("shallows_ruin_piece",
                    () -> (context, tag) ->
                            new ShallowsRuinPiece(context.structureTemplateManager(), tag));
}
