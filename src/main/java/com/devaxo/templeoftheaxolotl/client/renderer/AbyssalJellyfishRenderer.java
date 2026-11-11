package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.client.model.AbyssalJellyfishModel;
import com.devaxo.templeoftheaxolotl.entity.AbyssalJellyfishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class AbyssalJellyfishRenderer extends GeoEntityRenderer<AbyssalJellyfishEntity> {
 public AbyssalJellyfishRenderer(EntityRendererProvider.Context context) {super(context,new AbyssalJellyfishModel());shadowRadius=0.0F;}
 @Override public RenderType getRenderType(AbyssalJellyfishEntity entity, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {return RenderType.entityTranslucent(texture);}
 @Override public void render(AbyssalJellyfishEntity entity,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
  pose.pushPose();pose.scale(3.0F,3.0F,3.0F);
  super.render(entity,yaw,partialTick,pose,buffers,light);pose.popPose();
 }
    // Yaw-independent bounds cover the scaled model even when its center is offscreen.
    @Override
    public boolean shouldRender(AbyssalJellyfishEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        double horizontal = Math.max(2.0, entity.getBbWidth() * 3.0 * 1.5);
        double vertical = Math.max(2.0, entity.getBbHeight() * 3.0);
        AABB visualBounds = entity.getBoundingBox().inflate(horizontal, vertical, horizontal);
        return frustum.isVisible(visualBounds);
    }
}
