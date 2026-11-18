package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.trident.AxoliteGuidedPhasing;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** onHitBlock belongs to AbstractArrow, not ThrownTrident in 1.21.1. */
@Mixin(AbstractArrow.class)
public abstract class AxoliteGuidedBlockCollisionMixin {
    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void tota$ignoreGuidedBlockHit(BlockHitResult hit, CallbackInfo ci) {
        Object arrow = this;
        if (arrow instanceof ThrownTrident && arrow instanceof AxoliteGuidedPhasing state
                && state.tota$isGuidedPhasing()) {
            ci.cancel();
        }
    }
}
