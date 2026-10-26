
package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.effect.ModEffects;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.compass.TempleNavigationClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.EquipmentSlot;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Client-only Celestial HUD.
 * Gameplay abilities remain server-authoritative.
 */
@EventBusSubscriber(
        modid = TempleOfTheAxolotl.MODID,
        value = Dist.CLIENT
)
public final class CelestialHud {

    private static final Path SETTINGS =
            FMLPaths.CONFIGDIR.get()
                    .resolve("temple_of_the_axolotl_hud.properties");

    private static boolean enabled = true;
    private static boolean right = false;

    private static int offsetX = 8;
    private static int offsetY = 8;

    private static boolean initialized = false;

    private CelestialHud() {}

    private static void load() {
        if (initialized) return;
        initialized = true;

        Properties properties = new Properties();

        try (var input = Files.newInputStream(SETTINGS)) {
            properties.load(input);
        } catch (Exception ignored) {
            return;
        }

        enabled = Boolean.parseBoolean(
                properties.getProperty("enabled", "true")
        );

        right = Boolean.parseBoolean(
                properties.getProperty("right", "false")
        );

        try {
            offsetX = Math.max(
                    0,
                    Math.min(
                            200,
                            Integer.parseInt(
                                    properties.getProperty("x", "8")
                            )
                    )
            );
        } catch (NumberFormatException ignored) {}

        try {
            offsetY = Math.max(
                    0,
                    Math.min(
                            200,
                            Integer.parseInt(
                                    properties.getProperty("y", "8")
                            )
                    )
            );
        } catch (NumberFormatException ignored) {}
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        load();

        Minecraft minecraft = Minecraft.getInstance();

        if (!enabled
                || minecraft.player == null
                || minecraft.options.hideGui
                || (minecraft.screen != null
                    && !(minecraft.screen instanceof ChatScreen)
                    && !(minecraft.screen instanceof PauseScreen)
                    && !(minecraft.screen instanceof ContainerScreen
                         && minecraft.screen.getTitle().getString().startsWith("Celestial Modules")))) {
            return;
        }

        var player = minecraft.player;

        boolean boots = player
                .getItemBySlot(EquipmentSlot.FEET)
                .is(ModItems.AXOLOTL_BOOTS.get());

        boolean chestplate = player
                .getItemBySlot(EquipmentSlot.CHEST)
                .is(ModItems.AXOLOTL_CHESTPLATE.get());

        var curse = player.getEffect(ModEffects.RNGESUS_CURSE);

        var navigation = TempleNavigationClient.current();
        if (!boots && !chestplate && curse == null && !navigation.equipped() && !navigation.compass() && !navigation.clock()) {
            return;
        }

        var graphics = event.getGuiGraphics();

        int width = 190;

        int height = 12
                + (boots ? 12 : 0)
                + (chestplate ? 12 : 0)
                + (curse != null ? 12 : 0)
                + (navigation.equipped() ? 44 : 0)
                + (navigation.compass() ? 36 : 0)
                + (navigation.clock() ? 24 : 0);

        int x = right
                ? graphics.guiWidth() - width - offsetX
                : offsetX;

        int y = offsetY;

        graphics.fill(
                x, y,
                x + width, y + height,
                0x990B0918
        );

        graphics.fill(
                x, y,
                x + width, y + 1,
                0xFFAD77DA
        );

        int line = y + 4;

        graphics.drawString(
                minecraft.font,
                "CELESTIAL",
                x + 5,
                line,
                0xFFE1C5FF,
                false
        );

        line += 12;

        if (boots) {
            int max = CelestialDoubleJumpInput.hudMaxAirJumps();

            int remaining = Math.max(
                    0,
                    max - CelestialDoubleJumpInput.hudUsedAirJumps()
            );

            graphics.drawString(
                    minecraft.font,
                    "Pounce: " + remaining + "/" + max,
                    x + 5,
                    line,
                    0xFFB6E8FF,
                    false
            );

            line += 12;
        }

        if (chestplate) {
            graphics.drawString(
                    minecraft.font,
                    "Eternal Guard: equipped",
                    x + 5,
                    line,
                    0xFFFFD4A5,
                    false
            );

            line += 12;
        }

        if (curse != null) {
            graphics.drawString(
                    minecraft.font,
                    "RNGesus Curse: " + (curse.getAmplifier() + 1),
                    x + 5,
                    line,
                    0xFFFF9CA7,
                    false
            );
            line += 12;
        }
        if (navigation.equipped()) {
            graphics.drawString(minecraft.font, "TEMPLE NAVIGATION", x + 5, line, 0xFFE1C5FF, false);
            line += 12;
            if (navigation.arrived()) {
                graphics.drawString(minecraft.font, "Temple Arrived.", x + 5, line, 0xFF9BE5DC, false);
                line += 12;
                graphics.drawString(minecraft.font, "The Axolotl Absolute Watches", x + 5, line, 0xFFE1C5FF, false);
            } else if (navigation.unavailable()) {
                graphics.drawString(minecraft.font, "Target unavailable - right-click seeker", x + 5, line, 0xFFFFB5A5, false);
            } else if (navigation.valid()) {
                double dx = navigation.x() + 0.5 - player.getX();
                double dz = navigation.z() + 0.5 - player.getZ();
                long distance = Math.round(Math.hypot(dx, dz));
                // Minecraft yaw: 0 faces south (+Z), 90 faces west (-X).
                double targetAngle = Math.toDegrees(Math.atan2(-dx, dz));
                double relative = Math.toRadians(targetAngle - player.getYRot());

                String arrow = Math.cos(relative) > 0.707 ? "^" :
                        Math.cos(relative) < -0.707 ? "v" :
                                Math.sin(relative) > 0 ? "<" : ">";

                graphics.drawString(minecraft.font, "Compass: " + arrow + "  " + distance + " blocks", x + 5, line, 0xFF9BE5DC, false);
                line += 12;
                graphics.drawString(minecraft.font, "X: " + navigation.x() + "  Z: " + navigation.z(), x + 5, line, 0xFFD3D9F1, false);
            } else {
                graphics.drawString(minecraft.font, "No temple target / wrong dimension", x + 5, line, 0xFFFFB5A5, false);
            }
        }
        if (navigation.compass()) {
            line += 14;
            graphics.drawString(minecraft.font, "POSITION", x + 5, line, 0xFFE1C5FF, false);
            line += 12;
            graphics.drawString(minecraft.font, "X: " + player.blockPosition().getX() +
                    " Y: " + player.blockPosition().getY() + " Z: " + player.blockPosition().getZ(),
                    x + 5, line, 0xFFD3D9F1, false);
            line += 12;
            String biome = player.level().getBiome(player.blockPosition()).unwrapKey()
                    .map(key -> key.location().getPath().replace('_', ' ')).orElse("Unknown");
            graphics.drawString(minecraft.font, biome + " | " + player.getDirection().getName().toUpperCase(),
                    x + 5, line, 0xFF9BE5DC, false);
        }
        if (navigation.clock()) {
            line += 14;
            long dayTime = player.level().getDayTime();
            long day = Math.floorDiv(dayTime, 24000L) + 1;
            long ticks = Math.floorMod(dayTime, 24000L);
            int minutes = (int)((ticks * 1440L / 24000L + 360) % 1440);
            int hour24 = minutes / 60;
            int hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
            String time = String.format(java.util.Locale.ROOT, "%02d:%02d %s",
                    hour12, minutes % 60, hour24 < 12 ? "AM" : "PM");
            graphics.drawString(minecraft.font, "CHRONOMETER", x + 5, line, 0xFFE1C5FF, false);
            line += 12;
            graphics.drawString(minecraft.font, "Day " + day + "  |  " + time,
                    x + 5, line, 0xFFF5D797, false);
        }
    }
}
