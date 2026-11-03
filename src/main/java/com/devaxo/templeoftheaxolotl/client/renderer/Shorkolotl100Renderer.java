package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.Shorkolotl100Model;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Shorkolotl100Renderer extends GeoEntityRenderer<Shorkolotl100Entity> {
    public static final float MODEL_SCALE = 1.8F;

    public Shorkolotl100Renderer(EntityRendererProvider.Context context) {
        super(context, new Shorkolotl100Model());
    }

    @Override
    public void preRender(PoseStack poseStack, Shorkolotl100Entity animatable,
                          software.bernie.geckolib.cache.object.BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        Minecraft mc = Minecraft.getInstance();
        boolean localCockpit = mc.player != null && mc.player.getVehicle() == animatable
                && mc.options.getCameraType().isFirstPerson();
        if (localCockpit) poseStack.scale(0.0F, 0.0F, 0.0F);
        else if (!isReRender) poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        // Keep the live vehicle heading, with the model facing corrected by 180 degrees.
        // GeoEntityRenderer's non-LivingEntity path does not supply Mob body yaw.
        // Follow the vehicle's actual interpolated yaw every frame, even at rest.
        // This affects visuals only: steering, camera and movement remain untouched.
        if (!isReRender && !localCockpit) {
            float heading = net.minecraft.util.Mth.rotLerp(partialTick,
                    animatable.yRotO, animatable.getYRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(-heading));
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
