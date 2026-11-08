package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.item.StructureGeoBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Reuses the actual placed structure's GeckoLib geometry and texture. */
public final class StructureItemModel extends GeoModel<StructureGeoBlockItem> {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path); }
    @Override public ResourceLocation getModelResource(StructureGeoBlockItem item) { return id("geo/" + item.assetName() + ".geo.json"); }
    @Override public ResourceLocation getTextureResource(StructureGeoBlockItem item) { return id("textures/block/" + item.assetName() + ".png"); }
    @Override public ResourceLocation getAnimationResource(StructureGeoBlockItem item) { return id("animations/" + item.assetName() + ".animation.json"); }
}
