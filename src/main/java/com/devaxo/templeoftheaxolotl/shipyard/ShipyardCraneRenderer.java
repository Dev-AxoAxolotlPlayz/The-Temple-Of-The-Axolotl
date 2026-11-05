package com.devaxo.templeoftheaxolotl.shipyard;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.Minecraft;
import com.devaxo.templeoftheaxolotl.entity.ModEntities;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
public class ShipyardCraneRenderer extends GeoBlockRenderer<ShipyardCraneBlockEntity>{
 private Shorkolotl100Entity miniature;
 public ShipyardCraneRenderer(BlockEntityRendererProvider.Context c){super(new ShipyardCraneModel());}
 @Override public void render(ShipyardCraneBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  pose.pushPose();
  // The original geometry's root pivot is [0,0,0]. Reposition its machinery footprint at the block center.
  pose.translate(0.5,0,0.5);
  // Base spans roughly 106 model units; 0.45 scales it to ~3 blocks across.
  pose.scale(0.45F,0.45F,0.45F);
  pose.translate(-0.5,0,-0.5);
  super.render(be,partial,pose,buffers,light,overlay);pose.popPose();
  // Render the actual GeckoLib submarine geometry as a small suspended model.
  // This is visual only: the original cargo ItemEntity and attack-to-collect logic remain.
  if (be.hasMiniatureSubmarineCargo() && be.getLevel() != null) {
   if (miniature == null || miniature.level() != be.getLevel())
    miniature = ModEntities.SHORKOLOTL_100.get().create(be.getLevel());
   if (miniature != null) {
    net.minecraft.world.phys.Vec3 anchor = be.hookPosition().subtract(
        be.getBlockPos().getX(), be.getBlockPos().getY(), be.getBlockPos().getZ());
    pose.pushPose();
    pose.translate(anchor.x, anchor.y - 0.35, anchor.z);
    pose.scale(0.23F, 0.23F, 0.23F);
    Minecraft.getInstance().getEntityRenderDispatcher().render(
        miniature, 0, 0, 0, 0, partial, pose, buffers, light);
    pose.popPose();
   }
  }
 }
 @Override public AABB getRenderBoundingBox(ShipyardCraneBlockEntity be){return new AABB(be.getBlockPos()).inflate(16,20,16);}
}
