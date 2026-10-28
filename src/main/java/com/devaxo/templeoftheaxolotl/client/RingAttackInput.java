package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.network.RingAttackInputPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class RingAttackInput {
    private static boolean miningMode = false;
    private static boolean lastHeld;
    private static int refreshTicks;
    private RingAttackInput() {}

    public static void setMiningMode(boolean enabled) { miningMode = enabled; }

    public static boolean physicalMiningHeld(Minecraft mc) {
        return mc.screen == null && mc.isWindowActive() && physicalAttackDown(mc);
    }

    private static boolean physicalAttackDown(Minecraft mc) {
        InputConstants.Key key = mc.options.keyAttack.getKey();
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        if (key.getType() == InputConstants.Type.KEYSYM)
            return InputConstants.isKeyDown(window, key.getValue());
        return mc.options.keyAttack.isDown();
    }

    private static boolean ringCombat(Minecraft mc) {
        return !miningMode && mc.player != null
                && mc.player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get());
    }

    @SubscribeEvent
    public static void beforeTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        // Prevent the vanilla destroy-block loop from treating combat throws as mining.
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        if (!mc.player.getMainHandItem().is(ModItems.RING_OF_THE_ONE.get())) return;
        if (miningMode) {
            // Restore the actual held button BEFORE vanilla processes block damage.
            mc.options.keyAttack.setDown(physicalAttackDown(mc));
        } else {
            mc.options.keyAttack.setDown(false);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean held = mc.player != null && mc.level != null && mc.screen == null
                && mc.isWindowActive() && ringCombat(mc) && physicalAttackDown(mc);
        if (held != lastHeld || (held && ++refreshTicks >= 10)) {
            if (mc.getConnection() != null) PacketDistributor.sendToServer(new RingAttackInputPayload(held));
            lastHeld = held;
            refreshTicks = 0;
        }
    }
}
