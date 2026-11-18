
package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.effect.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class ShellShockedEndermanMixin {

    @Inject(method = "teleport()Z", at = @At("HEAD"), cancellable = true)
    private void tota$noRandomTeleport(CallbackInfoReturnable<Boolean> cir) {
        if (((EnderMan) (Object) this).hasEffect(ModEffects.SHELL_SHOCKED)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "teleportTowards", at = @At("HEAD"), cancellable = true)
    private void tota$noTargetTeleport(
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (((EnderMan) (Object) this).hasEffect(ModEffects.SHELL_SHOCKED)) {
            cir.setReturnValue(false);
        }
    }
}
