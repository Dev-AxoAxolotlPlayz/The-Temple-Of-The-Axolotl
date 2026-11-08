package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record AbyssalLorePayload(int mask) implements CustomPacketPayload {
    public static final Type<AbyssalLorePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"abyssal_lore_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AbyssalLorePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AbyssalLorePayload::mask, AbyssalLorePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (packet,context) -> context.enqueueWork(() ->
                com.devaxo.templeoftheaxolotl.client.AbyssalCommandBookScreen.updateLore(packet.mask())));
    }
}
