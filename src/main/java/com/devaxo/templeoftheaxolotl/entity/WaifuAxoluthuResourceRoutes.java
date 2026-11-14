package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Stage 3A planning registry only: no virtual drops or world modifications. */
public final class WaifuAxoluthuResourceRoutes {
    private WaifuAxoluthuResourceRoutes() {}
    public static String route(Item item) {
        if(item==Items.OAK_LOG || item==Items.SPRUCE_LOG || item==Items.BIRCH_LOG
                || item==Items.JUNGLE_LOG || item==Items.ACACIA_LOG || item==Items.DARK_OAK_LOG
                || item==Items.MANGROVE_LOG || item==Items.CHERRY_LOG)
            return "NATURAL_TREE -> CHAIN_HARVEST -> COLLECT_DROPS -> REPLANT_ALL";
        if(item==Items.COBBLESTONE || item==Items.STONE || item==Items.COBBLED_DEEPSLATE
                || item==Items.IRON_ORE || item==Items.COAL || item==Items.RAW_IRON)
            return "GRINDSTONE -> MINING_SIMULATION -> ONE_ITEM_PER_CYCLE";
        if(item==Items.BONE || item==Items.STRING || item==Items.ROTTEN_FLESH
                || item==Items.GUNPOWDER || item==Items.ENDER_PEARL ||
                item==Items.BLAZE_ROD || item==Items.GHAST_TEAR ||
                item==Items.MAGMA_CREAM || item==Items.WITHER_SKELETON_SKULL ||
                item==Items.SLIME_BALL || item==Items.SPIDER_EYE ||
                item==Items.PHANTOM_MEMBRANE)
            return "COMPOSTER -> SIMULATED_MOB_DROP -> ONE_ITEM_PER_CYCLE";
        if(item==Items.SUGAR_CANE || item==Items.WHEAT || item==Items.POPPY
                || item==Items.DANDELION || item==Items.CORNFLOWER)
            return "COMPOSTER -> FARM_FORAGE_SIMULATION -> ONE_ITEM_PER_CYCLE";
        if(item==Items.SAND || item==Items.RED_SAND || item==Items.TERRACOTTA ||
                item==Items.CLAY_BALL || item==Items.NETHERRACK ||
                item==Items.BLACKSTONE || item==Items.BASALT || item==Items.END_STONE ||
                item==Items.SOUL_SAND || item==Items.NETHER_BRICKS ||
                item==Items.CRIMSON_STEM || item==Items.WARPED_STEM)
            return "GRINDSTONE -> EXOTIC_MINING_SIMULATION -> ONE_ITEM_PER_CYCLE";
        return "NO_ROUTE_REGISTERED";
    }
}
