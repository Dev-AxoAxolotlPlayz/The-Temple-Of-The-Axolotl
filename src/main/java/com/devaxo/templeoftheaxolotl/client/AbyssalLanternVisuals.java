package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.event.AbyssalLanternEvents;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import com.devaxo.templeoftheaxolotl.entity.BossWrathEntity;

/** Soft cyan ambient tint near the camera; intentionally does not alter world blocks. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class AbyssalLanternVisuals {
    private AbyssalLanternVisuals() {}
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,
                    "textures/misc/abyssal_lantern_glow.png");
    private static boolean moduleActive;
    public static void updateModule(boolean active) { moduleActive = active; }
    /**
     * Particle-free local cyan halo, rendered as a camera-facing sprite.
     * The vanilla light block remains responsible for actual illumination.
     */
    @SubscribeEvent
    public static void glow(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean active = AbyssalLanternEvents.held(event.getEntity())
                || (event.getEntity() == mc.player && moduleActive);
        if (!active) return;
        double phase = (mc.level.getGameTime() % 160) / 20.0;
        double fraction = phase < 1 ? 0 : phase < 4 ? smooth((phase - 1) / 3)
                : phase < 5 ? 1 : 1 - smooth((phase - 5) / 3);
        int alpha = (int)(50 + 75 * fraction);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, 1.05, 0);
        Quaternionf cameraRotation = new Quaternionf(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.mulPose(cameraRotation);
        float radius = 0.75f + (float)fraction * 0.18f;
        VertexConsumer vertices = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(GLOW));
        var matrix = pose.last().pose();
        var normal = pose.last();
        vertices.addVertex(matrix, -radius, -radius, 0).setColor(60, 235, 245, alpha)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(normal, 0, 0, 1);
        vertices.addVertex(matrix, radius, -radius, 0).setColor(60, 235, 245, alpha)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(normal, 0, 0, 1);
        vertices.addVertex(matrix, radius, radius, 0).setColor(60, 235, 245, alpha)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(normal, 0, 0, 1);
        vertices.addVertex(matrix, -radius, radius, 0).setColor(60, 235, 245, alpha)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(normal, 0, 0, 1);
        pose.popPose();
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { moduleActive = false; return; }
        if (!AbyssalLanternEvents.held(mc.player) && !moduleActive) return;
        // Preserve the Second Fall crimson atmosphere.
        if (!mc.level.getEntitiesOfClass(BossWrathEntity.class,
                mc.player.getBoundingBox().inflate(96), BossWrathEntity::isSecondFall).isEmpty()) return;
        double cycle = (mc.level.getGameTime() % 160) / 20.0;
        double fraction;
        if (cycle < 1) fraction = 0;
        else if (cycle < 4) fraction = smooth((cycle - 1) / 3);
        else if (cycle < 5) fraction = 1;
        else fraction = 1 - smooth((cycle - 5) / 3);
        boolean water = mc.player.isUnderWater();
        double light = (water ? 13 : 10) + fraction * (water ? 2 : 3);
        float strength = (float) (0.045 + (light - 10) * 0.012);
        event.setRed(event.getRed() * (1 - strength) + 0.18f * strength);
        event.setGreen(event.getGreen() * (1 - strength) + 0.92f * strength);
        event.setBlue(event.getBlue() * (1 - strength) + 0.98f * strength);
    }
    private static double smooth(double x) { return x*x*(3-2*x); }
}
