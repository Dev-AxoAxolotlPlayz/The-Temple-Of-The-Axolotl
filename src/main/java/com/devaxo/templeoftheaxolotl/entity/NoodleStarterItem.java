package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

/** One starter bundle per world/player, granted alongside the Field Guide. */
public class NoodleStarterItem extends Item {
    public NoodleStarterItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(held);
        if (player instanceof ServerPlayer owner) {
            give(owner, new ItemStack(ModItems.ABYSSAL_CHARM.get()));
            give(owner, new ItemStack(ModItems.ABYSSAL_TORCH.get()));
            ItemStack book = new ItemStack(ModItems.NOODLE_COMMAND_BOOK.get());
            NoodleTraining.setCommands(book, NoodleCommandLibrary.load(owner.getUUID()));
            give(owner, book);
            give(owner, new ItemStack(ModItems.NOODLE_TRAINING_MEDAL.get()));
            if (!owner.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResultHolder.consume(held);
    }
    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
