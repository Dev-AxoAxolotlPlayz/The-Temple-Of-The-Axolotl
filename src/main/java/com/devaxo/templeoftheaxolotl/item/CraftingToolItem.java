
package com.devaxo.templeoftheaxolotl.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CraftingToolItem extends Item {

    private final int craftingDamage;

    public CraftingToolItem(Properties properties, int craftingDamage) {
        super(properties);
        this.craftingDamage = craftingDamage;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remainder = stack.copy();
        remainder.setCount(1);

        if (remainder.isDamageableItem() && craftingDamage > 0) {
            int newDamage = remainder.getDamageValue() + craftingDamage;

            if (newDamage >= remainder.getMaxDamage()) {
                return ItemStack.EMPTY;
            }

            remainder.setDamageValue(newDamage);
        }

        return remainder;
    }
}
