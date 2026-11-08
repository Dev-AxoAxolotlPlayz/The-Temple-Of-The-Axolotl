package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.MerolotlEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class MerolotlModel extends GeoModel<MerolotlEntity> {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl",path); }
    @Override public ResourceLocation getModelResource(MerolotlEntity e) { return id("geo/merolotl.geo.json"); }
    @Override public ResourceLocation getTextureResource(MerolotlEntity e) { return id("textures/entity/merolotl.png"); }
    @Override public ResourceLocation getAnimationResource(MerolotlEntity e) { return id("animations/merolotl.animation.json"); }
}
