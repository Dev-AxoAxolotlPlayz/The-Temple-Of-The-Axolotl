package com.devaxo.templeoftheaxolotl.registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
public final class CreativeBookFilter {
    private static boolean extended(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return false;
        for (var entry : stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet())
            if (entry.getIntValue() > entry.getKey().value().getMaxLevel()) return true;
        return false;
    }
    public static void filter(BuildCreativeModeTabContentsEvent event) {
        for (var stack : java.util.List.copyOf(event.getParentEntries()))
            if (extended(stack)) event.remove(stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        for (var stack : java.util.List.copyOf(event.getSearchEntries()))
            if (extended(stack)) event.remove(stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }
}
