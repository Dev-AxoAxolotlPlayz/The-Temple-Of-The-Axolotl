package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.combat.ShellShockedLogic;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class ShellShockedCreeperMixin {
    @Inject(method="explodeCreeper",at=@At("HEAD"),cancellable=true)
    private void tota$stopExplosion(CallbackInfo ci) {
        if(ShellShockedLogic.stunned((Creeper)(Object)this)) ci.cancel();
    }
}
