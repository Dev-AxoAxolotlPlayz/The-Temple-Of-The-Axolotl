package com.devaxo.templeoftheaxolotl.shipyard;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class ShipyardCraneModel extends GeoModel<ShipyardCraneBlockEntity>{
 private static ResourceLocation path(String p){return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl",p);}
 @Override public ResourceLocation getModelResource(ShipyardCraneBlockEntity e){return path("geo/shipyard_crane.geo.json");}
 @Override public ResourceLocation getTextureResource(ShipyardCraneBlockEntity e){return path("textures/block/shipyard_crane.png");}
 @Override public ResourceLocation getAnimationResource(ShipyardCraneBlockEntity e){return path("animations/shipyard_crane.animation.json");}
}
