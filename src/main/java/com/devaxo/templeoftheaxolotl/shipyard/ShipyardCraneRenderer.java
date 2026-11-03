package com.devaxo.templeoftheaxolotl.shipyard;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
public class ShipyardCraneRenderer extends GeoBlockRenderer<ShipyardCraneBlockEntity>{
 public ShipyardCraneRenderer(BlockEntityRendererProvider.Context c){super(new ShipyardCraneModel());}
 @Override public void render(ShipyardCraneBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  pose.pushPose();
  // The original geometry's root pivot is [0,0,0]. Reposition its machinery footprint at the block center.
  pose.translate(0.5,0,0.5);
  // Base spans roughly 106 model units; 0.45 scales it to ~3 blocks across.
  pose.scale(0.45F,0.45F,0.45F);
  pose.translate(-0.5,0,-0.5);
  super.render(be,partial,pose,buffers,light,overlay);pose.popPose();
 }
 @Override public AABB getRenderBoundingBox(ShipyardCraneBlockEntity be){return new AABB(be.getBlockPos()).inflate(16,20,16);}
}
