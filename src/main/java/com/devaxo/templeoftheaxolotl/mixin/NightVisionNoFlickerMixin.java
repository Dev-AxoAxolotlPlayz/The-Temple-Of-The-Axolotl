package com.devaxo.templeoftheaxolotl.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppress vanilla's low-duration Night Vision intensity oscillation.
 * Vanilla only calls this helper while computing the Night Vision contribution.
 * An active effect stays fully bright until it expires, rather than blinking.
 * This does not change effect duration or water/boat physics.
 */
@Mixin(GameRenderer.class)
public abstract class NightVisionNoFlickerMixin {
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true, require = 0)
    private static void tota$stableNightVision(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(1.0F);
    }
}
