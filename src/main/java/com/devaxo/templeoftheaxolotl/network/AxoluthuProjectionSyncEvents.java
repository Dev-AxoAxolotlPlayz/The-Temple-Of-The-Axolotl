package com.devaxo.templeoftheaxolotl.network;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Periodic nearby-owner synchronization for approved and pending legacy projects.
 * Server remains authoritative; manifests are recreated from saved permit positions.
 */
@EventBusSubscriber(modid = TempleOfTheAxolotl.MODID)
public final class AxoluthuProjectionSyncEvents {
    private AxoluthuProjectionSyncEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 40 != 0) return;
        for (WaifuAxoluthuEntity axo : player.serverLevel().getEntitiesOfClass(
                WaifuAxoluthuEntity.class, player.getBoundingBox().inflate(96),
                a -> player.getUUID().equals(a.getBondedPlayer()))) {
            CompoundTag learning = axo.getArchitecturalLearning();
            CompoundTag permits = learning.getCompound("phase35BuildingPermits");
            send(player, axo, permits, "workshop");
            send(player, axo, permits, "storage");
            sendStage5(player, axo, learning);
            sendOwnerSuggestion(player, axo, learning);
        }
    }


    /** Read-only projection for a conversational suggestion at the owner's exact target. */
    private static void sendOwnerSuggestion(ServerPlayer player, WaifuAxoluthuEntity axo,
                                            CompoundTag learning) {
        CompoundTag request = learning.getCompound("phase35OwnerSuggestion");
        // A held blueprint owns its temporary projection until the owner locks it.
        if ("BLUEPRINT_CLAIMED".equals(request.getString("phase"))) return;
        UUID id = request.hasUUID("blueprintId") ? request.getUUID("blueprintId")
                : UUID.nameUUIDFromBytes((axo.getUUID() + ":owner_suggestion")
                    .getBytes(StandardCharsets.UTF_8));
        String dimension = player.level().dimension().location().toString();
        CompoundTag manifest = new CompoundTag();
        if (request.getBoolean("pending") && request.getBoolean("siteLocked")
                && ("COOP_BUILDING".equals(request.getString("phase"))
                    || "COOP_PAUSED".equals(request.getString("phase")))
                && dimension.equals(request.getString("dimension"))
                && request.contains("blueprintPreview", 10)) {
            manifest = request.getCompound("blueprintPreview").copy();
            manifest.putInt("quarterTurns", request.getInt("quarterTurns"));
        }
        PacketDistributor.sendToPlayer(player,
                new AxoluthuProjectionSyncPayload(id, dimension, manifest));
    }

    /** Stage 5 uses its persisted blueprintPreview, never a regenerated approximation. */
    private static void sendStage5(ServerPlayer player, WaifuAxoluthuEntity axo,
                                   CompoundTag learning) {
        UUID id = UUID.nameUUIDFromBytes(
                (axo.getUUID() + ":stage5_house").getBytes(StandardCharsets.UTF_8));
        CompoundTag project = learning.getCompound("stage5HousingProject");
        String dimension = player.level().dimension().location().toString();
        CompoundTag manifest = new CompoundTag();
        String phase = project.getString("phase");
        boolean inactive = "SITE_REJECTED".equals(phase)
                || "COMPLETE".equals(phase) || "COMPLETED".equals(phase)
                || "CANCELLED".equals(phase) || "HOUSE_COMPLETE".equals(phase);
        if (!inactive && dimension.equals(project.getString("dimension"))
                && project.contains("blueprintPreview", 10)) {
            CompoundTag preview = project.getCompound("blueprintPreview");
            if (preview.contains("blocks", 9)) manifest = preview.copy();
        }
        PacketDistributor.sendToPlayer(player,
                new AxoluthuProjectionSyncPayload(id, dimension, manifest));
    }

    private static void send(ServerPlayer player, WaifuAxoluthuEntity axo,
                             CompoundTag permits, String type) {
        CompoundTag record = permits.getCompound(type);
        UUID id = UUID.nameUUIDFromBytes(
                (axo.getUUID() + ":" + type).getBytes(StandardCharsets.UTF_8));
        CompoundTag manifest = new CompoundTag();
        String dimension = player.level().dimension().location().toString();
        if (record.contains("origin") && !record.getBoolean("completed")
                && !("workshop".equals(type) ? axo.isStage4WorkshopComplete() : axo.isSettlementStorageComplete())
                && dimension.equals(record.getString("dimension"))) {
            BlockPos origin = BlockPos.of(record.getLong("origin"));
            manifest = "workshop".equals(type)
                    ? com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuCelestialBlueprint.workshop(origin)
                    : com.devaxo.templeoftheaxolotl.entity.WaifuAxoluthuCelestialBlueprint.storage(origin);
        }
        PacketDistributor.sendToPlayer(player,
                new AxoluthuProjectionSyncPayload(id, dimension, manifest));
    }
}
