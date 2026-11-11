package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.AxobloopEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import net.minecraft.util.Mth;
public class AxobloopModel extends GeoModel<AxobloopEntity> {
    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl",p); }
    @Override public ResourceLocation getModelResource(AxobloopEntity e) { return id("geo/axobloop.geo.json"); }
    @Override public ResourceLocation getTextureResource(AxobloopEntity e) { return id("textures/entity/axobloop.png"); }
    @Override public ResourceLocation getAnimationResource(AxobloopEntity e) { return id("animations/axobloop.animation.json"); }
    // Pitch in model-local space AFTER the renderer applies the entity's yaw.
    // A pre-super.render PoseStack rotation is world-space and gives incorrect tilt.
    @Override
    public void setCustomAnimations(AxobloopEntity entity, long instanceId, AnimationState<AxobloopEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        GeoBone root = getAnimationProcessor().getBone("root");
        if (root != null) {
            float pitch = Mth.lerp(animationState.getPartialTick(), entity.xRotO, entity.getXRot());
            root.setRotX((float) Math.toRadians(pitch));
        }
    }
}
