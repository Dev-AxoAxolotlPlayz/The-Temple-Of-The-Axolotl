package com.devaxo.templeoftheaxolotl.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Retains original Axolite speed and damage, upgrading durability and harvesting. */
public enum AxolitePickaxeTier implements Tier {
    SECOND(false), THIRD(true);
    private final boolean diamondHarvest;
    AxolitePickaxeTier(boolean diamondHarvest) { this.diamondHarvest = diamondHarvest; }
    @Override public int getUses() { return 2031; }
    @Override public float getSpeed() { return 9.0F; }
    @Override public float getAttackDamageBonus() { return 2.0F; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() {
        return diamondHarvest ? BlockTags.INCORRECT_FOR_DIAMOND_TOOL : BlockTags.INCORRECT_FOR_IRON_TOOL;
    }
    @Override public int getEnchantmentValue() { return 10; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.AXOLITE_INGOT.get()); }
}
