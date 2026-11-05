package com.devaxo.templeoftheaxolotl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static net.minecraft.commands.Commands.literal;

/** Local, persistent visual preference. Never touches water or boat physics. */
@EventBusSubscriber(modid = "temple_of_the_axolotl", value = Dist.CLIENT)
public final class CelestialWaterMode {
    private static final Path CONFIG = Path.of("config", "tota-water.properties");
    private static volatile boolean enabled = read();
    private CelestialWaterMode() {}
    public static boolean enabled() { return enabled; }

    private static boolean read() {
        Properties p = new Properties();
        try (var in = Files.newInputStream(CONFIG)) { p.load(in); }
        catch (IOException ignored) { return false; }
        return Boolean.parseBoolean(p.getProperty("celestialWater", "false"));
    }

    private static void set(boolean value) {
        enabled = value;
        Properties p = new Properties();
        p.setProperty("celestialWater", Boolean.toString(value));
        try {
            Files.createDirectories(CONFIG.getParent());
            try (var out = Files.newOutputStream(CONFIG)) { p.store(out, "Temple of the Axolotl client celestial water"); }
        } catch (IOException ignored) {}
        // Rebuild once to replace the water-only vertex marker in cached meshes.
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(literal("tota")
            .then(literal("celestialwater")
                .then(literal("enable").executes(ctx -> { set(true); say("Celestial water enabled"); return 1; }))
                .then(literal("disable").executes(ctx -> { set(false); say("Celestial water disabled"); return 1; }))
                .then(literal("status").executes(ctx -> { say("Celestial water: " + (enabled ? "enabled" : "disabled")); return 1; })))
            .then(literal("celestialskies")
                .then(literal("enable").executes(ctx -> { CelestialAuroraMode.set(true); say("Auroras enabled"); return 1; }))
                .then(literal("disable").executes(ctx -> { CelestialAuroraMode.set(false); say("Auroras disabled (moon, stars and clouds unchanged)"); return 1; }))
                .then(literal("status").executes(ctx -> { say("Auroras: " + (CelestialAuroraMode.enabled() ? "enabled" : "disabled")); return 1; }))));
    }
    private static void say(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("[TOTA] " + message), false);
    }
}
