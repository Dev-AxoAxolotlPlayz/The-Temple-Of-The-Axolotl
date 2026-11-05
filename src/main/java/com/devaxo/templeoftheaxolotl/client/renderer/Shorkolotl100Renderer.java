package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.client.model.Shorkolotl100Model;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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

    /** Render two continuous, short translucent headlight shafts.
     * Geometry is client-only; actual block lighting remains server-managed.
     * Coordinates are local to the submarine, matching the entity headlight origins.
     */
    @Override
    public void render(Shorkolotl100Entity submarine, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(submarine, entityYaw, partialTick, poseStack, buffers, packedLight);
        if (!submarine.headlightsOn() || !submarine.isInWater()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() == submarine
                && mc.options.getCameraType().isFirstPerson()) return;

        poseStack.pushPose();
        float yaw = Mth.rotLerp(partialTick, submarine.yRotO, submarine.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        // The vanilla beacon texture provides a continuous shaft appearance,
        // without spawning any particles or requiring a beacon block.
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(
                ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png")));
        float forward = (float)Math.cos(Math.toRadians(25));
        float drop = (float)Math.sin(Math.toRadians(25));
        for (int side : new int[]{-1, 1}) {
            float cx = side * 0.98f;
            float y = 1.45f;
            float z = 2.0f;
            float endY = y - 5.0f * drop;
            float endZ = z + 5.0f * forward;
            float startWidth = 0.14f;
            float endWidth = 0.33f;
            // Crossed ribbons keep the beam visible from the side and above.
            quad(poseStack, vertices,
                    cx-startWidth,y,z, cx+startWidth,y,z,
                    cx+endWidth,endY,endZ, cx-endWidth,endY,endZ);
            quad(poseStack, vertices,
                    cx,y-startWidth,z, cx,y+startWidth,z,
                    cx,endY+endWidth,endZ, cx,endY-endWidth,endZ);
        }
        poseStack.popPose();
    }

    private static void quad(PoseStack stack, VertexConsumer out,
                             float ax,float ay,float az, float bx,float by,float bz,
                             float cx,float cy,float cz, float dx,float dy,float dz) {
        var pose = stack.last();
        // Double-sided geometry; subtle opacity avoids obstructing the camera.
        vertex(out, pose, ax,ay,az, 0,0);
        vertex(out, pose, bx,by,bz, 1,0);
        vertex(out, pose, cx,cy,cz, 1,1);
        vertex(out, pose, dx,dy,dz, 0,1);
        vertex(out, pose, dx,dy,dz, 0,1);
        vertex(out, pose, cx,cy,cz, 1,1);
        vertex(out, pose, bx,by,bz, 1,0);
        vertex(out, pose, ax,ay,az, 0,0);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose,
                               float x,float y,float z, float u,float v) {
        out.addVertex(pose.pose(), x,y,z)
                .setColor(190, 245, 255, 95)
                .setUv(u,v)
                .setOverlay(0)
                .setLight(0xF000F0)
                .setNormal(pose, 0, 1, 0);
    }

}
