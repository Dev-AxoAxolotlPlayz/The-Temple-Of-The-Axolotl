package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.CelestialAnvilModel;
import com.devaxo.templeoftheaxolotl.eclipse.EclipseAltarEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class CelestialAnvilRenderer extends GeoBlockRenderer<EclipseAltarEntity> {
    @Override
    public void render(EclipseAltarEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // Blockbench mesh spans about 12.2 x 10.75 x 12.2 model units.
        // Scale up about the center of the block to fit its 16-unit footprint.
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(1.27F, 1.27F, 1.27F);
        poseStack.translate(-0.5, 0.0, -0.5);
        super.render(entity, partialTick, poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public CelestialAnvilRenderer(BlockEntityRendererProvider.Context context) {
        super(new CelestialAnvilModel());
    }
}
