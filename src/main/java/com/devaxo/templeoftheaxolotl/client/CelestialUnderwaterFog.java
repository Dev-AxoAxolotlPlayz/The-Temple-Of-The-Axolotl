package com.devaxo.templeoftheaxolotl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Smooth camera-depth fog; Abyss ends its twilight at Y=45, above the typical Y=40 seabed. */
@EventBusSubscriber(modid="temple_of_the_axolotl",value=Dist.CLIENT)
public final class CelestialUnderwaterFog {
    private CelestialUnderwaterFog() {}
    private static boolean abyss() {
        Minecraft mc=Minecraft.getInstance();
        return mc.level!=null && mc.level.dimension().location().toString().equals("temple_of_the_axolotl:ethereal_abyss");
    }
    private static boolean applies(ViewportEvent event) {
        return Minecraft.getInstance().level!=null && event.getCamera().getFluidInCamera()==FogType.WATER
                && (abyss()||CelestialWaterMode.enabled());
    }
    private static float clamp(double t) {return (float)Math.max(0,Math.min(1,t));}
    private static float smooth(float t) {return t*t*(3-2*t);}
    private static float lerp(float a,float b,float t) {return a+(b-a)*t;}
    private static float depth(double y) {
        return smooth(clamp(abyss()?(200-y)/155.0:(62-y)/92.0));
    }
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event) {
        if(!applies(event))return;
        float d=depth(event.getCamera().getPosition().y);
        event.setRed(lerp(.12f,.004f,d));
        event.setGreen(lerp(.45f,.018f,d));
        event.setBlue(lerp(.56f,.048f,d));
    }
    @SubscribeEvent public static void fogDistance(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension() == Level.END
                && CelestialWaterMode.enabled()
                && event.getCamera().getFluidInCamera() == FogType.NONE) {
            // Disable ordinary End distance fog without touching terrain depth tests.
            // This affects End atmosphere, not fog in other dimensions or fluids.
            event.setNearPlaneDistance(-8.0f);
            event.setFarPlaneDistance(Math.max(1024.0f,
                    mc.options.renderDistance().get() * 16.0f + 128.0f));
            event.setCanceled(true);
            return;
        }
        if(!applies(event))return;
        float d=depth(event.getCamera().getPosition().y);
        // Shallow-water exploration remains readable; deep water closes in progressively.
        float far=lerp(abyss()?176f:144f,abyss()?22f:70f,d);
        event.setNearPlaneDistance(abyss()?0f:-8f);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }
}
