package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.brewing.CosmicBrewingEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class CosmicBrewingModel extends GeoModel<CosmicBrewingEntity> {
 private static final String NS="temple_of_the_axolotl";
 @Override public ResourceLocation getModelResource(CosmicBrewingEntity e){return ResourceLocation.fromNamespaceAndPath(NS,"geo/cosmic_brewing_stand.geo.json");}
 @Override public ResourceLocation getTextureResource(CosmicBrewingEntity e){return ResourceLocation.fromNamespaceAndPath(NS,"textures/block/cosmic_brewing_64.png");}
 @Override public ResourceLocation getAnimationResource(CosmicBrewingEntity e){return ResourceLocation.fromNamespaceAndPath(NS,"animations/cosmic_brewing_stand.animation.json");}
}
