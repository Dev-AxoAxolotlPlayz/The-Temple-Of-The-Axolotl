package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Owner-only roster sync. Never send entity data/NBT to the client. */
public record AbyssalRosterPayload(String serialized) implements CustomPacketPayload {
    public static final Type<AbyssalRosterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"abyssal_roster"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AbyssalRosterPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, AbyssalRosterPayload::serialized, AbyssalRosterPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (packet,context) -> context.enqueueWork(() ->
                com.devaxo.templeoftheaxolotl.client.AbyssalCommandBookScreen.updateRoster(packet.serialized())));
    }
}
