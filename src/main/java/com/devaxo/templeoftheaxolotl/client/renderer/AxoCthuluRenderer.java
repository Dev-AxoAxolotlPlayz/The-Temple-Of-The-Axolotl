package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.entity.AxoCthuluEntity;
import com.devaxo.templeoftheaxolotl.client.model.AxoCthuluModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AxoCthuluRenderer extends GeoEntityRenderer<AxoCthuluEntity> {
    public AxoCthuluRenderer(EntityRendererProvider.Context context) {
        super(context, new AxoCthuluModel());
    }

    @Override public void render(AxoCthuluEntity entity, float yaw, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        // Eye cube is X[-24,24], Y[87,135], Z[-11.60947,34.79053] in 16px/block.
        // Align the eye itself to the 3x3x2.9 entity hitbox, not the tentacle silhouette.
        // Scale the entire GeckoLib model, then offset the eye into the entity AABB.
        // The offset is also scaled, keeping the eye centered on the collision box.
        poseStack.scale(3.0F, 3.0F, 3.0F);
        poseStack.translate(0.0, -87.0 / 16.0, -11.59053 / 16.0);
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        poseStack.popPose();
    }

    @Override public boolean shouldRender(AxoCthuluEntity entity, Frustum frustum,
                                          double camX, double camY, double camZ) {
        AABB bounds = entity.getBoundingBox().inflate(120.0, 120.0, 120.0);
        return frustum.isVisible(bounds);
    }
}
