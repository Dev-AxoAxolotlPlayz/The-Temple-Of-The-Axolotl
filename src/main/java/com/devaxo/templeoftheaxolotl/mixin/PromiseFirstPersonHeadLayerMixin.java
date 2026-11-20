package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.client.PromiseWearableRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.LivingEntity;
@Mixin(CustomHeadLayer.class)
public abstract class PromiseFirstPersonHeadLayerMixin {
 @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",at=@At("HEAD"),cancellable=true)
 private void tota$hideAccessories(PoseStack pose,MultiBufferSource buffers,int light,LivingEntity entity,float limbSwing,float limbAmount,float partial,float age,float yaw,float pitch,CallbackInfo ci){
   if(PromiseWearableRender.isFirstPersonBody(entity))ci.cancel();
 }
}
