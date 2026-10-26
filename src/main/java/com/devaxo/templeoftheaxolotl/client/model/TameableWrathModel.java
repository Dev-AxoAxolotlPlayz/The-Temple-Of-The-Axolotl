package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.TameableWrathEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class TameableWrathModel extends GeoModel<TameableWrathEntity> {
    @Override public ResourceLocation getModelResource(TameableWrathEntity e) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "geo/axolotl_wrath_tameable.geo.json"); }
    @Override public ResourceLocation getTextureResource(TameableWrathEntity e) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "textures/entity/axolotl_wrath_tameable.png"); }
    @Override public ResourceLocation getAnimationResource(TameableWrathEntity e) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", "animations/axolotl_wrath_tame.animation.json"); }
}
