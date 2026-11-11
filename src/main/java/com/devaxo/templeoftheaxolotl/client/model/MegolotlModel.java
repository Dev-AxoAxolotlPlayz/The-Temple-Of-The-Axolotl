package com.devaxo.templeoftheaxolotl.client.model;
import com.devaxo.templeoftheaxolotl.entity.MegolotlEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import net.minecraft.util.Mth;
public class MegolotlModel extends GeoModel<MegolotlEntity> {
 private static ResourceLocation id(String p) {return ResourceLocation.fromNamespaceAndPath("temple_of_the_axolotl",p);}
 @Override public ResourceLocation getModelResource(MegolotlEntity e) {return id("geo/megolotl.geo.json");}
 @Override public ResourceLocation getTextureResource(MegolotlEntity e) {return id("textures/entity/megolotl.png");}
 @Override public ResourceLocation getAnimationResource(MegolotlEntity e) {return id("animations/megolotl.animation.json");}
    // Pitch in model-local space AFTER the renderer applies the entity's yaw.
    // A pre-super.render PoseStack rotation is world-space and gives incorrect tilt.
    @Override
    public void setCustomAnimations(MegolotlEntity entity, long instanceId, AnimationState<MegolotlEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        GeoBone root = getAnimationProcessor().getBone("root");
        if (root != null) {
            float pitch = Mth.lerp(animationState.getPartialTick(), entity.xRotO, entity.getXRot());
            root.setRotX((float) Math.toRadians(pitch));
        }
    }
}
