package com.devaxo.templeoftheaxolotl.water;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.Level;

/** Main-tick-thread only: avoids repeating 25-column depth scans for boats. */
public final class WaterDepthCache {
    private static final int LIMIT = 4096;
    private static Level levelIdentity;
    private static long tick = Long.MIN_VALUE;
    private static final Map<Key, Double> values = new HashMap<>();
    private WaterDepthCache() {}

    public static double sample(Level level, int x, int y, int z) {
        if (level == null) return 0;
        if (levelIdentity != level || tick != level.getGameTime()) {
            levelIdentity = level;
            tick = level.getGameTime();
            values.clear();
        }
        Key key = new Key(x, y, z);
        Double cached = values.get(key);
        if (cached != null) return cached;
        double result = WaterDepthSampler.smoothedDepth(level, x, y, z);
        if (values.size() >= LIMIT) values.clear();
        values.put(key, result);
        return result;
    }
    private record Key(int x, int y, int z) {}
}
