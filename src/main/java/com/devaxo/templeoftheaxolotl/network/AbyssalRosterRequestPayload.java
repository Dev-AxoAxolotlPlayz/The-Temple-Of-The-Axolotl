package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record AbyssalRosterRequestPayload() implements CustomPacketPayload {
    public static final Type<AbyssalRosterRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"abyssal_roster_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AbyssalRosterRequestPayload> STREAM_CODEC = StreamCodec.unit(new AbyssalRosterRequestPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet,context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                com.devaxo.templeoftheaxolotl.entity.AbyssalCompanionRegistry.sync(player);
                com.devaxo.templeoftheaxolotl.lore.AbyssalLoreIndex.sync(player);
            }
        }));
    }
}
