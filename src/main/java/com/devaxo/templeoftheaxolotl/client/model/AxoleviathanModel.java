package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.AxoleviathanEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class AxoleviathanModel extends GeoModel<AxoleviathanEntity>{
 private static ResourceLocation res(String path){return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl",path);}
 @Override public ResourceLocation getModelResource(AxoleviathanEntity e){return res("geo/axoleviathan.geo.json");}
 @Override public ResourceLocation getTextureResource(AxoleviathanEntity e){return res("textures/entity/axoleviathan.png");}
 @Override public ResourceLocation getAnimationResource(AxoleviathanEntity e){return res("animations/axoleviathan.animation.json");}
}
