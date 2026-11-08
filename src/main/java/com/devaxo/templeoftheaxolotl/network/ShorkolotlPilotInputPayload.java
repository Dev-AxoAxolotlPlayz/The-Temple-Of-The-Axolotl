package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;
import com.devaxo.templeoftheaxolotl.entity.Shorkolotl100Entity;

/**
 * Sends independent Space ascent and V descent signals from the pilot.
 */
public record ShorkolotlPilotInputPayload(boolean ascending, boolean descending) implements CustomPacketPayload {
    public static final Type<ShorkolotlPilotInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "shorkolotl_pilot_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShorkolotlPilotInputPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ShorkolotlPilotInputPayload::ascending,
                    ByteBufCodecs.BOOL, ShorkolotlPilotInputPayload::descending,
                    ShorkolotlPilotInputPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ShorkolotlPilotInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.getVehicle() instanceof Shorkolotl100Entity sub
                    && sub.getControllingPassenger() == player) {
                sub.setPilotControls(payload.ascending(), payload.descending());
            }
        });
    }
}
