package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.combat.RingBoomerangCombat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RingAttackInputPayload(boolean held) implements CustomPacketPayload {
    public static final Type<RingAttackInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "ring_attack_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RingAttackInputPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, RingAttackInputPayload::held, RingAttackInputPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(RingAttackInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                RingBoomerangCombat.setHeld(player, payload.held());
            }
        });
    }
}
