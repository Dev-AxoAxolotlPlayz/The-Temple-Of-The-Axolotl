package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy payload retained for compatibility with existing network registration.
 * The pitch-driven vehicle no longer uses a Space-to-ascend signal.
 */
public record ShorkolotlPilotInputPayload(boolean ascending) implements CustomPacketPayload {
    public static final Type<ShorkolotlPilotInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "shorkolotl_pilot_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShorkolotlPilotInputPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ShorkolotlPilotInputPayload::ascending,
                    ShorkolotlPilotInputPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ShorkolotlPilotInputPayload payload, IPayloadContext context) {
        // Obsolete: W thrust now follows the pilot's camera pitch directly.
    }
}
