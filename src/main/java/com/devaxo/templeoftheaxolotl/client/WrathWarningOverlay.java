package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Translucent Fallen Wrath eyes shown after a successful RNGesus Curse warp. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class WrathWarningOverlay {
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "textures/gui/wrath_warning.png");
    private static final int TEXTURE_WIDTH = 1527;
    private static final int TEXTURE_HEIGHT = 1030;
    private static int remaining;

    private WrathWarningOverlay() {}
    public static void show() { remaining = 45; }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) return;
        if (remaining > 0) remaining--;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        if (remaining <= 0) return;
        var graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        // Preserve the artwork's original aspect ratio instead of squashing the eyes.
        int width = Math.min(screenWidth * 2 / 3,
                (int) ((long) screenHeight * 2 * TEXTURE_WIDTH / (3L * TEXTURE_HEIGHT)));
        int height = (int) ((long) width * TEXTURE_HEIGHT / TEXTURE_WIDTH);
        int x = (screenWidth - width) / 2;
        int y = (screenHeight - height) / 2;

        // The PNG already has a transparent background. Halve the opacity of the
        // remaining eye pixels, similar to the Elder Guardian apparition.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        try {
            graphics.blit(EYES, x, y, 0, 0, width, height, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }
}
