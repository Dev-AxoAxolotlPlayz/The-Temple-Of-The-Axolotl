package com.devaxo.templeoftheaxolotl.client;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import com.devaxo.templeoftheaxolotl.network.ShorkolotlPilotInputPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Space ascends; configurable V descends. Camera pitch is not propulsion. */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID, value = Dist.CLIENT)
public final class ShorkolotlPilotInput {
    public static final KeyMapping ASCEND = new KeyMapping(
            "key.temple_of_the_axolotl.shorkolotl_ascend",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE,
            "key.categories.temple_of_the_axolotl");
    public static final KeyMapping DESCEND = new KeyMapping(
            "key.temple_of_the_axolotl.shorkolotl_descend",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
            "key.categories.temple_of_the_axolotl");
    private static boolean lastAscending;
    private static boolean lastDescending;
    private static boolean wasPiloting;
    private static int refresh;
    private ShorkolotlPilotInput() {}

    public static final class Registration {
        private Registration() {}
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(ASCEND);
            event.register(DESCEND);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean piloting = mc.player != null
                && mc.player.getVehicle() instanceof Shorkolotl100Entity;
        boolean ascending = piloting && ASCEND.isDown()
                && mc.screen == null;
        boolean descending = piloting && DESCEND.isDown()
                && mc.screen == null && !ascending;
        if (piloting && (!wasPiloting || ascending != lastAscending
                || descending != lastDescending || ++refresh >= 10)) {
            PacketDistributor.sendToServer(new ShorkolotlPilotInputPayload(ascending, descending));
            refresh = 0;
        }
        wasPiloting = piloting;
        lastAscending = ascending;
        lastDescending = descending;
    }
}
