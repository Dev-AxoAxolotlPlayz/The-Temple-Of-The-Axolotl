
package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.event.BedrockMiningEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record BedrockMiningPayload(
        boolean mining,
        BlockPos pos
) implements CustomPacketPayload {

    public static final Type<BedrockMiningPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            TempleOfTheAxolotl.MODID,
                            "bedrock_mining"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            BedrockMiningPayload
            > STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.mining());
                buffer.writeBlockPos(payload.pos());
            },
            buffer -> new BedrockMiningPayload(
                    buffer.readBoolean(),
                    buffer.readBlockPos()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static class Registration {

            public static void register(
                RegisterPayloadHandlersEvent event
        ) {
            PayloadRegistrar registrar = event.registrar("1");

            registrar.playToServer(
                    TYPE,
                    STREAM_CODEC,
                    (payload, context) -> {
                        if (context.player() instanceof ServerPlayer player) {
                            BedrockMiningEvents.receiveInput(
                                    player,
                                    payload.mining(),
                                    payload.pos()
                            );
                        }
                    }
            );
        }
    }
}
