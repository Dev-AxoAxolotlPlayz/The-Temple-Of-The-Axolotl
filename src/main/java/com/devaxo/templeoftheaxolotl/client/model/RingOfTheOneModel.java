package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.item.RingOfTheOneItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Original Blockbench model and animation files, unchanged. */
public final class RingOfTheOneModel extends GeoModel<RingOfTheOneItem> {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path);
    }
    @Override public ResourceLocation getModelResource(RingOfTheOneItem item) {
        return id("geo/the_ring_of_the_one.geo.json");
    }
    @Override public ResourceLocation getTextureResource(RingOfTheOneItem item) {
        return id("textures/item/the_ring_of_the_one.png");
    }
    @Override public ResourceLocation getAnimationResource(RingOfTheOneItem item) {
        return id("animations/the_ring_of_the_one.animation.json");
    }
}
