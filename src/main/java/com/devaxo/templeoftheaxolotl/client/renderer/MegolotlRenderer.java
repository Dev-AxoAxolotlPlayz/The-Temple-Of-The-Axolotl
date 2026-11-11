package com.devaxo.templeoftheaxolotl.client.renderer;
import com.devaxo.templeoftheaxolotl.client.model.MegolotlModel;
import com.devaxo.templeoftheaxolotl.entity.MegolotlEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
public class MegolotlRenderer extends GeoEntityRenderer<MegolotlEntity> {
 public MegolotlRenderer(EntityRendererProvider.Context context) {super(context,new MegolotlModel());shadowRadius=0.0F;}
 
 @Override public void render(MegolotlEntity entity,float yaw,float partialTick,PoseStack pose,MultiBufferSource buffers,int light) {
  pose.pushPose();pose.scale(6.0F,6.0F,6.0F);
  super.render(entity,yaw,partialTick,pose,buffers,light);pose.popPose();
 }
}
