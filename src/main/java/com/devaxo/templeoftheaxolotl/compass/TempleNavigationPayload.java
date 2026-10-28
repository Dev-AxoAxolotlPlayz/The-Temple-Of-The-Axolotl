package com.devaxo.templeoftheaxolotl.compass;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record TempleNavigationPayload(boolean equipped, boolean valid, int x, int z, boolean arrived, boolean unavailable, boolean compass, boolean clock)
        implements CustomPacketPayload {
    public static final Type<TempleNavigationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "temple_navigation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TempleNavigationPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                buf.writeBoolean(payload.equipped());
                buf.writeBoolean(payload.valid());
                buf.writeInt(payload.x());
                buf.writeInt(payload.z());
                buf.writeBoolean(payload.arrived());
                buf.writeBoolean(payload.unavailable());
                buf.writeBoolean(payload.compass());
                buf.writeBoolean(payload.clock());
            }, buf -> new TempleNavigationPayload(buf.readBoolean(), buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static final class Registration {
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> TempleNavigationClient.accept(payload)));
        }
    }
}
