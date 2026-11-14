package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuEntity;
import com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuStage5Journal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record AxoluthuJournalPayload(String content) implements CustomPacketPayload {
    public static final Type<AxoluthuJournalPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "axoluthu_journal_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AxoluthuJournalPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(12000), AxoluthuJournalPayload::content, AxoluthuJournalPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (packet,context) -> context.enqueueWork(() ->
                com.devaxo.templeoftheaxolotl.client.AxoluthuArchitectJournalScreen.open(packet.content())));
    }
    public static void send(ServerPlayer player) {
        String result = "No loaded bonded Axoluthu found. Her project is world-specific.";
        for (WaifuAxoluthuEntity axo : player.serverLevel().getEntitiesOfClass(WaifuAxoluthuEntity.class,
                player.getBoundingBox().inflate(128), a -> player.getUUID().equals(a.getBondedPlayer()))) {
            result = WaifuAxoluthuStage5Journal.liveView(axo);
            break;
        }
        // Never send personalized project contents to an unbonded player.
        if (result.startsWith("No loaded bonded Axoluthu")) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "[Axoluthu] Your bonded Axoluthu must be nearby to open her journal."));
            return;
        }
        PacketDistributor.sendToPlayer(player, new AxoluthuJournalPayload(result));
    }
}
