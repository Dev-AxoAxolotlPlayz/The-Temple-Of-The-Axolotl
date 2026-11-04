
package com.devaxo.templeoftheaxolotl.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Places the existing Shallows ruin NBT at the Overworld surface. */
public final class ShallowsRuinStructure extends Structure {
    public static final MapCodec<ShallowsRuinStructure> CODEC =
            simpleCodec(ShallowsRuinStructure::new);

    private static final ResourceLocation TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(
                    "temple_of_the_axolotl",
                    "shallows_overworld_ruin"
            );

    public ShallowsRuinStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(
            GenerationContext context
    ) {
        int x = context.chunkPos().getMinBlockX();
        int z = context.chunkPos().getMinBlockZ();

        int surfaceY = context.chunkGenerator().getFirstFreeHeight(
                x,
                z,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );

        BlockPos origin = new BlockPos(x, surfaceY, z);

        return Optional.of(new GenerationStub(origin, builder -> {
            builder.addPiece(new ShallowsRuinPiece(
                    context.structureTemplateManager(),
                    TEMPLATE,
                    origin
            ));
        }));
    }

    @Override
    public StructureType<?> type() {
        return ShallowsRuinStructures.RUIN.get();
    }
}
