
package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.BossWrathModel;
import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class BossWrathRenderer extends GeoEntityRenderer<BossWrathEntity> {

    public BossWrathRenderer(EntityRendererProvider.Context context) {
        super(context, new BossWrathModel());

        addRenderLayer(new BlockAndItemGeoLayer<BossWrathEntity>(this) {

            @Override
            protected ItemStack getStackForBone(
                    GeoBone bone,
                    BossWrathEntity animatable) {

                return "weapon_anchor".equals(bone.getName())
                        ? animatable.getItemBySlot(EquipmentSlot.MAINHAND)
                        : ItemStack.EMPTY;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(
                    GeoBone bone,
                    ItemStack stack,
                    BossWrathEntity animatable) {

                return animatable.isSecondFall()
                        && "weapon_anchor".equals(bone.getName())
                        ? ItemDisplayContext.GROUND
                        : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(
                    PoseStack poseStack,
                    GeoBone bone,
                    ItemStack stack,
                    BossWrathEntity animatable,
                    MultiBufferSource bufferSource,
                    float partialTick,
                    int packedLight,
                    int packedOverlay) {

                if ("weapon_anchor".equals(bone.getName())) {

                    poseStack.pushPose();

                    // Recenter Ring: -2 Blockbench units on X.
                    // Move Ring outward: -3 Blockbench units on Z.
                    poseStack.translate(
                            -0.125D,
                            0.0D,
                            -0.1875D);

                    // Maintain the successful 90-degree Z rotation.
                    poseStack.mulPose(
                            Axis.ZP.rotationDegrees(90.0F));

                    super.renderStackForBone(
                            poseStack,
                            bone,
                            stack,
                            animatable,
                            bufferSource,
                            partialTick,
                            packedLight,
                            packedOverlay);

                    poseStack.popPose();
                    return;
                }

                super.renderStackForBone(
                        poseStack,
                        bone,
                        stack,
                        animatable,
                        bufferSource,
                        partialTick,
                        packedLight,
                        packedOverlay);
            }
        });
    }
}
