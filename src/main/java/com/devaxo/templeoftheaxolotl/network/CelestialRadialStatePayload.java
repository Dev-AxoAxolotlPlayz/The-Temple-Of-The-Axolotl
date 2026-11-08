package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Current server-authoritative module state for the radial interface. */
public record CelestialRadialStatePayload(int flags) implements CustomPacketPayload {
    public static final Type<CelestialRadialStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "celestial_radial_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CelestialRadialStatePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, CelestialRadialStatePayload::flags, CelestialRadialStatePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (packet, context) -> context.enqueueWork(() ->
                com.devaxo.templeoftheaxolotl.client.CelestialRadialWheel.receiveState(packet.flags())));
    }
}
