package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.lore.LoreBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class LoreBlockItemModel extends GeoModel<LoreBlockItem> {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path); }
    @Override public ResourceLocation getModelResource(LoreBlockItem item) {
        return id(item.isBroken() ? "geo/broken_lore_block.geo.json" : "geo/lore_block.geo.json");
    }
    @Override public ResourceLocation getTextureResource(LoreBlockItem item) {
        return id(item.isBroken() ? "textures/block/faded_lore_block.png" : "textures/block/lore_block.png");
    }
    @Override public ResourceLocation getAnimationResource(LoreBlockItem item) {
        return id("animations/lore_block.animation.json");
    }
}
