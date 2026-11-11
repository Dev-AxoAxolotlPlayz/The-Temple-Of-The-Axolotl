package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.entity.AbyssalAxolotlEntity;
import com.devaxo.templeoftheaxolotl.client.model.AbyssalAxolotlModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class AbyssalAxolotlRenderer extends GeoEntityRenderer<AbyssalAxolotlEntity> {
    public AbyssalAxolotlRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalAxolotlModel());
    }
    @Override
    public void preRender(PoseStack poseStack, AbyssalAxolotlEntity animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        // GeoEntityRenderer applies yaw but not the entity's X rotation to the full model.
        // Rotate around the model's mid-body, not its feet, to make dives look natural.
        if (animatable.isInWater()) {
            float pitch = net.minecraft.util.Mth.rotLerp(partialTick,
                    animatable.xRotO, animatable.getXRot());
            poseStack.translate(0.0D, animatable.getBbHeight() * 0.5D, 0.0D);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
            poseStack.translate(0.0D, -animatable.getBbHeight() * 0.5D, 0.0D);
        }
        poseStack.scale(0.25f, 0.25f, 0.25f);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
    @Override
    public void render(AbyssalAxolotlEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }
    // Yaw-independent bounds cover the scaled model even when its center is offscreen.
    @Override
    public boolean shouldRender(AbyssalAxolotlEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        double horizontal = Math.max(2.0, entity.getBbWidth() * 1.0 * 1.5);
        double vertical = Math.max(2.0, entity.getBbHeight() * 1.0);
        AABB visualBounds = entity.getBoundingBox().inflate(horizontal, vertical, horizontal);
        return frustum.isVisible(visualBounds);
    }
}
