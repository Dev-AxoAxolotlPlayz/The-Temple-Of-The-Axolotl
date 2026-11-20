package com.devaxo.templeoftheaxolotl.client.renderer;

import net.minecraft.world.entity.LivingEntity;

/** Render-thread context for the entity whose hand item is currently being drawn. */
public final class TridentChargeRenderContext {
    private TridentChargeRenderContext() {}
    private static final ThreadLocal<LivingEntity> ACTIVE = new ThreadLocal<>();
    public static void set(LivingEntity entity) { ACTIVE.set(entity); }
    public static LivingEntity get() { return ACTIVE.get(); }
    public static void clear() { ACTIVE.remove(); }
}
