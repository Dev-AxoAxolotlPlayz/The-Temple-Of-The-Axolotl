package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.combat.ShellShockedLogic;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class ShellShockedMobMixin {
    @Inject(method="aiStep",at=@At("HEAD"),cancellable=true)
    private void tota$freezeAI(CallbackInfo ci) {
        if(ShellShockedLogic.stunned((Mob)(Object)this)) ci.cancel();
    }
    @Inject(method="doHurtTarget",at=@At("HEAD"),cancellable=true)
    private void tota$stopAttack(Entity victim,CallbackInfoReturnable<Boolean> cir) {
        if(ShellShockedLogic.stunned((Mob)(Object)this)) cir.setReturnValue(false);
    }
}
