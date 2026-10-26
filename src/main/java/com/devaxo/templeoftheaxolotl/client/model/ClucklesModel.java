package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.ClucklesEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class ClucklesModel extends GeoModel<ClucklesEntity> {
    @Override public ResourceLocation getModelResource(ClucklesEntity e) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "geo/cluckles.geo.json"); }
    @Override public ResourceLocation getTextureResource(ClucklesEntity e) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "textures/entity/cluckles.png"); }
    @Override public ResourceLocation getAnimationResource(ClucklesEntity e) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "animations/cluckles.animation.json"); }
}
