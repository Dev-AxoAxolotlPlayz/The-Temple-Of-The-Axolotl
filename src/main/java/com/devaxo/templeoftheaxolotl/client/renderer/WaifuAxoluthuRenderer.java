package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.WaifuAxoluthuModel;
import com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WaifuAxoluthuRenderer extends GeoEntityRenderer<WaifuAxoluthuEntity> {
    private static final float MODEL_SCALE = 1.8F / 2.3F;

    @Override
    public void render(WaifuAxoluthuEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    protected void renderNameTag(WaifuAxoluthuEntity entity, net.minecraft.network.chat.Component name,
            PoseStack poseStack, MultiBufferSource buffers, int light, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.65, 0.0);
        super.renderNameTag(entity, name, poseStack, buffers, light, partialTick);
        poseStack.popPose();
    }

    public WaifuAxoluthuRenderer(EntityRendererProvider.Context context) {
        super(context, new WaifuAxoluthuModel());
    }
}
