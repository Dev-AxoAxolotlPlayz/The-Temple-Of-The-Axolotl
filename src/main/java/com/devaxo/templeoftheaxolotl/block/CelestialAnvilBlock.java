package com.devaxo.templeoftheaxolotl.block;

import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A vanilla-style anvil whose menu remains valid for our custom block ID. */
public class CelestialAnvilBlock extends AnvilBlock {
    public CelestialAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider((containerId, inventory, menuPlayer) ->
                    new CelestialAnvilMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)) {
                        @Override
                        public boolean stillValid(Player checkingPlayer) {
                            return level.getBlockState(pos).is(ModBlocks.CELESTIAL_ANVIL_BLOCK.get())
                                    && checkingPlayer.distanceToSqr(pos.getX() + 0.5,
                                            pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
                        }
                    }, Component.translatable("container.repair")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
