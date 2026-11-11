package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.client.model.AxokrakenModel;
import com.devaxo.templeoftheaxolotl.entity.AxokrakenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class AxokrakenRenderer extends GeoEntityRenderer<AxokrakenEntity> {
    public AxokrakenRenderer(EntityRendererProvider.Context context) { super(context, new AxokrakenModel()); this.shadowRadius = 1.5F; }
    @Override public void render(AxokrakenEntity entity, float yaw, float partialTick, PoseStack poseStack,
                                 MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(4.0F, 4.0F, 4.0F);
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        poseStack.popPose();
    }
    // Yaw-independent bounds cover the scaled model even when its center is offscreen.
    @Override
    public boolean shouldRender(AxokrakenEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        double horizontal = Math.max(2.0, entity.getBbWidth() * 4.0 * 1.5);
        double vertical = Math.max(2.0, entity.getBbHeight() * 4.0);
        AABB visualBounds = entity.getBoundingBox().inflate(horizontal, vertical, horizontal);
        return frustum.isVisible(visualBounds);
    }
}
