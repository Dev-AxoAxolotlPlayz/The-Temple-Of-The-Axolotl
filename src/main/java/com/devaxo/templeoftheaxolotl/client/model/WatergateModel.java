package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.abyss.WatergateEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class WatergateModel extends GeoModel<WatergateEntity> {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path); }
    @Override public ResourceLocation getModelResource(WatergateEntity entity) { return id("geo/abyssal_watergate.geo.json"); }
    @Override public ResourceLocation getTextureResource(WatergateEntity entity) { return id("textures/block/abyssal_watergate.png"); }
    @Override public ResourceLocation getAnimationResource(WatergateEntity entity) { return id("animations/abyssal_watergate.animation.json"); }
}
