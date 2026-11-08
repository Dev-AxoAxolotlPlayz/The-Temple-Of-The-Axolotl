package com.devaxo.templeoftheaxolotl.abyss;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;

/** Invisible solid helper occupying only the stone arch's structural blocks. */
public final class WatergateFrameBlock extends Block implements SimpleWaterloggedBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final MapCodec<WatergateFrameBlock> CODEC = simpleCodec(WatergateFrameBlock::new);

    public WatergateFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    /** A Creative hit on any collision segment dismantles its owning Watergate. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild) {
            BlockPos owner = findOwner(level, pos);
            if (owner != null) level.removeBlock(owner, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static BlockPos findOwner(Level level, BlockPos segment) {
        for (int dy = 0; dy <= 4; dy++) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                Direction sideways = facing.getClockWise();
                for (int lateral = -2; lateral <= 2; lateral++) {
                    if (Math.abs(lateral) != 2 && dy != 4) continue;
                    BlockPos origin = segment.below(dy).relative(sideways, -lateral);
                    BlockState candidate = level.getBlockState(origin);
                    if (candidate.is(ModBlocks.ABYSSAL_WATERGATE.get())
                            && candidate.getValue(WatergateBlock.FACING) == facing) return origin;
                }
            }
        }
        return null;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

}
