package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.entity.AxoleviathanEntity;
import com.devaxo.templeoftheaxolotl.client.model.AxoleviathanModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 8x visuals, expanded culling, head alignment and velocity-derived pitch. */
public class AxoleviathanRenderer extends GeoEntityRenderer<AxoleviathanEntity> {
    public AxoleviathanRenderer(EntityRendererProvider.Context context) {
        super(context, new AxoleviathanModel());
    }

    @Override
    public void render(AxoleviathanEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        // Apply pitch around the model's local X axis, accounting for its yaw.
        // This rotates the WHOLE articulated model rather than only the skull.
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
        // Shift the visual model down relative to the physical entity so its
        // existing 3x3x3 hitbox sits higher, nearer the skull instead of the chin.
        // This does not change the physics box or water navigation origin.
        poseStack.translate(0.0D, -4.0D, 0.0D);
        float variantScale = entity.isEldritchVariant() ? 0.7F : 1.0F;
        poseStack.scale(8.0F * variantScale, 8.0F * variantScale, 8.0F * variantScale);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(AxoleviathanEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        AABB visualBounds = entity.getBoundingBox().inflate(460.0, 450.0, 460.0);
        return frustum.isVisible(visualBounds);
    }
}
