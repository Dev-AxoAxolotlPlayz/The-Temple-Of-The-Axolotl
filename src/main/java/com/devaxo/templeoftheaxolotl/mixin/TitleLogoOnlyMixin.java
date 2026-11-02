package com.devaxo.templeoftheaxolotl.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.LogoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pressure test #4: suppress only vanilla LogoRenderer on the title screen. */
@Mixin(LogoRenderer.class)
public abstract class TitleLogoOnlyMixin {
    @Inject(method = "renderLogo", at = @At("HEAD"), cancellable = true)
    private void tota$hideVanillaTitleArt(CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof TitleScreen) {
            ci.cancel();
        }
    }
}
