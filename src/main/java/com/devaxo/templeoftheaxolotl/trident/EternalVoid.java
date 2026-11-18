package com.devaxo.templeoftheaxolotl.trident;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public final class EternalVoid {
    private EternalVoid() {}
    public static int level(Level world, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        var key = ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "eternal_void"));
        var holder = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
        return holder == null ? 0 : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(holder);
    }
    public static double radius(int level) {
        return switch (level) { case 1 -> 5; case 2 -> 7; case 3 -> 9; case 4 -> 12; default -> level >= 5 ? 20 : 0; };
    }
}
