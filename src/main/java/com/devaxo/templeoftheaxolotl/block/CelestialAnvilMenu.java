package com.devaxo.templeoftheaxolotl.block;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Celestial-only anvil rules; normal anvils remain unchanged. */
public class CelestialAnvilMenu extends AnvilMenu {
    public CelestialAnvilMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(id, inventory, access);
    }

    @Override
    public void createResult() {
        // Vanilla only discards results costing >= 40 when the player is not creative.
        // Temporarily bypass that *calculation-only* check; always restore abilities.
        boolean originalInstabuild = this.player.getAbilities().instabuild;
        try {
            this.player.getAbilities().instabuild = true;
            super.createResult();
        } finally {
            this.player.getAbilities().instabuild = originalInstabuild;
        }

        ItemStack left = inputSlots.getItem(0);
        ItemStack right = inputSlots.getItem(1);
        if (left.isEmpty()) return;

        ItemStack vanillaResult = resultSlots.getItem(0);
        int vanillaCost = this.getCost();
        if (right.isEmpty()) {
            if (!vanillaResult.isEmpty()) discount(vanillaCost);
            return;
        }

        ItemEnchantments rightEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(right);
        if (rightEnchantments.isEmpty()) {
            if (!vanillaResult.isEmpty()) discount(vanillaCost);
            return;
        }

        ItemStack output = vanillaResult.isEmpty() ? left.copy() : vanillaResult.copy();
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(
                EnchantmentHelper.getEnchantmentsForCrafting(output));
        int addedCost = 0;
        boolean changed = false;

        for (Holder<Enchantment> enchantment : rightEnchantments.keySet()) {
            int existing = merged.getLevel(enchantment);
            int incoming = rightEnchantments.getLevel(enchantment);
            int max = enchantment.value().getMaxLevel() + 1;
            int next = Math.min(max, existing == incoming ? incoming + 1 : Math.max(existing, incoming));
            if (next <= existing) continue;

            boolean fortune = enchantment.is(Enchantments.FORTUNE);
            boolean silk = enchantment.is(Enchantments.SILK_TOUCH);
            boolean hasOpposite = merged.keySet().stream().anyMatch(h ->
                    (fortune && h.is(Enchantments.SILK_TOUCH)) ||
                            (silk && h.is(Enchantments.FORTUNE)));
            if (hasOpposite) continue;

            merged.set(enchantment, next);
            addedCost += Math.max(1, next - existing);
            changed = true;
        }

        if (!changed) {
            if (!vanillaResult.isEmpty()) discount(vanillaCost);
            return;
        }

        EnchantmentHelper.setEnchantments(output, merged.toImmutable());
        resultSlots.setItem(0, output);
        int fullCost = vanillaResult.isEmpty()
                ? Math.max(1, addedCost)
                : Math.max(vanillaCost, addedCost);
        discount(fullCost);
        broadcastChanges();
    }

    private void discount(int fullCost) {
        this.setMaximumCost(Math.max(1L, ((long) fullCost + 1L) / 2L));
        broadcastChanges();
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        return hasStack && (player.getAbilities().instabuild
                || player.experienceLevel >= this.getCost());
    }
}
