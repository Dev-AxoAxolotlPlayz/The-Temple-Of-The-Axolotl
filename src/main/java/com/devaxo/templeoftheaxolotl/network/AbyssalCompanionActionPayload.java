package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** Client sends only an entity UUID; server owns every state transition. */
public record AbyssalCompanionActionPayload(String uuid) implements CustomPacketPayload {
    public static final Type<AbyssalCompanionActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"abyssal_companion_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AbyssalCompanionActionPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, AbyssalCompanionActionPayload::uuid, AbyssalCompanionActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet,context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                final UUID companionId;
                try {
                    companionId = UUID.fromString(packet.uuid().trim());
                } catch (IllegalArgumentException invalid) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[Noodle Rescue] Malformed UUID: '" + packet.uuid() + "'"));
                    TempleOfTheAxolotl.LOGGER.error("Malformed companion UUID '{}'", packet.uuid(), invalid);
                    return;
                }
                try {
                    com.devaxo.templeoftheaxolotl.entity.AbyssalCompanionRegistry.action(player, companionId);
                } catch (Exception ex) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[Noodle Rescue] Registry exception: " + ex.getClass().getSimpleName()
                        + " — see latest.log"));
                    TempleOfTheAxolotl.LOGGER.error("Companion registry failed for {}", companionId, ex);
                }
            }
        }));
    }
}
