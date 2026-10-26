package com.devaxo.templeoftheaxolotl.compass;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TempleNavigationClient {
    private static TempleNavigationPayload current = new TempleNavigationPayload(false, false, 0, 0, false, false, false, false);
    private TempleNavigationClient() {}
    public static void accept(TempleNavigationPayload payload) { current = payload; }
    public static TempleNavigationPayload current() { return current; }
}
