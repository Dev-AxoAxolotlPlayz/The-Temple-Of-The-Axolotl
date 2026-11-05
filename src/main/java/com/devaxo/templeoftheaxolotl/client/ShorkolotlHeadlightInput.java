package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import com.devaxo.templeoftheaxolotl.network.ShorkolotlHeadlightPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** H toggles the headlights while piloting; ignores held-key repeats. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class ShorkolotlHeadlightInput {
    private static boolean previouslyDown;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.player.getVehicle() == null) {
            previouslyDown = false; return;
        }
        boolean down = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_H);
        if (down && !previouslyDown && mc.player.getVehicle() instanceof Shorkolotl100Entity)
            PacketDistributor.sendToServer(new ShorkolotlHeadlightPayload());
        previouslyDown = down;
    }
}
