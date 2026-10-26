package com.devaxo.templeoftheaxolotl.eclipse;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EclipseAltarBlock extends AnvilBlock implements EntityBlock {
    public EclipseAltarBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EclipseAltarEntity(pos, state);
    }
    public static void open(ServerPlayer player, BlockPos pos, boolean inscription) {
        if (!(player.level().getBlockEntity(pos) instanceof EclipseAltarEntity)) return;
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.literal(inscription ? "Celestial Inscription" : "Celestial Transmutation");
            }
            @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new EclipseMenu(id, inv, pos, inscription);
            }
        }, buf -> buf.writeBlockPos(pos));
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                           Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server) open(server, pos, false);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

}
