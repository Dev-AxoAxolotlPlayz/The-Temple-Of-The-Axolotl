package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A reusable view of the player's bonded Axoluthu project, not a snapshot book. */
public final class AxoluthuArchitectJournalItem extends Item {
    public AxoluthuArchitectJournalItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server)
            com.devaxo.templeoftheaxolotl.network.AxoluthuJournalPayload.send(server);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
