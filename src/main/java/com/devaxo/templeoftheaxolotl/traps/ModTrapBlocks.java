package com.devaxo.templeoftheaxolotl.traps;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTrapBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("temple_of_the_axolotl");
    public static final DeferredBlock<Block> RNGESUS_TRAP = BLOCKS.register("rngesus_trap",
            () -> new RngesusTrapBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<Block> SPIKE_TRAP = BLOCKS.register("spike_trap",
            () -> new CoralSpikeTrapBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<Block> HONEYBRICK_TRAP = BLOCKS.register("honeybrick_trap",
            () -> new HoneybrickTrapBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<Block> SNEAK_TRAP = BLOCKS.register("sneak_trap",
            () -> new SneakTrapBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
    private ModTrapBlocks() {}
}
