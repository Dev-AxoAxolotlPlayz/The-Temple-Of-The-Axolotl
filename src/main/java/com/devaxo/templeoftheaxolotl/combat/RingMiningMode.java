package com.devaxo.templeoftheaxolotl.combat;

import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative, player-persistent Ring mode. Default: combat. */
public final class RingMiningMode {
    private static final String KEY = "temple_of_the_axolotl.ring_mining_mode";
    private RingMiningMode() {}
    public static boolean isMining(ServerPlayer player) {
        return player.getPersistentData().getBoolean(KEY);
    }
    public static void toggle(ServerPlayer player) {
        player.getPersistentData().putBoolean(KEY, !isMining(player));
        // Immediately stop accepting boomerang input when switching modes.
        RingBoomerangCombat.setHeld(player, false);
    }
}
