package com.devaxo.templeoftheaxolotl.event;

import com.devaxo.templeoftheaxolotl.maze.TempleDimensionManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Shared pressure depth scale for the Overworld, Ethereal Abyss and Origin Temple.
 * The Temple uses the Abyss' Y=200 to Y=0 depth scale.
 * Submersion checks remain in the calling event handlers.
 */
public final class TemplePressureScale {
    private TemplePressureScale() {}

    public static boolean usesAbyssScale(Level level) {
        // Keep the established 6.6 behavior for other non-Overworld dimensions,
        // including the Ethereal Abyss, while explicitly supporting the Temple.
        return level.dimension().equals(TempleDimensionManager.TEMPLE_DIMENSION)
                || !level.dimension().equals(Level.OVERWORLD);
    }

    public static double surface(Level level) {
        return usesAbyssScale(level) ? 200.0 : 62.0;
    }

    public static double depthRange(Level level) {
        return usesAbyssScale(level) ? 200.0 : 92.0;
    }

    public static double pressure(Player player) {
        Level level = player.level();
        return Math.max(0.0, Math.min(100.0,
                (surface(level) - player.getEyeY()) * 100.0 / depthRange(level)));
    }
}
