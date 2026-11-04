package com.devaxo.templeoftheaxolotl.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Concrete piece for the existing Shallows ruin NBT; no jigsaw logic. */
public final class ShallowsRuinPiece extends TemplateStructurePiece {
    private static StructurePlaceSettings settings() {
        return new StructurePlaceSettings().setRotation(Rotation.NONE);
    }

    public ShallowsRuinPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos pos) {
        super(ShallowsRuinPieceTypes.RUIN.get(), 0, manager, template, template.toString(), settings(), pos);
    }

    public ShallowsRuinPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ShallowsRuinPieceTypes.RUIN.get(), tag, manager, ignored -> settings());
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        // The ruin NBT uses real portal blocks, not structure data markers.
    }
}
