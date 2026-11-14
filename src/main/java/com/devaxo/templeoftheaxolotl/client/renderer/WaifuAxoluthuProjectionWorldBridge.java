package com.devaxo.templeoftheaxolotl.client.renderer;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client-side rendering bridge. Server blueprint data must be delivered by a packet handler.
 * No authority over placement, permits or project state is granted to the client.
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class WaifuAxoluthuProjectionWorldBridge {
    private WaifuAxoluthuProjectionWorldBridge() {}
    private record Projection(ResourceKey<Level> dimension, CompoundTag manifest) {}
    private static final Map<UUID, Projection> ACTIVE = new HashMap<>();
    private static Level lastLevel;
    private static boolean detailKeyDown;

    /** Called on the client main thread by a future server-to-client sync packet. */
    public static void update(UUID project, ResourceKey<Level> dimension, CompoundTag manifest) {
        if (manifest == null || !manifest.contains("blocks", 9)) {
            ACTIVE.remove(project);
            return;
        }
        WaifuAxoluthuCelestialProjection.clearCache();
        ACTIVE.put(project, new Projection(dimension, manifest.copy()));
    }

    public static void remove(UUID project) { ACTIVE.remove(project); WaifuAxoluthuCelestialProjection.clearCache(); }
    public static void clear() { ACTIVE.clear(); WaifuAxoluthuCelestialProjection.clearCache(); }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { clear(); lastLevel = null; return; }
        if (mc.level != lastLevel) { clear(); lastLevel = mc.level; }
        if (ACTIVE.isEmpty()) return;
        // F8: cycle exterior / full / ground-floor inspection.
        // Edge detection prevents repeating while the key is held.
        boolean keyDown = org.lwjgl.glfw.GLFW.glfwGetKey(
                mc.getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_F8)
                == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (keyDown && !detailKeyDown) {
            WaifuAxoluthuCelestialProjection.cycleDetail();
            if (mc.player != null) mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "Celestial Blueprint view: " +
                        WaifuAxoluthuCelestialProjection.detail().name()), true);
        }
        detailKeyDown = keyDown;
        PoseStack pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        for (Projection p : ACTIVE.values()) {
            if (!p.dimension.equals(mc.level.dimension())) continue;
            WaifuAxoluthuCelestialProjection.render(p.manifest, mc.level, pose, buffers, camera,
                    event.getPartialTick().getGameTimeDeltaPartialTick(false), mc.level.getGameTime());
        }
        buffers.endBatch(net.minecraft.client.renderer.RenderType.debugFilledBox());
        buffers.endBatch(net.minecraft.client.renderer.RenderType.lines());
    }
}
