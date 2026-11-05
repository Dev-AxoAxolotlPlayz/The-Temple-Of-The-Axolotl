package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Persistent module switch. A handheld lantern always works regardless of this switch. */
public final class AbyssalLanternEvents {
    private static final String DISABLED = "tota_abyssal_lantern_disabled";
    private AbyssalLanternEvents() {}
    public static boolean moduleEnabled(ServerPlayer player) {
        return TempleHudModules.getSlot(player, 11).is(ModItems.ABYSSAL_LANTERN.get())
                && !player.getPersistentData().getBoolean(DISABLED);
    }
    public static void toggle(ServerPlayer player) {
        player.getPersistentData().putBoolean(DISABLED, !player.getPersistentData().getBoolean(DISABLED));
    }
    public static boolean held(Player player) {
        return player.getMainHandItem().is(ModItems.ABYSSAL_LANTERN.get())
                || player.getOffhandItem().is(ModItems.ABYSSAL_LANTERN.get());
    }
}
