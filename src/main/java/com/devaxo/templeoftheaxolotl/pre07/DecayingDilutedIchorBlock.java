package com.devaxo.templeoftheaxolotl.pre07;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Every newly created Diluted Ichor fluid block expires after 300 loaded game ticks. */
public final class DecayingDilutedIchorBlock extends LiquidBlock {
    public static final int DECAY_TICKS = 300;

    public DecayingDilutedIchorBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        // The source and each new flowing position are both this block type.
        // Schedule once when a position first becomes Diluted Ichor; changes in
        // fluid level must not reset the original expiration deadline.
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            server.scheduleTick(pos, this, DECAY_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // This is a BLOCK tick, independent of ordinary fluid-flow ticks.
        if (state.is(this)) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    public ItemStack pickupBlock(Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
