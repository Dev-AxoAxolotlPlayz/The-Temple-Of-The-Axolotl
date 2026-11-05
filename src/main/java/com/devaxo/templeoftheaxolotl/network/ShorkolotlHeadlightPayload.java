package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Pilot-only headlight switch, validated on the server. */
public record ShorkolotlHeadlightPayload() implements CustomPacketPayload {
    public static final Type<ShorkolotlHeadlightPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "shorkolotl_headlights"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShorkolotlHeadlightPayload> STREAM_CODEC =
            StreamCodec.unit(new ShorkolotlHeadlightPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final class Registration {
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet, context) ->
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player &&
                                player.getVehicle() instanceof Shorkolotl100Entity submarine &&
                                submarine.getControllingPassenger() == player) submarine.toggleHeadlights();
                    }));
        }
    }
}
