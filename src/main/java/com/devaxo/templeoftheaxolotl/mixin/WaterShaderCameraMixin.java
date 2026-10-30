package com.devaxo.templeoftheaxolotl.mixin;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Camera/world-space information for Celestial Water; visuals only. */
@Mixin(ShaderInstance.class)
public abstract class WaterShaderCameraMixin {
    @Inject(method = "apply", at = @At("HEAD"), require = 0)
    private void tota$cameraUniform(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance)(Object)this;
        Uniform xz = shader.getUniform("TotaCameraXZ");
        Uniform cameraY = shader.getUniform("TotaCameraY");
        Uniform seaLevel = shader.getUniform("TotaSeaLevel");
        Uniform depthRange = shader.getUniform("TotaDepthRange");
        if (xz == null && cameraY == null && seaLevel == null && depthRange == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var position = mc.gameRenderer.getMainCamera().getPosition();
        if (xz != null) xz.set((float)position.x, (float)position.z);
        if (cameraY != null) cameraY.set((float)position.y);

        // The Abyss dimension is still planned. Match the eventual dimension path
        // containing "abyss" until its definitive resource key is established.
        boolean abyss = mc.level.dimension().location().getPath().contains("abyss");
        if (seaLevel != null) seaLevel.set(abyss ? 200.0f : 62.0f);
        if (depthRange != null) depthRange.set(abyss ? 200.0f : 92.0f);
    }
}
