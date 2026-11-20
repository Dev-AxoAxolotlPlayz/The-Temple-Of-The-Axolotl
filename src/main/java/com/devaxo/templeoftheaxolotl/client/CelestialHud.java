
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
        if (!boots && !chestplate && curse == null && !navigation.equipped() && !navigation.compass() && !navigation.clock() && !navigation.pressureGauge() && navigation.oxygenTier()==0) {
            return;
        }

        var graphics = event.getGuiGraphics();

        boolean showGauge = navigation.pressureGauge() && !com.devaxo.templeoftheaxolotl.event.TemplePressureScale.exempt(player.level());
        // Ability status is independent of the information sidebar.
        if (!player.getAbilities().instabuild && !player.isSpectator() && !player.isPassenger()) {
            int abilityX = graphics.guiWidth() / 2 + 95;
            int abilityY = graphics.guiHeight() - 7 - minecraft.font.lineHeight;
            int abilityWidth = minecraft.font.width("Eternal Guard: equipped");
            // Narrow screens: keep text on screen, above the hotbar if necessary.
            if (abilityX + abilityWidth > graphics.guiWidth() - 4) {
                abilityX = Math.max(4, graphics.guiWidth() - abilityWidth - 4);
                abilityY = graphics.guiHeight() - 49;
                if (player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply()) abilityY -= 10;
            }
            if (chestplate) {
                graphics.drawString(minecraft.font, "Eternal Guard: equipped", abilityX, abilityY, 0xFFFFD4A5, true);
                abilityY -= 10;
            }
            if (boots) {
                int max = CelestialDoubleJumpInput.hudMaxAirJumps();
                int remaining = Math.max(0, max - CelestialDoubleJumpInput.hudUsedAirJumps());
                graphics.drawString(minecraft.font, "Pounce: " + remaining + "/" + max, abilityX, abilityY, 0xFFB6E8FF, true);
            }
        }
        record HudLine(String text, int color) {}
        var lines = new java.util.ArrayList<HudLine>();
        if (curse != null) lines.add(new HudLine("RNGesus Curse: " + (curse.getAmplifier() + 1), 0xFFFF9CA7));
        if (navigation.equipped()) {
            lines.add(new HudLine(navigation.heading(), 0xFFE1C5FF));
            if (navigation.arrived()) {
                lines.add(new HudLine("Temple Arrived.", 0xFF9BE5DC));
                lines.add(new HudLine("The Axolotl Absolute Watches", 0xFFE1C5FF));
            } else if (!navigation.valid() || navigation.unavailable()) {
                lines.add(new HudLine(navigation.label().isEmpty() ? "No target" : navigation.label(), 0xFF9BE5DC));
            } else {
                if (!navigation.label().isEmpty()) lines.add(new HudLine(navigation.label(), 0xFF9BE5DC));
                double dx = navigation.x() + 0.5 - player.getX();
                double dz = navigation.z() + 0.5 - player.getZ();
                long distance = Math.round(Math.hypot(dx, dz));
                double relative = Math.toRadians(Math.toDegrees(Math.atan2(-dx, dz)) - player.getYRot());
                String arrow = Math.cos(relative) > 0.707 ? "^" : Math.cos(relative) < -0.707 ? "v" : Math.sin(relative) > 0 ? "<" : ">";
                lines.add(new HudLine("Compass: " + arrow + "  " + distance + " blocks", 0xFF9BE5DC));
                lines.add(new HudLine("X: " + navigation.x() + "  Z: " + navigation.z(), 0xFFD3D9F1));
            }
        }
        if (navigation.compass()) {
            lines.add(new HudLine("POSITION", 0xFFE1C5FF));
            lines.add(new HudLine("X: " + player.blockPosition().getX() + " Y: " + player.blockPosition().getY() + " Z: " + player.blockPosition().getZ(), 0xFFD3D9F1));
            String biome = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location().getPath().replace('_', ' ')).orElse("Unknown");
            lines.add(new HudLine(biome, 0xFF9BE5DC));
            lines.add(new HudLine("Facing: " + player.getDirection().getName().toUpperCase(java.util.Locale.ROOT), 0xFF9BE5DC));
        }
        if (navigation.clock()) {
            long dayTime = player.level().getDayTime();
            long day = Math.floorDiv(dayTime, 24000L) + 1;
            long ticks = Math.floorMod(dayTime, 24000L);
            int minutes = (int)((ticks * 1440L / 24000L + 360) % 1440);
            int hour24 = minutes / 60;
            int hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
            String time = String.format(java.util.Locale.ROOT, "%02d:%02d %s", hour12, minutes % 60, hour24 < 12 ? "AM" : "PM");
            lines.add(new HudLine("CHRONOMETER", 0xFFE1C5FF));
            lines.add(new HudLine("Day " + day + "  |  " + time, 0xFFF5D797));
        }
        boolean tank= navigation.oxygenTier()>0;
        boolean instruments=showGauge||tank;
        int minHalf=tank?166:132;
        if (instruments && graphics.guiWidth() / 2 >= minHalf) {
            boolean submerged = player.isUnderWater();
            double pressure = submerged ? com.devaxo.templeoftheaxolotl.event.TemplePressureScale.pressure(player) : 0.0;
            if(showGauge) drawPressureDial(graphics, minecraft, graphics.guiWidth() / 2 - 113, graphics.guiHeight() - 35, pressure, submerged);
            if(tank) drawTank(graphics,minecraft,graphics.guiWidth()/2-151,graphics.guiHeight()-35,navigation.oxygenTier(),navigation.oxygenPercent());
        }
        boolean gaugeInSidebar = instruments && graphics.guiWidth() / 2 < minHalf;
        if (lines.isEmpty() && !gaugeInSidebar) return;
        int contentWidth = gaugeInSidebar ? (tank?71:33) : 0;
        for (HudLine entry : lines) contentWidth = Math.max(contentWidth, minecraft.font.width(entry.text()));
        int width = Math.min(contentWidth + 10, Math.max(44, graphics.guiWidth() - offsetX - 5));
        int height = 8 + lines.size() * 12 + (gaugeInSidebar ? 48 : 0);
        int x = right ? graphics.guiWidth() - width - offsetX : offsetX;
        int y = offsetY;
        graphics.fill(x, y, x + width, y + height, 0x990B0918);
        graphics.fill(x, y, x + width, y + 1, 0xFFAD77DA);
        int line = y + 4;
        for (HudLine entry : lines) {
            graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(entry.text(), width - 10), x + 5, line, entry.color(), false);
            line += 12;
        }
        if (gaugeInSidebar) {
            boolean submerged = player.isUnderWater();
            if(tank) drawTank(graphics,minecraft,x+21,line+17,navigation.oxygenTier(),navigation.oxygenPercent());
            if(showGauge) drawPressureDial(graphics, minecraft, x + (tank?59:21), line + 17, submerged ? com.devaxo.templeoftheaxolotl.event.TemplePressureScale.pressure(player) : 0.0, submerged);
        }

    }
    private static void drawTank(net.minecraft.client.gui.GuiGraphics g,Minecraft mc,int cx,int cy,int tier,int percent){
        int outline=0xFF8CB6C8; int color=switch(tier){case 2->0xFFD3D3D3;case 3->0xFFE8BC38;case 4->0xFF46D8DE;case 5->0xFF51434F;default->0xFF647D91;};
        g.fill(cx-6,cy-16,cx+7,cy+10,outline);g.fill(cx-5,cy-15,cx+6,cy+9,0xFF101C32);
        int fill=Math.round(24*Math.max(0,Math.min(100,percent))/100f);
        if(fill>0)g.fill(cx-5,cy+9-fill,cx+6,cy+9,0xFF0054EE);
        g.fill(cx-10,cy+10,cx+11,cy+17,outline);g.fill(cx-9,cy+11,cx+10,cy+16,color);
        String label=percent+"%";g.drawString(mc.font,label,cx-mc.font.width(label)/2,cy+19,0xFF5BF5FF,false);
    }
    private static void drawPressureDial(net.minecraft.client.gui.GuiGraphics g, Minecraft mc,
                                         int cx, int cy, double pressure, boolean submerged) {
        // Pixel-drawn analog instrument: no font glyphs or external image dependencies.
        for (int dy = -16; dy <= 16; dy++) for (int dx = -16; dx <= 16; dx++) {
            int d = dx * dx + dy * dy;
            int color = d <= 14 * 14 ? 0xFF101C32 : d <= 16 * 16 ? 0xFF8CB6C8 : 0;
            if (color != 0) g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
        // The dial sweeps 270 degrees from 0 (lower-left) to 100 (lower-right).
        for (int n = 0; n <= 20; n++) {
            double a = Math.toRadians(135.0 + 270.0 * n / 20.0);
            int r1 = n % 4 == 0 ? 11 : 13;
            drawPixelLine(g, cx + (int)Math.round(Math.cos(a) * r1), cy + (int)Math.round(Math.sin(a) * r1),
                    cx + (int)Math.round(Math.cos(a) * 14), cy + (int)Math.round(Math.sin(a) * 14), 0xFFBCEBFF);
        }
        double angle = Math.toRadians(135.0 + 270.0 * pressure / 100.0);
        drawPixelLine(g, cx, cy, cx + (int)Math.round(Math.cos(angle) * 11),
                cy + (int)Math.round(Math.sin(angle) * 11), 0xFF51F5FF);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFE8FFFF);
        String readout = String.format(java.util.Locale.ROOT, "%03d", (int)Math.round(pressure));
        g.drawString(mc.font, readout, cx - mc.font.width(readout)/2, cy + 19,
                submerged ? 0xFF5BF5FF : 0xFF99AAB9, false);
    }

    private static void drawPixelLine(net.minecraft.client.gui.GuiGraphics g, int x0, int y0,
                                      int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err * 2;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

}
