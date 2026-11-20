package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.AxokrakenEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
public class AxokrakenModel extends GeoModel<AxokrakenEntity> {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl", path); }
    @Override public ResourceLocation getModelResource(AxokrakenEntity entity) { return id("geo/axokraken.geo.json"); }
    @Override public ResourceLocation getTextureResource(AxokrakenEntity entity) { return id("textures/entity/axokraken.png"); }
    @Override public ResourceLocation getAnimationResource(AxokrakenEntity entity) { return id("animations/axokraken.animation.json"); }
    @Override public void setCustomAnimations(AxokrakenEntity entity,long id,software.bernie.geckolib.animation.AnimationState<AxokrakenEntity> state) {
        super.setCustomAnimations(entity,id,state);
        var root=getAnimationProcessor().getBone("root");
        if(root!=null) root.setRotX((float)Math.toRadians(net.minecraft.util.Mth.lerp(state.getPartialTick(),entity.xRotO,entity.getXRot())));
    }
}
