package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.client.model.AxobloopModel;
import com.devaxo.templeoftheaxolotl.entity.AxobloopEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class AxobloopRenderer extends GeoEntityRenderer<AxobloopEntity> {
    public AxobloopRenderer(EntityRendererProvider.Context context) {
        super(context, new AxobloopModel());
        this.shadowRadius = 0.0F;
    }
    @Override public RenderType getRenderType(AxobloopEntity animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
    @Override public void render(AxobloopEntity entity, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(10.0F, 10.0F, 10.0F);
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        poseStack.popPose();
    }
    // Yaw-independent bounds cover the scaled model even when its center is offscreen.
    @Override
    public boolean shouldRender(AxobloopEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        double horizontal = Math.max(2.0, entity.getBbWidth() * 10.0 * 1.5);
        double vertical = Math.max(2.0, entity.getBbHeight() * 10.0);
        AABB visualBounds = entity.getBoundingBox().inflate(horizontal, vertical, horizontal);
        return frustum.isVisible(visualBounds);
    }
}
