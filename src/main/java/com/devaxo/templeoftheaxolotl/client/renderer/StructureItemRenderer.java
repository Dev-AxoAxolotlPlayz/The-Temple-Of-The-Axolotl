package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.StructureItemModel;
import com.devaxo.templeoftheaxolotl.item.StructureGeoBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Scales large multi-block geo meshes into the inventory item viewport. */
public final class StructureItemRenderer extends GeoItemRenderer<StructureGeoBlockItem> {
    public StructureItemRenderer() { super(new StructureItemModel()); }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        boolean crane = stack.getItem() instanceof StructureGeoBlockItem item && "shipyard_crane".equals(item.assetName());
        float scale = crane ? 0.12f : 0.22f;
        if (context == ItemDisplayContext.GUI) {
            pose.translate(0.5, 0.22, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(145));
            pose.mulPose(Axis.XP.rotationDegrees(20));
            pose.scale(scale, scale, scale);
            pose.translate(-0.5, -0.5, -0.5);
        } else {
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(scale * 1.5f, scale * 1.5f, scale * 1.5f);
            pose.translate(-0.5, -0.5, -0.5);
        }
        super.renderByItem(stack, context, pose, buffers, light, overlay);
        pose.popPose();
    }
}
