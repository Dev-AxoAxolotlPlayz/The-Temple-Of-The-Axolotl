package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Construction materials may only originate from the three approved game namespaces. */
public final class WaifuAxoluthuMaterialPolicy {
    private WaifuAxoluthuMaterialPolicy() {}
    public static boolean allowedNamespace(String namespace) {
        return "minecraft".equals(namespace)
                || "templeoftheaxolotl".equals(namespace);
    }
    public static boolean allowed(Item item) {
        return allowedNamespace(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }
    public static boolean allowed(Block block) {
        return allowedNamespace(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }
    /** Migrate legacy modded windows without altering the approved plot coordinates. */
    public static String normalizeBlueprintBlock(String id) {
        if (id == null || id.isEmpty()) return id;
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        if ("minecraft:glass_pane".equals(id) || "glass_pane".equals(id))
            return "minecraft:glass";
        if (allowedNamespace(namespace)) return id;
        String path = colon < 0 ? id : id.substring(colon + 1);
        if (path.contains("glass") || path.contains("window")) return "minecraft:glass";
        return id; // Unrecognized blocks must fail validation, not silently change.
    }
}
