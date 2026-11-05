package com.devaxo.templeoftheaxolotl.mixin;

import com.devaxo.templeoftheaxolotl.client.CelestialSkies;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sky-pass injection using the same 1.21.1 signature as the working aurora mixin. */
@Mixin(LevelRenderer.class)
public abstract class CelestialSkyPassMixin {
    @Inject(method = "renderSky", at = @At("TAIL"), require = 0)
    private void tota$renderCelestialSky(Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix, float partialTick, Camera camera,
            boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        // The End may be flagged foggy by vanilla; the End nebula should
        // still render there. Overworld sky respects the existing renderer rules.
        CelestialSkies.renderSky(modelViewMatrix, partialTick);
    }
}
