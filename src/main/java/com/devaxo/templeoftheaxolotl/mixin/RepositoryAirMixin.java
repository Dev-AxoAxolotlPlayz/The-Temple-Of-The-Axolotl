package com.devaxo.templeoftheaxolotl.mixin;
import com.devaxo.templeoftheaxolotl.item.AxolitePlating;
import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class) public abstract class RepositoryAirMixin {
 @Inject(method="decreaseAirSupply",at=@At("RETURN"),cancellable=true) private void tank(int air,CallbackInfoReturnable<Integer> cir){if((Object)this instanceof ServerPlayer p && cir.getReturnValue()<air){var s=TempleHudModules.getSlot(p,3);int n=AxolitePlating.oxygen(s);if(n>0){AxolitePlating.oxygen(s,n-1);TempleHudModules.storeSlot(p,3,s);cir.setReturnValue(air);}}}
}
