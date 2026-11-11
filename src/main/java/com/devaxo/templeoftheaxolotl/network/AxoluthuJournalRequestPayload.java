package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Refreshes the journal only on explicit player interaction; no client-authoritative project edits. */
public record AxoluthuJournalRequestPayload() implements CustomPacketPayload {
    public static final Type<AxoluthuJournalRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "axoluthu_journal_refresh"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AxoluthuJournalRequestPayload> STREAM_CODEC = StreamCodec.unit(new AxoluthuJournalRequestPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet,context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) AxoluthuJournalPayload.send(player);
        }));
    }
}
