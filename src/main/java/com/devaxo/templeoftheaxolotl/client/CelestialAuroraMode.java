package com.devaxo.templeoftheaxolotl.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Persistent client-side aurora preference; does not affect the moon, stars or clouds. */
public final class CelestialAuroraMode {
    private static final Path CONFIG = Path.of("config", "tota-auroras.properties");
    private static volatile boolean enabled = read();
    private CelestialAuroraMode() {}
    public static boolean enabled() { return enabled; }
    private static boolean read() {
        Properties p = new Properties();
        try (var in = Files.newInputStream(CONFIG)) { p.load(in); }
        catch (IOException ignored) { return true; }
        return Boolean.parseBoolean(p.getProperty("auroras", "true"));
    }
    public static void set(boolean value) {
        enabled = value;
        Properties p = new Properties();
        p.setProperty("auroras", Boolean.toString(value));
        try {
            Files.createDirectories(CONFIG.getParent());
            try (var out = Files.newOutputStream(CONFIG)) { p.store(out, "Temple of the Axolotl client auroras"); }
        } catch (IOException ignored) {}
    }
}
