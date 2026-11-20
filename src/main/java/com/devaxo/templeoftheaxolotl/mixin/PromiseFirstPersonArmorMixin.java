package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.client.PromiseWearableRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
@Mixin(value = HumanoidArmorLayer.class, priority = 2000)
public abstract class PromiseFirstPersonArmorMixin {
 @Inject(method="renderArmorPiece",at=@At("HEAD"),cancellable=true)
 private void tota$hideHelmet(PoseStack pose,MultiBufferSource buffers,LivingEntity entity,EquipmentSlot slot,int light,HumanoidModel<?> model,CallbackInfo ci){
   if(slot==EquipmentSlot.HEAD && PromiseWearableRender.isFirstPersonBody(entity))ci.cancel();
 }
}
