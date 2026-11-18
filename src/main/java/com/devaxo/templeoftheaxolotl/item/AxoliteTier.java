package com.devaxo.templeoftheaxolotl.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;

/** Iron harvest restriction, Netherite mining speed, Diamond durability. */
public enum AxoliteTier implements Tier {
    INSTANCE;
    @Override public int getUses() { return 1561; }
    @Override public float getSpeed() { return 9.0F; }
    @Override public float getAttackDamageBonus() { return 2.0F; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
    @Override public int getEnchantmentValue() { return 10; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.AXOLITE_INGOT.get()); }
}
