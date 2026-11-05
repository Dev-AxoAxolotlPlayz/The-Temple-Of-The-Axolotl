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
        // Restore original X center (0.5), and move the panel along Z instead (0.5).
        // Animation is a UV scroll and pulse, not a new texture dependency.
        if (entity.getLevel() != null) {
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
