package com.devaxo.templeoftheaxolotl.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pressure test: suppress only the panorama renderer on the title screen. */
@Mixin(PanoramaRenderer.class)
public abstract class TitlePanoramaOnlyMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void tota$skipTitlePanorama(CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof TitleScreen) {
            ci.cancel();
        }
    }
}
