package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.client.WrathWarningOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Single client-only warning; scheduling occurs on the server after successful teleport. */
public record WrathWarningPayload() implements CustomPacketPayload {
    public static final Type<WrathWarningPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "wrath_warning"));
    public static final StreamCodec<FriendlyByteBuf, WrathWarningPayload> STREAM_CODEC =
            StreamCodec.unit(new WrathWarningPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(WrathWarningPayload payload, IPayloadContext context) {
        context.enqueueWork(WrathWarningOverlay::show);
    }
}
