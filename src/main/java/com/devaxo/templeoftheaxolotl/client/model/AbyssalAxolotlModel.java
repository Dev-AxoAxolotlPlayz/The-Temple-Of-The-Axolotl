package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.AbyssalAxolotlEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class AbyssalAxolotlModel extends GeoModel<AbyssalAxolotlEntity> {
    private static final String MOD = "temple_of_the_axolotl";
    @Override public ResourceLocation getModelResource(AbyssalAxolotlEntity e) { return ResourceLocation.fromNamespaceAndPath(MOD, "geo/abyssal_axolotl.geo.json"); }
    @Override public ResourceLocation getTextureResource(AbyssalAxolotlEntity e) { return ResourceLocation.fromNamespaceAndPath(MOD, "textures/entity/abyssal_axolotl.png"); }
    @Override public ResourceLocation getAnimationResource(AbyssalAxolotlEntity e) { return ResourceLocation.fromNamespaceAndPath(MOD, "animations/abyssal_axolotl.animation.json"); }
}
