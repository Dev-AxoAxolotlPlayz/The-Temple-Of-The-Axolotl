package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.AbyssalJellyfishEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import net.minecraft.util.Mth;
public class AbyssalJellyfishModel extends GeoModel<AbyssalJellyfishEntity> {
 private static ResourceLocation id(String p) {return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl",p);}
 @Override public ResourceLocation getModelResource(AbyssalJellyfishEntity e) {return id("geo/abyssal_jellyfish.geo.json");}
 @Override public ResourceLocation getTextureResource(AbyssalJellyfishEntity e) {return id("textures/entity/abyssal_jellyfish.png");}
 @Override public ResourceLocation getAnimationResource(AbyssalJellyfishEntity e) {return id("animations/abyssal_jellyfish.animation.json");}
    // Pitch in model-local space AFTER the renderer applies the entity's yaw.
    // A pre-super.render PoseStack rotation is world-space and gives incorrect tilt.
    @Override
    public void setCustomAnimations(AbyssalJellyfishEntity entity, long instanceId, AnimationState<AbyssalJellyfishEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        GeoBone root = getAnimationProcessor().getBone("root");
        if (root != null) {
            float pitch = Mth.lerp(animationState.getPartialTick(), entity.xRotO, entity.getXRot());
            root.setRotX((float) Math.toRadians(pitch));
        }
    }
}
