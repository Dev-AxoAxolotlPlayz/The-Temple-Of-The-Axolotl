package com.devaxo.templeoftheaxolotl.traps;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Full stone-brick collision; top-only slowdown, no sticky wall sliding. */
public class HoneybrickTrapBlock extends Block {
    public HoneybrickTrapBlock(BlockBehaviour.Properties properties) { super(properties.speedFactor(0.4F).jumpFactor(0.5F)); }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
    }
}
