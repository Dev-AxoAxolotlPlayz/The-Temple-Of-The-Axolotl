
package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CelestialJumpNetworking {

    private CelestialJumpNetworking() {}

    @EventBusSubscriber(
            modid = TempleOfTheAxolotl.MODID,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class Registration {

        private Registration() {}

        @SubscribeEvent
        public static void registerPayloads(
                RegisterPayloadHandlersEvent event
        ) {
            event.registrar("1").playToClient(
                    WrathWarningPayload.TYPE,
                    WrathWarningPayload.STREAM_CODEC,
                    WrathWarningPayload::handle
            );
            event.registrar("1").playToServer(
                    CelestialDoubleJumpPayload.TYPE,
                    CelestialDoubleJumpPayload.STREAM_CODEC,
                    CelestialDoubleJumpPayload::handle
            );
        }
    }

    @EventBusSubscriber(
            modid = TempleOfTheAxolotl.MODID
    )
    public static final class PlayerEvents {

        private PlayerEvents() {}

        @SubscribeEvent
        public static void onPlayerTick(
                PlayerTickEvent.Post event
        ) {
            if (event.getEntity() instanceof ServerPlayer player) {
                CelestialDoubleJumpPayload.resetIfGrounded(player);
            }
        }
    }
}
