package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.client.CelestialAuroraSky;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inject at the actual vanilla sky render method. Unlike the old implementation,
 * passes the sky's own model-view matrix; does not sample RenderSystem's
 * potentially stale model-view matrix or create an AFTER_SKY event overlay.
 */
@Mixin(LevelRenderer.class)
public abstract class CelestialAuroraSkyMixin {
    @Inject(method = "renderSky", at = @At("TAIL"), require = 0)
    private void tota$auroraInSkyPass(Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix, float partialTick, Camera camera,
            boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        if (!isFoggy) CelestialAuroraSky.draw(modelViewMatrix, partialTick);
    }
}
