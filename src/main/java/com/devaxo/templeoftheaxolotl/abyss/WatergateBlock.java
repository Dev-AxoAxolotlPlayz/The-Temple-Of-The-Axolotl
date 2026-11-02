
package com.devaxo.templeoftheaxolotl.abyss;

import com.mojang.serialization.MapCodec;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Trial Watergate controller.
 * The exported 5-block visual mesh is retained as an asset
 * for the future GeckoLib rendering pass.
 */
public class WatergateBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<WatergateBlock> CODEC =
            simpleCodec(WatergateBlock::new);

    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    public WatergateBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH)
        );
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext ctx) { return Block.box(0, 0, 0, 16, 16, 16); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }
    @Nullable @Override public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) { return new WatergateEntity(pos, state); }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    /** Two 1-block-wide pillars at +/-2, and a top lintel at Y+4.
     * The central 3x4 portal remains open and passable.
     * Only replace empty/replaceable positions; never destroy player structures.
     */
    private static void updateFrame(Level level, BlockPos origin, Direction facing, boolean place) {
        Direction sideways = facing.getClockWise();
        for (int lateral = -2; lateral <= 2; lateral++) {
            for (int height = 0; height <= 4; height++) {
                if (Math.abs(lateral) != 2 && height != 4) continue;
                BlockPos target = origin.relative(sideways, lateral).above(height);
                if (target.equals(origin)) continue;
                BlockState current = level.getBlockState(target);
                if (place) {
                    if (current.isAir() || current.canBeReplaced()) {
                        level.setBlock(target, ModBlocks.ABYSSAL_WATERGATE_FRAME.get().defaultBlockState(), 3);
                    }
                } else if (current.is(ModBlocks.ABYSSAL_WATERGATE_FRAME.get())) {
                    level.removeBlock(target, false);
                }
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            updateFrame(level, pos, state.getValue(FACING), true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this)) {
            updateFrame(level, pos, state.getValue(FACING), false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }
}
