package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.network.CelestialRadialOpenBookPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;

/** Place the real Book of Axoluthu in a lectern; click to access revival. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AbyssalLecternInteraction {
    private AbyssalLecternInteraction() {}
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        BlockPos pos = event.getPos();
        var level = event.getLevel();
        var state = level.getBlockState(pos);
        if (!state.is(Blocks.LECTERN) || !(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)) return;
        ItemStack held = event.getEntity().getMainHandItem();
        if (!state.getValue(LecternBlock.HAS_BOOK) && held.is(ModItems.NOODLE_COMMAND_BOOK.get())) {
            if (!level.isClientSide && event.getEntity() instanceof ServerPlayer player) {
                ItemStack inserted = held.copy(); inserted.setCount(1);
                lectern.setBook(inserted);
                level.setBlock(pos, state.setValue(LecternBlock.HAS_BOOK, true), 3);
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Book of Axoluthu placed. Click the lectern to open it."));
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (lectern.getBook().is(ModItems.NOODLE_COMMAND_BOOK.get())) {
            if (!level.isClientSide && event.getEntity() instanceof ServerPlayer player)
                PacketDistributor.sendToPlayer(player, new CelestialRadialOpenBookPayload(2));
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
