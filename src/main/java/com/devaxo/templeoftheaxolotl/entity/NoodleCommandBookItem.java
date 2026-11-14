package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Opens the illustrated command reference; player-created entries are synced through book data. */
public class NoodleCommandBookItem extends Item {
    public NoodleCommandBookItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            if (stack.is(com.devaxo.templeoftheaxolotl.item.ModItems.ABYSSAL_COMPANION_REGISTRY.get())) {
                com.devaxo.templeoftheaxolotl.client.AbyssalCommandBookScreen.openRegistry(stack);
            } else {
                // Legacy Book of Axoluthu: original pirate-parchment Field Guide + Journal tab.
                com.devaxo.templeoftheaxolotl.client.AbyssalCommandBookScreen.open(stack);
            }
        } else if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            NoodleTraining.syncBooks(server);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
