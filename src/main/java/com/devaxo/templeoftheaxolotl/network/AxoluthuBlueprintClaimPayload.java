package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.*;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** Claim an immutable design once, on the server, from the owner's journal. */
public record AxoluthuBlueprintClaimPayload() implements CustomPacketPayload {
    public static final Type<AxoluthuBlueprintClaimPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "axoluthu_claim_blueprint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AxoluthuBlueprintClaimPayload> STREAM_CODEC =
        StreamCodec.unit(new AxoluthuBlueprintClaimPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet, context) ->
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) claim(player);
            }));
    }
    private static void claim(ServerPlayer player) {
        for (WaifuAxoluthuEntity axo : player.serverLevel().getEntitiesOfClass(
                WaifuAxoluthuEntity.class, player.getBoundingBox().inflate(128),
                a -> player.getUUID().equals(a.getBondedPlayer()))) {
            CompoundTag request = axo.getArchitecturalLearning().getCompound("phase35OwnerSuggestion");
            if (!request.getBoolean("pending") || !"PROPOSAL_READY".equals(request.getString("phase"))
                    || !request.hasUUID("owner") || !player.getUUID().equals(request.getUUID("owner"))) continue;
            String type = WaifuAxoluthuVillageStructures.normalize(request.getString("type"));
            if (type.isEmpty()) {
                player.sendSystemMessage(Component.literal("[Axoluthu] I haven't learned that building design yet! QwQ"));
                return;
            }
            // Generate once, using a fixed origin. The blueprint item owns this
            // immutable manifest; its eventual placement moves the coordinates.
            long seed = request.contains("designSeed", 4) ? request.getLong("designSeed")
                    : java.util.concurrent.ThreadLocalRandom.current().nextLong();
            request.putLong("designSeed", seed);
            CompoundTag design = WaifuAxoluthuVillageStructures.create(type, BlockPos.ZERO, seed);
            WaifuAxoluthuPlayerLessons.personalizePreview(player, type, design);
            UUID blueprintId = UUID.randomUUID();
            CompoundTag data = new CompoundTag();
            data.putUUID("owner", player.getUUID());
            data.putUUID("axo", axo.getUUID());
            data.putUUID("blueprintId", blueprintId);
            data.putString("type", type);
            data.put("manifest", design);
            ItemStack stack = new ItemStack(ModItems.CELESTIAL_BLUEPRINT.get());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            if (!player.getInventory().add(stack)) {
                player.sendSystemMessage(Component.literal("[Axoluthu] Your inventory is full! Make room for my blueprint. :3"));
                return;
            }
            request.putString("phase", "BLUEPRINT_CLAIMED");
            request.putUUID("blueprintId", blueprintId);
            request.putBoolean("constructionAllowed", false);
            request.putBoolean("designApproved", false);
            player.sendSystemMessage(Component.literal("[Axoluthu] Tadaaa! One " + type
                    + " blueprint! Keep it safe until we pick a site! :D"));
            AxoluthuJournalPayload.send(player);
            return;
        }
        player.sendSystemMessage(Component.literal("[Axoluthu] No unclaimed blueprint proposal found nearby."));
    }
}
