package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.client.renderer.WaifuAxoluthuProjectionWorldBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** Server-to-client manifest snapshot. No client authority over construction. */
public record AxoluthuProjectionSyncPayload(UUID project, String dimension, CompoundTag manifest)
        implements CustomPacketPayload {
    public static final Type<AxoluthuProjectionSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "axoluthu_projection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AxoluthuProjectionSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                (buf, value) -> {
                    buf.writeUUID(value.project());
                    buf.writeUtf(value.dimension(), 256);
                    buf.writeNbt(value.manifest());
                },
                buf -> new AxoluthuProjectionSyncPayload(buf.readUUID(), buf.readUtf(256), buf.readNbt())
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (packet, context) ->
            context.enqueueWork(() -> {
                ResourceLocation location = ResourceLocation.tryParse(packet.dimension());
                if (location == null) return;
                ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, location);
                WaifuAxoluthuProjectionWorldBridge.update(packet.project(), dimension, packet.manifest());
            }));
    }
}
