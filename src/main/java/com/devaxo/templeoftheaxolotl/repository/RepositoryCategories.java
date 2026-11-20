package com.devaxo.templeoftheaxolotl.repository;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;

/** Server-side view classification. All unrecognized items remain accessible in Other. */
public final class RepositoryCategories {
    private RepositoryCategories() {}
    public static boolean matches(ItemStack stack, int filter) {
        if (filter == 0) return true;
        boolean ore = ore(stack);
        boolean equipment = equipment(stack);
        boolean drop = mobDrop(stack);
        return switch (filter) {
            case 1 -> ore;
            case 2 -> equipment;
            case 3 -> drop;
            case 4 -> !ore && !equipment && !drop;
            default -> true;
        };
    }
    private static boolean ore(ItemStack s) {
        if (s.is(ItemTags.COALS) || s.is(ItemTags.IRON_ORES) || s.is(ItemTags.GOLD_ORES)
                || s.is(ItemTags.DIAMOND_ORES) || s.is(ItemTags.EMERALD_ORES)
                || s.is(ItemTags.COPPER_ORES) || s.is(ItemTags.REDSTONE_ORES)
                || s.is(ItemTags.LAPIS_ORES)) return true;
        Item item = s.getItem();
        return item == Items.IRON_INGOT || item == Items.GOLD_INGOT || item == Items.COPPER_INGOT
            || item == Items.NETHERITE_INGOT || item == Items.IRON_NUGGET || item == Items.GOLD_NUGGET
            || item == Items.DIAMOND || item == Items.EMERALD || item == Items.LAPIS_LAZULI
            || item == Items.REDSTONE || item == Items.QUARTZ || item == Items.AMETHYST_SHARD
            || item == Items.RAW_IRON || item == Items.RAW_GOLD || item == Items.RAW_COPPER
            || item == Items.COAL || item == Items.CHARCOAL || item == Items.ANCIENT_DEBRIS;
    }
    private static boolean equipment(ItemStack s) {
        Item item = s.getItem();
        return item instanceof TieredItem || item instanceof ArmorItem || item instanceof BowItem
            || item instanceof CrossbowItem || item instanceof TridentItem || item instanceof ShieldItem
            || item instanceof ElytraItem || item instanceof FishingRodItem || item instanceof ShearsItem;
    }
    private static boolean mobDrop(ItemStack s) {
        Item i = s.getItem();
        return i == Items.BONE || i == Items.BONE_MEAL || i == Items.STRING || i == Items.ROTTEN_FLESH
            || i == Items.SPIDER_EYE || i == Items.FERMENTED_SPIDER_EYE || i == Items.ENDER_PEARL
            || i == Items.BLAZE_ROD || i == Items.BLAZE_POWDER || i == Items.GHAST_TEAR
            || i == Items.GUNPOWDER || i == Items.SLIME_BALL || i == Items.MAGMA_CREAM
            || i == Items.PHANTOM_MEMBRANE || i == Items.PRISMARINE_SHARD || i == Items.PRISMARINE_CRYSTALS
            || i == Items.NAUTILUS_SHELL || i == Items.LEATHER || i == Items.RABBIT_HIDE
            || i == Items.FEATHER || i == Items.INK_SAC || i == Items.GLOW_INK_SAC
            || i == Items.SHULKER_SHELL || i == Items.WITHER_SKELETON_SKULL;
    }
}
