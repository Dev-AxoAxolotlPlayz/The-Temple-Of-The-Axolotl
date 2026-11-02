package com.devaxo.templeoftheaxolotl.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pressure test #3: replace only the stable opaque fill with the existing celestial stage. */
@Mixin(TitleScreen.class)
public abstract class TitleSolidBackdropMixin {
    private static final ResourceLocation TOTA_STAGE = ResourceLocation.fromNamespaceAndPath(
            "temple_of_the_axolotl", "textures/gui/celestial_stage.png");

    @Inject(method = "render", at = @At("HEAD"))
    private void tota$celestialBackdrop(GuiGraphics graphics, int mouseX, int mouseY,
                                        float partialTick, CallbackInfo ci) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        final int tw = 1711;
        final int th = 919;
        double destRatio = (double) w / Math.max(1, h);
        double textureRatio = (double) tw / th;
        int srcX = 0, srcY = 0, srcW = tw, srcH = th;
        if (destRatio > textureRatio) {
            srcH = Math.max(1, (int) Math.round(tw / destRatio));
            srcY = (th - srcH) / 2;
        } else {
            srcW = Math.max(1, (int) Math.round(th * destRatio));
            srcX = (tw - srcW) / 2;
        }
        graphics.blit(TOTA_STAGE, 0, 0, w, h, srcX, srcY, srcW, srcH, tw, th);
        com.devaxo.templeoftheaxolotl.client.CelestialTitleMenu.drawTitleForeground(graphics, w, h);
    }
}
