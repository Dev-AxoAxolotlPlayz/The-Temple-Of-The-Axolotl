package com.devaxo.templeoftheaxolotl.water;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.vehicle.Boat;

/** Optional visual state, accessible to a future verified BoatRenderer hook. */
public final class BoatWaveState {
    private static final Map<Boat, Orientation> STATES = new WeakHashMap<>();
    private BoatWaveState() {}
    public record Orientation(float pitch, float roll, long gameTick) {}
    public static void update(Boat boat, double pitch, double roll) {
        STATES.put(boat, new Orientation(
            (float)Math.max(-15, Math.min(15, pitch)),
            (float)Math.max(-15, Math.min(15, roll)),
            boat.level().getGameTime()));
    }
    public static Orientation get(Boat boat) { return STATES.get(boat); }
    public static void clear(Boat boat) { STATES.remove(boat); }
}
