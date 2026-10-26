package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Full replacement for the vanilla title visual pass. Vanilla TitleScreen widgets
 * remain real interactive widgets, but its panorama and logo are never rendered.
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class CelestialTitleMenu {
    private static final ResourceLocation STAGE = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "textures/gui/celestial_stage.png");
    private static final ResourceLocation SECONDARY_STAGE = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "textures/gui/celestial_secondary_stage.png");
    private static final ResourceLocation MUSIC_ID = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "celestial_menu");
    private static final SoundEvent MUSIC = SoundEvent.createVariableRangeEvent(MUSIC_ID);
    private static SimpleSoundInstance musicInstance;
    private static long ticks;
    private static boolean onTitle;
    private CelestialTitleMenu() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        int w = event.getScreen().width;
        int h = event.getScreen().height;
        List<AbstractWidget> wide = new ArrayList<>();
        List<AbstractWidget> small = new ArrayList<>();
        event.getScreen().children().forEach(child -> {
            if (child instanceof AbstractWidget widget) {
                if (widget.getWidth() >= 95) wide.add(widget);
                else small.add(widget);
            }
        });
        wide.sort(Comparator.comparingInt(AbstractWidget::getY).thenComparingInt(AbstractWidget::getX));
        // Layout is expressed as fractions of the logical viewport, not fixed GUI pixels.
        // Vanilla GUI scale changes w/h; these ratios preserve the same composition.
        int bw = Math.max(80, Math.round(w * 0.18f));
        int bh = Math.max(12, Math.round(h * 0.047f));
        int left = Math.max(8, Math.round(w * 0.043f));
        int gap = Math.max(3, Math.round(h * 0.012f));
        int step = bh + gap;
        int start = Math.max(10, Math.round(h * 0.36f));
        if (start + wide.size() * step > h * 0.87f) {
            bh = Math.max(10, (int)(h * 0.49f / Math.max(1, wide.size())) - gap);
            step = bh + gap;
        }
        for (int i = 0; i < wide.size(); i++) {
            AbstractWidget b = wide.get(i);
            b.setX(left);
            b.setY(start + i * step);
            b.setWidth(bw);
            b.setHeight(bh);
        }
        int smallY = Math.min(h - 16, start + wide.size() * step + gap);
        int smallWidth = Math.max(12, Math.round(w * 0.024f));
        for (int i = 0; i < small.size(); i++) {
            AbstractWidget b = small.get(i);
            b.setX(left + i * (smallWidth + gap));
            b.setY(smallY);
            b.setWidth(smallWidth);
            b.setHeight(Math.max(12, bh));
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean active = isCelestialScreen(mc.screen) || (mc.level == null && mc.getOverlay() != null);
        if (active && mc.level == null) {
            // Vanilla MusicManager can otherwise start its own menu music over ours.
            mc.getMusicManager().stopPlaying();
        }
        if (active) {
            ticks++;
            if (!onTitle || musicInstance == null || !mc.getSoundManager().isActive(musicInstance)) {
                musicInstance = SimpleSoundInstance.forUI(MUSIC, 1.0F, 1.0F);
                mc.getSoundManager().play(musicInstance);
            }
        } else if (onTitle) {
            if (musicInstance != null) mc.getSoundManager().stop(musicInstance);
            musicInstance = null;
        }
        onTitle = active;
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Pre event) {
        if (isLoadingScreen(event.getScreen())) {
            // Loading screens have special progress indicators; preserve their own
            // renderers rather than replacing them with a generic button list.
            // The background is rendered via the shared celestial screen pass.
            event.setCanceled(true);
            GuiGraphics g = event.getGuiGraphics();
            int w = event.getScreen().width, h = event.getScreen().height;
            drawCover(g, SECONDARY_STAGE, w, h);
            drawKelp(g, w, h);
            g.drawCenteredString(Minecraft.getInstance().font,
                    event.getScreen().getTitle(), w / 2, h / 2 - 8, 0xFFEFE8D6);
            for (var child : event.getScreen().children()) {
                if (child instanceof Renderable renderable)
                    renderable.render(g, event.getMouseX(), event.getMouseY(), event.getPartialTick());
            }
            return;
        }
        if (isSecondaryScreen(event.getScreen())) {
            // Render the clean cosmic backdrop and the screen's original interactive
            // renderables (including scrolling world/server/mod lists). This avoids
            // vanilla's opaque dirt/panorama background covering the artwork.
            event.setCanceled(true);
            GuiGraphics g = event.getGuiGraphics();
            int w = event.getScreen().width, h = event.getScreen().height;
            drawCover(g, SECONDARY_STAGE, w, h);
            drawKelp(g, w, h);
            for (var child : event.getScreen().children()) {
                if (child instanceof Renderable renderable) {
                    renderable.render(g, event.getMouseX(), event.getMouseY(), event.getPartialTick());
                }
            }
            // These screens normally draw their heading outside their widget list.
            g.drawCenteredString(Minecraft.getInstance().font,
                    event.getScreen().getTitle(), w / 2, 10, 0xFFEFE8D6);
            return;
        }
        if (!(event.getScreen() instanceof TitleScreen)) return;
        // Cancel the entire vanilla pass, including the panorama, logo and splash.
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int w = event.getScreen().width, h = event.getScreen().height;
        drawCover(g, STAGE, w, h);
        drawKelp(g, w, h);
        // Title and subtitle are already baked into the approved cosmic-ocean artwork.
        // Do not render the old oversized title layers on top of them.

        // A single movement clock for swimming and staring. Travel freezes during inspection.
        int duration = Math.max(140, w * 2);
        int pauseAt = (int)(duration * 0.82); // Inspect the Temple on the right
        int pauseLength = 95; // Freeze travel while Cluckles inspects
        int phase = (int)Math.floorMod(ticks, duration + pauseLength);
        boolean stare = phase >= pauseAt && phase < pauseAt + pauseLength;
        int travel = phase < pauseAt ? phase : phase < pauseAt + pauseLength ? pauseAt : phase - pauseLength;
        int x = -45 + (int)((w + 110.0) * travel / duration);
        int y = h * 79 / 100 + (int)(3 * Math.sin(ticks * .06));
        float spriteScale = Math.max(0.65f, Math.min(1.6f, w / 600.0f));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(spriteScale, spriteScale, 1.0f);
        drawCluckles(g, 0, 0, stare);
        g.pose().popPose();

        // Render actual Minecraft widgets as celestial-stone controls; input and actions
        // remain owned by vanilla/NeoForge and are fully functional.
        event.getScreen().children().forEach(child -> {
            if (!(child instanceof AbstractWidget b) || !b.visible) return;
            int x0 = b.getX(), y0 = b.getY(), x1 = x0 + b.getWidth(), y1 = y0 + b.getHeight();
            boolean hover = b.isMouseOver(event.getMouseX(), event.getMouseY());
            g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, hover ? 0xFFE5D08D : 0xFF947D56);
            g.fill(x0, y0, x1, y1, hover ? 0xFF316B72 : 0xE9163D50);
            g.fill(x0 + 3, y0 + 2, x1 - 3, y0 + 3, hover ? 0xFF91F6E2 : 0xFF4A9D9A);
            // Fit button text to the available width; no spill into neighboring controls.
            float textScale = Math.min(1.8f, Math.max(0.5f, b.getHeight() / 19.0f));
            int textWidth = mc.font.width(b.getMessage());
            if (textWidth > 0) textScale = Math.min(textScale, (b.getWidth() - 8.0f) / textWidth);
            textScale = Math.max(0.35f, textScale);
            g.pose().pushPose();
            g.pose().translate((x0 + x1) / 2.0f, (y0 + y1) / 2.0f, 0);
            g.pose().scale(textScale, textScale, 1.0f);
            g.drawCenteredString(mc.font, b.getMessage(), 0, -4,
                    b.active ? 0xFFF3E9D4 : 0xFF929D9F);
            g.pose().popPose();
        });
    }

    /** Limit the persistent menu soundtrack and background to the requested screens. */
    private static boolean isCelestialScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof TitleScreen || isSecondaryScreen(screen) || isLoadingScreen(screen);
    }

    private static boolean isSecondaryScreen(net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getSimpleName();
        return name.equals("SelectWorldScreen")
                || name.equals("JoinMultiplayerScreen")
                || name.equals("ModListScreen")
                || name.equals("CreateWorldScreen")
                || name.equals("WorldCreationScreen");
    }

    /** Dedicated world preparation and connection screens; never alter in-world HUD. */
    private static boolean isLoadingScreen(net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getSimpleName();
        return name.equals("LevelLoadingScreen")
                || name.equals("ReceivingLevelScreen")
                || name.equals("ProgressScreen")
                || name.equals("GenericDirtMessageScreen")
                || name.equals("ConnectScreen")
                || name.equals("DownloadingTerrainScreen");
    }

    /** Aspect-ratio-preserving cover, calculated in GUI coordinates on every frame. */
    private static void drawCover(GuiGraphics g, ResourceLocation texture, int w, int h) {
        final int tw = 1711, th = 919;
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
        g.blit(texture, 0, 0, w, h, srcX, srcY, srcW, srcH, tw, th);
    }

    /** Shared animated foreground; never remove the living kelp again. */
    private static void drawKelp(GuiGraphics g, int w, int h) {
        for (int i = 0; i < 26; i++) {
            int x = Math.floorMod(i * 113 + 17, Math.max(1, w));
            int base = h - (i * 19 % Math.max(1, h / 6));
            int length = h / 9 + i * 7 % Math.max(1, h / 5);
            for (int n = 0; n < 7; n++) {
                int y = base - n * length / 7;
                int sway = (int) (5 * Math.sin(ticks * 0.025 + i + n * .55));
                g.fill(x + sway, y, x + sway + 2, y + 5, 0x6650EBC9);
            }
        }
    }

    private static void drawCluckles(GuiGraphics g, int x, int y, boolean stare) {
        int kick = stare ? 0 : (int)(5 * Math.sin(ticks * .19));
        g.fill(x - 23, y + 6, x + 20, y + 28, 0xFFE7E4D9);
        g.fill(x - 31, y + 11, x - 20, y + 22, 0xFFB8B9B5);
        g.fill(x - 14, y + 27, x - 9, y + 40 + kick, 0xFFDEA02D);
        g.fill(x + 8, y + 27, x + 13, y + 40 - kick, 0xFFDEA02D);
        g.fill(x - 27, y + 8 - kick, x - 15, y + 18 - kick, 0xFFF4F0E6);
        g.fill(x + 16, y + 8 + kick, x + 28, y + 18 + kick, 0xFFF4F0E6);
        g.fill(x - 19, y - 19, x + 19, y + 18, 0x7073D7F4);
        g.fill(x - 14, y - 15, x + 14, y + 14, 0xFFF3EEDC);
        g.fill(x - 6, y - 6, x - 2, y - 1, 0xFF171717);
        g.fill(x + 3, y - 6, x + 7, y - 1, 0xFF171717);
        g.fill(x - 5, y + 2, x + 8, y + 7, 0xFFF4B336);
        g.fill(x - 2, y - 19, x + 5, y - 15, 0xFFE13D30);
        g.fill(x - 19, y - 19, x + 19, y - 17, 0xC5C5F2FF);
        g.fill(x - 19, y + 16, x + 19, y + 18, 0xC5C5F2FF);
        if (stare) g.fill(x - 8, y - 10, x + 9, y - 8, 0xFF312A2B);
        for (int i = 0; i < 4; i++) {
            int bx = x + 24 + i * 9;
            int by = y - 10 - Math.floorMod((int)(ticks / 3) + i * 13, 38);
            g.fill(bx, by, bx + 3, by + 3, 0xA0BDEEFF);
        }
    }
}
