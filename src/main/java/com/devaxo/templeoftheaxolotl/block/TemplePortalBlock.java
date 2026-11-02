package com.devaxo.templeoftheaxolotl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.maze.OriginTempleTravel;
import com.devaxo.templeoftheaxolotl.maze.TempleTravel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Solid right-click entrance for a future temple dungeon session. */
public class TemplePortalBlock extends Block {
    public static final BooleanProperty ORIGIN = BooleanProperty.create("origin");
    public TemplePortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ORIGIN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIGIN);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                         Player player, net.minecraft.world.InteractionHand hand,
                                         BlockHitResult hitResult) {
        if (stack.is(ModItems.CELESTIAL_KEY.get()) || state.getValue(ORIGIN)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
                OriginTempleTravel.enter(serverPlayer, pos, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (state.getValue(ORIGIN))
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("A Celestial Key is required."), true);
            else TempleTravel.enter(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
