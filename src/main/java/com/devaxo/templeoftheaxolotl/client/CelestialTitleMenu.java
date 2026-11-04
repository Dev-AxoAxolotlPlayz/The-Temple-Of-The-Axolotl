
package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.Set;
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

import java.util.List;

/**
 * Celestial title artwork with centered Minecraft/NeoForge buttons.
 * All external mod widgets retain their native geometry and render callbacks.
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class CelestialTitleMenu {
    private static final ResourceLocation STAGE = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "textures/gui/celestial_stage.png");
    private static final ResourceLocation SECONDARY_STAGE = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "textures/gui/celestial_secondary_stage.png");
    private static final ResourceLocation MUSIC_ID = ResourceLocation.fromNamespaceAndPath(
            TempleOfTheAxolotl.MODID, "celestial_menu");
    private static SoundEvent menuSound() { return com.devaxo.templeoftheaxolotl.sound.CelestialMusicSounds.MENU.get(); }
    private static SimpleSoundInstance musicInstance;
    private static final java.nio.file.Path MUSIC_SETTINGS =
        net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("tota_title_music.properties");
    private static double menuMusicVolume = readMusicVolume();
    private static double readMusicVolume() {
        java.util.Properties properties = new java.util.Properties();
        try (var input = java.nio.file.Files.newInputStream(MUSIC_SETTINGS)) {
            properties.load(input);
            return Math.max(0, Math.min(1, Double.parseDouble(properties.getProperty("volume", "0.65"))));
        } catch (Exception ignored) { return 0.65; }
    }
    private static void setMusicVolume(double value) {
        menuMusicVolume = Math.max(0, Math.min(1, value));
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("volume", Double.toString(menuMusicVolume));
        try {
            java.nio.file.Files.createDirectories(MUSIC_SETTINGS.getParent());
            try (var output = java.nio.file.Files.newOutputStream(MUSIC_SETTINGS)) {
                properties.store(output, "Temple of the Axolotl title music volume");
            }
        } catch (java.io.IOException ignored) { }
        // Restart the actual playing sound immediately with the new volume.
        // SoundEngine snapshots source gain at playback start; changing a field alone
        // does not change an already-playing streamed sound.
        Minecraft mc = Minecraft.getInstance();
        if (musicInstance != null) mc.getSoundManager().stop(musicInstance);
        musicInstance = null;
        if (onTitle && menuMusicVolume > 0.0) {
            musicInstance = SimpleSoundInstance.forUI(menuSound(), 1.0F, (float) menuMusicVolume);
            mc.getSoundManager().play(musicInstance);
        }
    }
    private static final class MenuVolumeSlider extends AbstractSliderButton {
        MenuVolumeSlider(int x, int y, int width) {
            super(x, y, width, 20, Component.empty(), menuMusicVolume);
            updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Component.literal("Menu Music: " + Math.round(value * 100) + "%"));
        }
        @Override protected void applyValue() { setMusicVolume(value); }
    }

    private static long ticks;
    private static boolean onTitle;
    private CelestialTitleMenu() {}

    // Only these Minecraft-owned controls receive the celestial treatment.
    // External mods (Create, Essential, etc.) retain their original widget rendering.
    private static final Set<String> VANILLA_TITLE_BUTTONS = Set.of(
            "menu.singleplayer", "menu.multiplayer", "menu.online",
            "menu.options", "menu.quit", "menu.language", "narrator.button.language",
            "options.accessibility.title", "menu.accessibility"
    );

    private static String translationKey(AbstractWidget widget) {
        if (widget.getMessage().getContents() instanceof TranslatableContents contents) {
            return contents.getKey();
        }
        return "";
    }

    private static boolean isStyledTitleButton(AbstractWidget widget) {
        String key = translationKey(widget);
        return key.equals("menu.singleplayer") || key.equals("menu.multiplayer")
                || key.equals("menu.online") || key.equals("menu.options")
                || key.equals("menu.quit") || key.equals("fml.menu.mods")
                || (widget.getMessage().getString().equalsIgnoreCase("Mods") && widget.getWidth() >= 90);
    }

    private static boolean isVanillaTitleButton(AbstractWidget widget) {
        return VANILLA_TITLE_BUTTONS.contains(translationKey(widget));
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        int w = event.getScreen().width;
        int h = event.getScreen().height;
        List<AbstractWidget> vanillaWide = new ArrayList<>();
        List<AbstractWidget> modButtons = new ArrayList<>();

        event.getScreen().children().forEach(child -> {
            if (!(child instanceof AbstractWidget widget)) return;
            if (isVanillaTitleButton(widget)) {
                if (widget.getWidth() >= 95) vanillaWide.add(widget);
                // Small language/accessibility icons remain at their native positions.
            } else if (translationKey(widget).equals("fml.menu.mods")
                    || (widget.getMessage().getString().equalsIgnoreCase("Mods")
                        && widget.getWidth() >= 90)) {
                modButtons.add(widget);
            }
            // Other widgets (especially Create) retain their original size and position.
        });
        vanillaWide.sort(java.util.Comparator.comparingInt(AbstractWidget::getY)
                .thenComparingInt(AbstractWidget::getX));

        // Three centered rows, two native buttons per row.
        // Keep every existing click handler and all external-mod widgets intact.
        int pairWidth = 98;
        int pairGap = 4;
        int bh = Math.max(20, Math.min(28, Math.round(h * 0.055f)));
        int gap = Math.max(4, Math.round(h * 0.012f));

        AbstractWidget singleplayer = null;
        AbstractWidget multiplayer = null;
        AbstractWidget realms = null;
        AbstractWidget options = null;
        AbstractWidget quit = null;
        AbstractWidget mods = modButtons.isEmpty() ? null : modButtons.get(0);
        for (AbstractWidget button : vanillaWide) {
            switch (translationKey(button)) {
                case "menu.singleplayer" -> singleplayer = button;
                case "menu.multiplayer" -> multiplayer = button;
                case "menu.online" -> realms = button;
                case "menu.options" -> options = button;
                case "menu.quit" -> quit = button;
                default -> { }
            }
        }

        AbstractWidget[][] pairs = {
                {multiplayer, singleplayer},
                {realms, options},
                {mods, quit}
        };
        int rows = pairs.length;
        int stackHeight = rows * bh + (rows - 1) * gap;
        int start = Math.max(12, Math.min(h - stackHeight - 12,
                Math.round(h * 0.47f - stackHeight * 0.10f) + Math.round((bh + gap) / 2.0f)));
        int pairX = (w - (pairWidth * 2 + pairGap)) / 2;

        for (int row = 0; row < pairs.length; row++) {
            int y = start + 3 + row * (bh + gap);
            for (int column = 0; column < 2; column++) {
                AbstractWidget button = pairs[row][column];
                if (button == null) continue;
                button.setX(pairX + column * (pairWidth + pairGap));
                button.setY(y);
                button.setWidth(pairWidth);
                button.setHeight(bh);
            }
        }
        // Dedicated persisted music control; do not move other mods' widgets.
        int sliderWidth = 140;
        event.addListener(new MenuVolumeSlider((w - sliderWidth) / 2,
            Math.min(h - 26, start + stackHeight + 14), sliderWidth));
        positionEssentialButtonsLeftOfPlayer(event.getScreen());
        positionEssentialUpdateLeftOfMods(event.getScreen(), mods);
        // Language and Accessibility keep their original vanilla positions and sizes.
        // Essential and Create controls are not repositioned, resized, or reimplemented.
    }


    /**
     * Experimental Essential-only layout: move its six real action widgets to
     * the left of its existing player-preview widget. Do not replace renderers,
     * callbacks, textures, or alter the preview itself.
     *
     * If Essential's placeholder widgets aren't present, do nothing.
     */
    private static void positionEssentialButtonsLeftOfPlayer(net.minecraft.client.gui.screens.Screen screen) {
        AbstractWidget preview = null;
        java.util.Map<String, AbstractWidget> actions = new java.util.HashMap<>();
        for (var child : screen.children()) {
            if (!(child instanceof AbstractWidget widget)) continue;
            String key = translationKey(widget).toLowerCase(java.util.Locale.ROOT);
            String message = widget.getMessage().getString().toLowerCase(java.util.Locale.ROOT);
            if (key.contains("essential_player") || message.contains("essential_player")) {
                preview = widget;
                continue;
            }
            String action = null;
            for (String candidate : new String[]{"host", "social", "wardrobe", "pictures", "settings", "account"}) {
                if ((key.contains("essential") && key.contains(candidate))
                        || (message.contains("essential") && message.contains(candidate))) {
                    action = candidate;
                    break;
                }
            }
            if (action != null) actions.putIfAbsent(action, widget);
        }
        if (preview == null || actions.size() != 6) return;

        String[] order = {"host", "social", "wardrobe", "pictures", "settings", "account"};
        int maxWidth = 0;
        int totalHeight = 0;
        final int gap = 3;
        for (String action : order) {
            AbstractWidget widget = actions.get(action);
            // Preserve Essential's native width and label/icon layout.
            // Its social notification badges may rely on these dimensions.
            maxWidth = Math.max(maxWidth, widget.getWidth());
            totalHeight += widget.getHeight();
        }
        totalHeight += gap * (order.length - 1);

        // Skip relocation rather than covering the preview or going offscreen.
        final int sideGap = 8;
        int x = preview.getX() - sideGap - maxWidth - 50;
        if (x < 4) return;
        int y = preview.getY() + (preview.getHeight() - totalHeight) / 2;
        // Three button slots downward, using the actual Essential widget height.
        y += 3 * (actions.get("host").getHeight() + gap) - 4;
        y = Math.max(4, Math.min(y, screen.height - totalHeight - 4));
        if (y < 4 || y + totalHeight > screen.height - 4) return;

        for (String action : order) {
            AbstractWidget widget = actions.get(action);
            widget.setX(x + maxWidth - widget.getWidth());
            widget.setY(y);
            y += widget.getHeight() + gap;
        }
    }

    /**
     * Position Essential's real "Update Available" widget exactly 6 GUI pixels
     * left of Mods, vertically centered. Never change its size or callbacks.
     */
    private static void positionEssentialUpdateLeftOfMods(
            net.minecraft.client.gui.screens.Screen screen, AbstractWidget mods) {
        if (mods == null) return;
        for (var child : screen.children()) {
            if (!(child instanceof AbstractWidget widget) || widget == mods) continue;
            String key = translationKey(widget).toLowerCase(java.util.Locale.ROOT);
            String label = widget.getMessage().getString().toLowerCase(java.util.Locale.ROOT);
            boolean isUpdate = (label.contains("update") && label.contains("available"))
                    || (key.contains("essential") && key.contains("update"));
            if (!isUpdate) continue;
            int x = mods.getX() - 6 - widget.getWidth();
            int y = mods.getY() + (mods.getHeight() - widget.getHeight()) / 2;
            if (x < 0 || y < 0 || y + widget.getHeight() > screen.height) return;
            widget.setX(x);
            widget.setY(y);
            return;
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
                if (menuMusicVolume > 0) {
                    musicInstance = SimpleSoundInstance.forUI(menuSound(), 1.0F, (float) menuMusicVolume);
                    mc.getSoundManager().play(musicInstance);
                }
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
        // Suppress only the six vanilla/NeoForge button visuals. Their actual
        // widgets remain alive and receive mouse/keyboard input normally.
        for (var child : event.getScreen().children()) {
            if (child instanceof AbstractWidget widget && isStyledTitleButton(widget)) {
                widget.setAlpha(0.0f);
            }
        }
        // DIAGNOSTIC ONLY: allow TitleScreen.render to execute normally.
        // Essential's own rendering hooks can now run. Vanilla panorama/logo/splash
        // WILL return for this test; this isolates the compatibility issue.
        // The centered widget positions from onInit remain active.
        return;
    }

    /** Paint only our six centered buttons after vanilla renders the screen. */
    @SubscribeEvent
    public static void renderStyledButtons(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        for (var child : event.getScreen().children()) {
            if (!(child instanceof AbstractWidget widget) || !isStyledTitleButton(widget)) continue;
            // Restore alpha for later screens and for all native input/tooltip logic.
            widget.setAlpha(1.0f);
            if (!widget.visible) continue;
            int x = widget.getX(), y = widget.getY();
            int width = widget.getWidth(), height = widget.getHeight();
            boolean hover = widget.isHoveredOrFocused();
            int fill = !widget.active ? 0xFF1A2D35 : hover ? 0xFF397180 : 0xFF173C4A;
            int rim = hover ? 0xFFFFDFA0 : 0xFFD4AD70;
            int inner = hover ? 0xFF75C8D0 : 0xFF487D88;
            // Render at a higher Z than vanilla widget text, so no native glyphs leak through.
            g.pose().pushPose();
            g.pose().translate(0.0f, 0.0f, 300.0f);
            g.fill(x, y, x + width, y + height, rim);
            g.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
            g.fill(x + 3, y + 3, x + width - 3, y + 4, inner);
            g.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, inner);
            int color = !widget.active ? 0xFF999E9C : hover ? 0xFFFFF5DA : 0xFFF0E9D8;
            g.drawCenteredString(mc.font, widget.getMessage(),
                    x + width / 2, y + (height - 8) / 2, color);
            g.pose().popPose();
        }
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

    /** Draw animated title foreground before vanilla and Essential render their widgets. */
    public static void drawTitleForeground(GuiGraphics g, int w, int h) {
        drawKelp(g, w, h);
        int duration = Math.max(140, w * 2);
        int pauseAt = (int) (duration * 0.82);
        int pauseLength = 95;
        int phase = (int) Math.floorMod(ticks, duration + pauseLength);
        boolean stare = phase >= pauseAt && phase < pauseAt + pauseLength;
        int travel = phase < pauseAt ? phase : phase < pauseAt + pauseLength ? pauseAt : phase - pauseLength;
        int x = -45 + (int) ((w + 110.0) * travel / duration);
        int y = h * 79 / 100 + (int) (3 * Math.sin(ticks * .06));
        float spriteScale = Math.max(0.65f, Math.min(1.6f, w / 600.0f));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(spriteScale, spriteScale, 1.0f);
        drawCluckles(g, 0, 0, stare);
        g.pose().popPose();
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
