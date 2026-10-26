package com.devaxo.templeoftheaxolotl.traps;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Connect these callbacks to the active maze's saved floor graph and status effect.
 * No guessed Y bands, no hardcoded maze origins, no changes to the generator. */
public final class TrapHooks {
    public interface MazeAccess {
        boolean isActiveMazePlayer(ServerPlayer player);
        /** Applies +1 He is Upset With You, with milk-resistant persistence. */
        void addCurse(ServerPlayer player);
        /** 50% normal roll; if forced, always teleport. Return false on floor one/no destination. */
        boolean teleportToPreviousFloorFurthestDeadEnd(ServerPlayer player, boolean forced);
        /** Nearest registered curse trap on SAME FLOOR; may be null. */
        BlockPos nearestCurseTrap(ServerPlayer player);
    }
    private static MazeAccess access;
    public static void install(MazeAccess implementation) { access = implementation; }
    public static boolean eligible(ServerPlayer p) {
        return !p.isCreative() && !p.isSpectator() && access != null && access.isActiveMazePlayer(p);
    }
    public static void curse(ServerPlayer p, boolean forceTeleport) {
        if (!eligible(p)) return;
        access.addCurse(p);
        access.teleportToPreviousFloorFurthestDeadEnd(p, forceTeleport);
    }
    public static void sneak(ServerPlayer p) {
        if (!eligible(p)) return;
        if (access.nearestCurseTrap(p) != null) curse(p, true);
    }
    private TrapHooks() {}
}
