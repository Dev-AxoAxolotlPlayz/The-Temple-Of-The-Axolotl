package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.client.PromiseWearableRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
@Mixin(PlayerRenderer.class)
public abstract class PromiseFirstPersonPlayerMixin {
 @Inject(method="setModelProperties",at=@At("TAIL"))
 private void tota$hideHead(AbstractClientPlayer player,CallbackInfo ci){
   if(PromiseWearableRender.isFirstPersonBody(player)){
     var model=((PlayerRenderer)(Object)this).getModel();
     model.head.visible=false; model.hat.visible=false;
   }
 }
}
