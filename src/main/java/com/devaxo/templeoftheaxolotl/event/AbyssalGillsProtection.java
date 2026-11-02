package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** One shared server-authoritative check for the 75%-rated Abyssal Gills module. */
public final class AbyssalGillsProtection {
    private static final String DISABLED = "tota_abyssal_gills_disabled";
    private AbyssalGillsProtection() {}
    public static boolean enabled(Player player) { return !player.getPersistentData().getBoolean(DISABLED); }
    public static void toggle(ServerPlayer player) {
        player.getPersistentData().putBoolean(DISABLED, enabled(player));
    }
    public static boolean installed(Player player) {
        return player instanceof ServerPlayer server &&
                TempleHudModules.getSlot(server, 10).is(ModItems.ABYSSAL_GILLS.get());
    }
    public static double pressure(Player player) {
        return TemplePressureScale.pressure(player);
    }
    public static boolean protects(Player player, double pressure) {
        return installed(player) && enabled(player) && pressure <= 75.0;
    }
}
