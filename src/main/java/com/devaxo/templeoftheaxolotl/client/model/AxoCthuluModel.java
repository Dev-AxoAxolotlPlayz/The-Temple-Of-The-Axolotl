package com.devaxo.templeoftheaxolotl.client.model;

import com.devaxo.templeoftheaxolotl.entity.AxoCthuluEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AxoCthuluModel extends GeoModel<AxoCthuluEntity> {
    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path);
    }
    @Override public ResourceLocation getModelResource(AxoCthuluEntity entity) { return resource("geo/axocthulu.geo.json"); }
    @Override public ResourceLocation getTextureResource(AxoCthuluEntity entity) { return resource("textures/entity/axocthulu.png"); }
    @Override public ResourceLocation getAnimationResource(AxoCthuluEntity entity) { return resource("animations/axocthulu.animation.json"); }
}
