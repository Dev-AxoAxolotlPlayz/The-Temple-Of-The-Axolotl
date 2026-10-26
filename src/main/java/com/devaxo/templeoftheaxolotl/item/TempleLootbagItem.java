package com.devaxo.templeoftheaxolotl.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import java.util.ArrayList;
import java.util.List;

/** Self-consuming reward parcel. CustomData stores complete ItemStacks without vanilla bundle weight limits. */
public final class TempleLootbagItem extends Item {
    private static final String CONTENTS = "tota_lootbag_contents_v1";
    public TempleLootbagItem(Properties properties) { super(properties.stacksTo(1)); }

    public static ItemStack pack(ServerPlayer player, List<ItemStack> rewards) {
        ItemStack bag = new ItemStack(ModItems.TEMPLE_LOOTBAG.get());
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack reward : rewards) {
            if (reward.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.put("stack", reward.save(player.registryAccess()));
            list.add(entry);
        }
        data.put(CONTENTS, list);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return bag;
    }

    private static List<ItemStack> contents(ServerPlayer player, ItemStack bag) {
        List<ItemStack> result = new ArrayList<>();
        CustomData component = bag.get(DataComponents.CUSTOM_DATA);
        if (component == null) return result;
        ListTag list = component.copyTag().getList(CONTENTS, Tag.TAG_COMPOUND);
        for (int i=0;i<list.size();i++) {
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), list.getCompound(i).getCompound("stack"));
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack bag = user.getItemInHand(hand);
        if (level.isClientSide || !(user instanceof ServerPlayer player))
            return InteractionResultHolder.sidedSuccess(bag, level.isClientSide);
        List<ItemStack> remaining = new ArrayList<>();
        int claimed = 0;
        for (ItemStack original : contents(player, bag)) {
            ItemStack copy = original.copy();
            player.getInventory().add(copy);
            if (copy.getCount() < original.getCount()) claimed += original.getCount() - copy.getCount();
            if (!copy.isEmpty()) remaining.add(copy);
        }
        if (remaining.isEmpty()) {
            bag.shrink(1); // no empty lootbags left behind
            player.displayClientMessage(Component.literal("Temple Lootbag emptied and vanished!"), true);
        } else {
            ItemStack updated = pack(player, remaining);
            bag.set(DataComponents.CUSTOM_DATA, updated.get(DataComponents.CUSTOM_DATA));
            player.displayClientMessage(Component.literal("Claimed " + claimed + " items. Free inventory space to claim the rest."), true);
        }
        player.getInventory().setChanged();
        return InteractionResultHolder.sidedSuccess(bag, false);
    }
}
