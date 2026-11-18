package com.devaxo.templeoftheaxolotl.item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
public final class AxoliteTridentMode {
    public static boolean isAxoliteFamily(ItemStack stack) { return stack.is(ModItems.AXOLITE_TRIDENT.get()) || stack.is(ModItems.COSMIC_TRIDENT.get()); }
    public static ItemStack held(Player p) {
        if (isAxoliteFamily(p.getMainHandItem())) return p.getMainHandItem();
        if (isAxoliteFamily(p.getOffhandItem())) return p.getOffhandItem();
        return ItemStack.EMPTY;
    }
    public static int state(Player p) {
        var stack = held(p);
        if (stack.isEmpty()) return 0;
        var registry = p.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).getLevel(registry.getHolderOrThrow(Enchantments.RIPTIDE))>0 ? 2 : 1;
    }
    public static boolean toggle(ServerPlayer p) {
        var stack=held(p);
        if(stack.isEmpty() || p.isUsingItem()) return false;
        var registry=p.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        boolean riptide=state(p)==1;
        EnchantmentHelper.updateEnchantments(stack, e -> {
            e.set(registry.getHolderOrThrow(Enchantments.LOYALTY),riptide?0:5);
            e.set(registry.getHolderOrThrow(Enchantments.RIPTIDE),riptide?10:0);
        });
        p.getInventory().setChanged();
        p.displayClientMessage(Component.literal(riptide?"Axolite Trident: Riptide X":"Axolite Trident: Loyalty V + Homing"),true);
        return true;
    }
}
