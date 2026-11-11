package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.abyss.WatergateBlock;
import com.devaxo.templeoftheaxolotl.abyss.WatergateEntity;
import com.devaxo.templeoftheaxolotl.client.model.WatergateModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Renders the complete Blockbench frame plus a pulsing translucent interior surface. */
public final class WatergateRenderer extends GeoBlockRenderer<WatergateEntity> {
    private static final ResourceLocation PORTAL = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    public WatergateRenderer(BlockEntityRendererProvider.Context context) { super(new WatergateModel()); }

    @Override public void render(WatergateEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // GeckoLib rotates the GeoBlockRenderer model using its FACING state.
        // Do not rotate the model twice. Apply the same facing only to the
        // custom portal surface, which is rendered outside GeckoLib.
        super.render(entity, partialTick, pose, buffers, light, overlay);
        float yaw = switch (entity.getBlockState().getValue(WatergateBlock.FACING)) {
            case SOUTH -> 180f; case EAST -> 90f; case WEST -> 270f; default -> 0f;
        };
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-.5, 0, -.5);
        // Controller-driven loading beams. Stages 1..8: violet attempt, cyan complete;
        // 9..13: fifth pair stays cyan while earlier pairs shut off every 20 ticks.
        int stage = entity.getAwakeningStage();
        int progress = entity.getChunkProgress();
        if (stage >= 9 && stage < 14 || stage < 9 && progress != 0) {
            VertexConsumer beamBuffer = buffers.getBuffer(RenderType.entityTranslucent(PORTAL));
            Matrix4f beamMatrix = pose.last().pose();
            // Coordinates relative to the Watergate controller at the bottom center.
            // Five nested pairs: outermost first, innermost fifth.
            // Every beam starts at the controller and ends at its own frame position.
            float[][] endpoints = {
                {1.53f, 0.83f},  // lowest runes: bottom pair, deliberately included
                {1.52f, 1.76f},  // middle-lower side runes
                {1.52f, 2.75f},  // upper side runes
                {1.12f, 3.75f},  // upper arch runes
                {0.36f, 4.18f}   // central crown completion pair
            };
            for (int pair = 1; pair <= 5; pair++) {
                int status = pair <= 4 ? ((progress >> ((pair - 1) * 2)) & 3) : 0;
                boolean on = pair == 5 ? stage >= 9 && stage < 14
                        : stage >= 9 ? stage < 9 + pair : status != 0;
                if (!on) continue;
                boolean violet = stage < 9 && pair <= 4 && status == 1;
                float[] target = endpoints[pair - 1];
                for (int side = -1; side <= 1; side += 2) {
                    beam(beamBuffer, beamMatrix, 0.5f, 0.28f,
                            0.5f + side * target[0], target[1], violet, overlay);
                }
            }
        }
        // Restore original X center (0.5), and move the panel along Z instead (0.5).
        // Animation is a UV scroll and pulse, not a new texture dependency.
        if (entity.getLevel() != null && entity.getBlockState().getValue(WatergateBlock.READY)) {
            float t = (entity.getLevel().getGameTime() + partialTick) * .014f;
            float pulse = .68f + .14f * (float)Math.sin(t * 3.1f);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(PORTAL));
            PoseStack.Pose last = pose.last();
            Matrix4f matrix = last.pose();
            float u = t % 1f;
            // Six faces of a 1/16-block-thick portal, centered on the verified Z=0.5 plane.
            // The frame geometry and the working horizontal alignment are unchanged.
            float zFront = .5f + 1f / 32f;
            float zBack = .5f - 1f / 32f;
            panel(vc, matrix, -1f, 0f, zFront, 2f, 4f, u, pulse, light, overlay, 0, 0, 1);
            panel(vc, matrix, 2f, 0f, zBack, -1f, 4f, u, pulse, light, overlay, 0, 0, -1);
            edge(vc, matrix, -1f, 0f, zBack, -1f, 4f, zFront, pulse, light, overlay, -1, 0, 0);
            edge(vc, matrix, 2f, 0f, zFront, 2f, 4f, zBack, pulse, light, overlay, 1, 0, 0);
            cap(vc, matrix, -1f, 4f, zFront, 2f, zBack, pulse, light, overlay, 0, 1, 0);
            cap(vc, matrix, -1f, 0f, zBack, 2f, zFront, pulse, light, overlay, 0, -1, 0);
        }
        pose.popPose();
    }

    @Override public boolean shouldRenderOffScreen(WatergateEntity entity) { return true; }

    // The Blockbench arch extends far outside the one-block controller.
    @Override public AABB getRenderBoundingBox(WatergateEntity entity) {
        return new AABB(entity.getBlockPos()).inflate(4, 2, 4).expandTowards(0, 5, 0);
    }

    private static void beam(VertexConsumer vc, Matrix4f m, float x1, float y1,
                             float x2, float y2, boolean violet, int overlay) {
        float z = 0.50f;
        float half = .035f;
        beamPoint(vc,m,x1-half,y1,z, 0,0,violet,overlay);
        beamPoint(vc,m,x1+half,y1,z, 1,0,violet,overlay);
        beamPoint(vc,m,x2+half,y2,z, 1,1,violet,overlay);
        beamPoint(vc,m,x2-half,y2,z, 0,1,violet,overlay);
        beamPoint(vc,m,x2-half,y2,z, 0,1,violet,overlay);
        beamPoint(vc,m,x2+half,y2,z, 1,1,violet,overlay);
        beamPoint(vc,m,x1+half,y1,z, 1,0,violet,overlay);
        beamPoint(vc,m,x1-half,y1,z, 0,0,violet,overlay);
    }
    private static void beamPoint(VertexConsumer vc, Matrix4f m, float x,float y,float z,
                                  float u, float v, boolean violet,int overlay) {
        vc.addVertex(m,x,y,z)
          .setColor(violet ? 166 : 55, violet ? 94 : 242, violet ? 255 : 245, 230)
          .setUv(u,v).setOverlay(overlay).setLight(0x00F000F0).setNormal(0,0,1);
    }

    private static void panel(VertexConsumer vc, Matrix4f m, float l, float b, float z,
                              float r, float t, float u, float a, int light, int overlay,
                              float nx, float ny, float nz) {
        point(vc,m,l,b,z,u,0,a,light,overlay,nx,ny,nz);
        point(vc,m,r,b,z,u+1,0,a,light,overlay,nx,ny,nz);
        point(vc,m,r,t,z,u+1,1,a,light,overlay,nx,ny,nz);
        point(vc,m,l,t,z,u,1,a,light,overlay,nx,ny,nz);
    }
    private static void edge(VertexConsumer vc, Matrix4f m, float x1,float y1,float z1,
                             float x2,float y2,float z2,float a,int light,int overlay,
                             float nx,float ny,float nz) {
        point(vc,m,x1,y1,z1,0,0,a,light,overlay,nx,ny,nz);
        point(vc,m,x2,y1,z2,1,0,a,light,overlay,nx,ny,nz);
        point(vc,m,x2,y2,z2,1,1,a,light,overlay,nx,ny,nz);
        point(vc,m,x1,y2,z1,0,1,a,light,overlay,nx,ny,nz);
    }
    private static void cap(VertexConsumer vc, Matrix4f m, float x1,float y,float z1,
                            float x2,float z2,float a,int light,int overlay,
                            float nx,float ny,float nz) {
        point(vc,m,x1,y,z1,0,0,a,light,overlay,nx,ny,nz);
        point(vc,m,x2,y,z1,1,0,a,light,overlay,nx,ny,nz);
        point(vc,m,x2,y,z2,1,1,a,light,overlay,nx,ny,nz);
        point(vc,m,x1,y,z2,0,1,a,light,overlay,nx,ny,nz);
    }
    private static void point(VertexConsumer vc, Matrix4f m, float x,float y,float z,
                              float u,float v,float a,int light,int overlay,
                              float nx,float ny,float nz) {
        vc.addVertex(m,x,y,z).setColor(50,221,230,115).setUv(u,v)
          .setOverlay(overlay).setLight(0x00F000F0).setNormal(nx,ny,nz);
    }
}
