package com.devaxo.templeoftheaxolotl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Phase 6.4.9: camera-submerged atmosphere. No blocks, fluids, boats,
 * shaders or gameplay effects are changed here.
 *
 * Fog colors use the same depth ratios as Celestial Water:
 * Overworld 62..-30 (92 blocks); Ethereal Abyss 200..0 (200 blocks).
 */
@EventBusSubscriber(modid = "temple_of_the_axolotl", value = Dist.CLIENT)
public final class CelestialUnderwaterFog {
    private CelestialUnderwaterFog() {}

    private static float depthFraction(double cameraY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0.0f;
        // Provisional Abyss dimension detection until its registry key is finalized.
        boolean abyss = mc.level.dimension().location().getPath().contains("abyss");
        double sea = abyss ? 200.0 : 62.0;
        double range = abyss ? 200.0 : 92.0;
        return (float)Math.max(0.0, Math.min(1.0, (sea - cameraY) / range));
    }

    private static boolean applies(ViewportEvent event) {
        Minecraft mc = Minecraft.getInstance();
        return CelestialWaterMode.enabled()
                && mc.level != null
                && event.getCamera().getFluidInCamera() == FogType.WATER;
    }

    private static float smooth(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @SubscribeEvent
    public static void color(ViewportEvent.ComputeFogColor event) {
        if (!applies(event)) return;
        float d = smooth(depthFraction(event.getCamera().getPosition().y));
        // RGB in linear-ish shader color units. Surface: cool clear teal-blue;
        // deep: dark cosmic indigo. Nearby world geometry stays visible.
        event.setRed(lerp(0.075f, 0.012f, d));
        event.setGreen(lerp(0.31f, 0.022f, d));
        event.setBlue(lerp(0.43f, 0.065f, d));
    }

    @SubscribeEvent
    public static void fogDistance(ViewportEvent.RenderFog event) {
        if (!applies(event)) return;
        float d = smooth(depthFraction(event.getCamera().getPosition().y));
        // Conduit-like visibility: long sightlines at the surface, still
        // navigable at the deepest point. Fog never increases water opacity.
        float far = lerp(144.0f, 88.0f, d);
        event.setNearPlaneDistance(-8.0f);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }
}
