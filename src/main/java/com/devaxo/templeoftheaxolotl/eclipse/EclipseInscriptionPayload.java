package com.devaxo.templeoftheaxolotl.eclipse;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Inscription text must be delivered to the server and validated against the actual altar slots. */
public record EclipseInscriptionPayload(String name, boolean uniformFont) implements CustomPacketPayload {
    public static final Type<EclipseInscriptionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "eclipse_inscribe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EclipseInscriptionPayload> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> {
                buf.writeUtf(packet.name(), 50);
                buf.writeBoolean(packet.uniformFont());
            }, buf -> new EclipseInscriptionPayload(buf.readUtf(50), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final class Registration {
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet, context) ->
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player
                                && player.containerMenu instanceof EclipseMenu menu) {
                            menu.inscribe(player, packet.name(), packet.uniformFont());
                        }
                    }));
        }
    }
}
