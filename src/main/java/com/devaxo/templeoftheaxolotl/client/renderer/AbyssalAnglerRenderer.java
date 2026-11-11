package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.client.model.AbyssalAnglerModel;
import com.devaxo.templeoftheaxolotl.entity.AbyssalAnglerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class AbyssalAnglerRenderer extends GeoEntityRenderer<AbyssalAnglerEntity> {
    public AbyssalAnglerRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalAnglerModel());
        this.shadowRadius = 0.0F;
    }
    @Override public RenderType getRenderType(AbyssalAnglerEntity animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
    @Override public void render(AbyssalAnglerEntity entity, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.scale(6.0F, 6.0F, 6.0F);
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
        poseStack.popPose();
    }
    // Yaw-independent bounds cover the scaled model even when its center is offscreen.
    @Override
    public boolean shouldRender(AbyssalAnglerEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        double horizontal = Math.max(2.0, entity.getBbWidth() * 6.0 * 1.5);
        double vertical = Math.max(2.0, entity.getBbHeight() * 6.0);
        AABB visualBounds = entity.getBoundingBox().inflate(horizontal, vertical, horizontal);
        return frustum.isVisible(visualBounds);
    }
}
