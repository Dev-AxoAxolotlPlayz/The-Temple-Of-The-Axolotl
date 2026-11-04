package com.devaxo.templeoftheaxolotl.block;

import com.devaxo.templeoftheaxolotl.maze.ShallowsExpedition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Same right-click activation pattern as the regular Temple portal. */
public final class ShallowsPortalBlock extends Block {
    private final boolean exit;
    public ShallowsPortalBlock(BlockBehaviour.Properties properties, boolean exit) {
        super(properties); this.exit = exit;
    }
    private void activate(Level level, Player player, BlockPos pos) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (exit) ShallowsExpedition.exit(sp);
            else ShallowsExpedition.enter(sp, pos);
        }
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        activate(level, player, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        activate(level, player, pos);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
