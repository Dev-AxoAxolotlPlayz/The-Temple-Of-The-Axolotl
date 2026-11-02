package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.LoreBlockItemModel;
import com.devaxo.templeoftheaxolotl.lore.LoreBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class LoreBlockItemRenderer extends GeoItemRenderer<LoreBlockItem> {
    public LoreBlockItemRenderer() { super(new LoreBlockItemModel()); }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.5F, 0.43F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(145F));
            poseStack.mulPose(Axis.XP.rotationDegrees(25F));
            poseStack.scale(0.52F, 0.52F, 0.52F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        } else if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.scale(0.28F, 0.28F, 0.28F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        } else if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.scale(0.40F, 0.40F, 0.40F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        } else if (context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }
        super.renderByItem(stack, context, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
