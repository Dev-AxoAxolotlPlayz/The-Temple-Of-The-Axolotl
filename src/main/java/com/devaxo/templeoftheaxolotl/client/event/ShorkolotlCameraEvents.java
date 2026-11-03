package com.devaxo.templeoftheaxolotl.client.event;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Switch only the local pilot to third person; restore their previous view on exit. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class ShorkolotlCameraEvents {
    private static CameraType previousCamera;
    private static boolean wasPiloting;
    private static int lastVehicleId = Integer.MIN_VALUE;
    private static float lastVehicleYaw;

    private ShorkolotlCameraEvents() {}

    /**
     * Keep yaw handling in onClientTick; allow the pilot's own vertical
     * mouse look to determine camera pitch, not the vehicle's zero pitch.
     */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof Shorkolotl100Entity) {
            event.setPitch(mc.player.getXRot());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            previousCamera = null;
            wasPiloting = false;
            lastVehicleId = Integer.MIN_VALUE;
            return;
        }
        boolean piloting = mc.player.getVehicle() instanceof Shorkolotl100Entity;
        if (piloting && !wasPiloting) {
            previousCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!piloting && wasPiloting) {
            if (previousCamera != null) mc.options.setCameraType(previousCamera);
            previousCamera = null;
            lastVehicleId = Integer.MIN_VALUE;
        }
        if (piloting && mc.player.getVehicle() instanceof Shorkolotl100Entity sub) {
            // Follow hull yaw when A/D steer, but retain mouse free-look relative to it.
            // Track entity ID so mounting does not produce a large camera jump.
            if (lastVehicleId == sub.getId()) {
                float hullDelta = Mth.wrapDegrees(sub.getYRot() - lastVehicleYaw);
                if (Math.abs(hullDelta) > 0.001F) {
                    mc.player.setYRot(mc.player.getYRot() + hullDelta);
                    mc.player.setYHeadRot(mc.player.getYRot());
                }
            }
            lastVehicleId = sub.getId();
            lastVehicleYaw = sub.getYRot();
            // 220-degree horizontal view arc: +/-110 from the submarine's heading.
            float relative = Mth.wrapDegrees(mc.player.getYRot() - sub.getYRot());
            float clamped = Mth.clamp(relative, -110.0F, 110.0F);
            if (Math.abs(relative - clamped) > 0.001F) {
                float yaw = sub.getYRot() + clamped;
                mc.player.setYRot(yaw);
                mc.player.setYHeadRot(yaw);
            }
        }
        wasPiloting = piloting;
    }
}
