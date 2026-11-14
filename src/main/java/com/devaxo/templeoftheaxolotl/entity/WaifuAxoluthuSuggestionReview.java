package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/** Owner review of a conversational suggestion. No building authorization is issued. */
public final class WaifuAxoluthuSuggestionReview {
    private static final String KEY = "phase35OwnerSuggestion";
    private WaifuAxoluthuSuggestionReview() {}

    // Retired: chat-based approve/rotate/relocate/redesign commands bypassed
    // the journal's physical Celestial Blueprint placement and locking flow.
    // Old commands are intentionally not registered.

    /** Legacy review prompts now point to the owner-controlled physical blueprint workflow.
     * Never approve or relocate a project from a chat control.
     */
    private static void showControls(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                "[Axoluthu] Open the Architect's Journal, claim the Celestial Blueprint, "
                + "place and adjust its projection, then lock the site to authorize construction. :3"));
    }

    private static net.minecraft.network.chat.MutableComponent link(String label, String command, int color) {
        return Component.literal(label).withStyle(Style.EMPTY.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    private static int action(CommandSourceStack source, String action)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        for (WaifuAxoluthuEntity axo : player.serverLevel().getEntitiesOfClass(
                WaifuAxoluthuEntity.class, player.getBoundingBox().inflate(48),
                a -> player.getUUID().equals(a.getBondedPlayer()))) {
            CompoundTag learning = axo.getArchitecturalLearning();
            if ("village".equals(action)) {
                CompoundTag proposal = learning.getCompound("phase35VillageExpansionProposal");
                if (!proposal.getBoolean("pending")) {
                    player.sendSystemMessage(Component.literal("[Axoluthu] No village idea is waiting. :3"));
                    return 0;
                }
                CompoundTag current = learning.getCompound(KEY);
                if (current.getBoolean("pending")) {
                    player.sendSystemMessage(Component.literal("[Axoluthu] Finish your existing suggestion first!"));
                    return 0;
                }
                if (!player.level().dimension().location().toString()
                        .equals(proposal.getString("dimension"))) return 0;
                BlockPos site = BlockPos.of(proposal.getLong("site"));
                String type = WaifuAxoluthuVillageStructures.normalize(proposal.getString("type"));
                if (type.isEmpty()) return 0;
                CompoundTag promoted = new CompoundTag();
                promoted.putBoolean("pending", true);
                promoted.putBoolean("constructionAllowed", false);
                promoted.putBoolean("designApproved", false);
                promoted.putUUID("owner", player.getUUID());
                promoted.putString("type", type);
                promoted.putLong("target", site.asLong());
                promoted.putString("dimension", proposal.getString("dimension"));
                promoted.putString("phase", "AWAITING_SURVEY");
                promoted.putLong("createdAt", player.serverLevel().getGameTime());
                learning.put(KEY, promoted);
                proposal.putBoolean("pending", false);
                proposal.putString("status", "TRANSFERRED_TO_OWNER_SITE_SURVEY");
                learning.put("phase35VillageExpansionProposal", proposal);
                player.sendSystemMessage(Component.literal("[Axoluthu] Let's survey the "
                        + type + " site! Your approval is still required. :3"));
                return 1;
            }
            CompoundTag request = learning.getCompound(KEY);
            if (!request.getBoolean("pending") || !request.hasUUID("owner")
                    || !player.getUUID().equals(request.getUUID("owner"))
                    || !player.level().dimension().location().toString().equals(request.getString("dimension")))
                continue;
            if ("review".equals(action)) {
                player.sendSystemMessage(Component.literal("[Axoluthu] " + request.getString("type")
                        + " at " + BlockPos.of(request.getLong("target")).toShortString()
                        + " | " + request.getString("phase")));
                if ("AWAITING_OWNER_REVIEW".equals(request.getString("phase"))) showControls(player);
                return 1;
            }
            if (!"AWAITING_OWNER_REVIEW".equals(request.getString("phase"))
                    || !request.contains("blueprintPreview", 10)) {
                source.sendFailure(Component.literal("[Axoluthu] No completed blueprint ready for review."));
                return 0;
            }
            if ("relocate".equals(action) || "redesign".equals(action)) {
                if ("relocate".equals(action)) {
                    // Reposition the EXISTING manifest to the player's feet, not the
                    // block the player happens to be looking at. Keep rotation and
                    // architecture; invalidate old approval and re-prompt immediately.
                    BlockPos old = BlockPos.of(request.getLong("target"));
                    BlockPos target = player.blockPosition();
                    BlockPos delta = target.subtract(old);
                    CompoundTag preview = request.getCompound("blueprintPreview").copy();
                    ListTag blocks = preview.getList("blocks", 10);
                    for (int i = 0; i < blocks.size(); i++) {
                        CompoundTag entry = blocks.getCompound(i);
                        BlockPos moved = BlockPos.of(entry.getLong("pos")).offset(delta);
                        if (moved.getY() < player.serverLevel().getMinBuildHeight()
                                || moved.getY() >= player.serverLevel().getMaxBuildHeight()
                                || !player.serverLevel().getChunkSource().hasChunk(moved.getX() >> 4, moved.getZ() >> 4)) {
                            source.sendFailure(Component.literal("[Axoluthu] New site is unloaded or outside build height."));
                            return 0;
                        }
                        entry.putLong("pos", moved.asLong());
                    }
                    if (preview.contains("entrance", 4))
                        preview.putLong("entrance", BlockPos.of(preview.getLong("entrance")).offset(delta).asLong());
                    preview.putLong("origin",target.asLong());
                    request.putLong("target", target.asLong());
                    request.put("blueprintPreview", preview);
                    request.putBoolean("designApproved", false);
                    request.putBoolean("constructionAllowed", false);
                    request.putInt("buildCursor", 0);
                    request.putInt("furnishingCursor", 0);
                    request.remove("buildWaitReason");
                    request.remove("supplyBatchItem");
                    request.remove("supplyBatchTarget");
                    request.putString("phase", "AWAITING_OWNER_REVIEW");
                    learning.put(KEY, request);
                    player.sendSystemMessage(Component.literal("[Axoluthu] Moved the whole hologram to your position! Please approve the new site. :3"));
                    showControls(player);
                } else {
                    long oldSeed=request.getLong("designSeed");
                    long newSeed=java.util.concurrent.ThreadLocalRandom.current().nextLong();
                    if(newSeed==oldSeed) newSeed++;
                    CompoundTag preview=WaifuAxoluthuVillageStructures.create(
                            WaifuAxoluthuVillageStructures.normalize(request.getString("type")),
                            BlockPos.of(request.getLong("target")),newSeed);
                    WaifuAxoluthuPlayerLessons.personalizePreview(player,
                            WaifuAxoluthuVillageStructures.normalize(request.getString("type")), preview);
                    // Redesign preserves the owner's selected entrance
                    // orientation, not just the plot position.
                    int turns=Math.floorMod(request.getInt("quarterTurns"),4);
                    BlockPos pivot=BlockPos.of(request.getLong("target"));
                    for(int turn=0;turn<turns;turn++) {
                        preview=WaifuAxoluthuBlueprintRotation.rotateClockwise(preview,pivot);
                    }
                    // Use the generated core width, which may have expanded.
                    BlockPos entry=pivot.offset(preview.getInt("width")/2,1,0);
                    for(int turn=0;turn<turns;turn++) {
                        int dx=entry.getX()-pivot.getX(),dz=entry.getZ()-pivot.getZ();
                        entry=pivot.offset(-dz,entry.getY()-pivot.getY(),dx);
                    }
                    preview.putLong("entrance",entry.asLong());
                    preview.putString("facing",switch(turns) {
                        case 1 -> "east";
                        case 2 -> "south";
                        case 3 -> "west";
                        default -> "north";
                    });
                    request.putLong("designSeed",newSeed);
                    request.put("blueprintPreview",preview);
                    request.putBoolean("designApproved",false);
                    request.putBoolean("constructionAllowed",false);
                    request.putInt("buildCursor",0);
                    request.putInt("furnishingCursor",0);
                    request.remove("buildWaitReason");
                    request.putString("phase","AWAITING_OWNER_REVIEW");
                    learning.put(KEY,request);
                    player.sendSystemMessage(Component.literal("[Axoluthu] New architecture! :D Here's a different footprint. Approve, redesign or relocate?"));
                    showControls(player);
                }
                return 1;
            }
            if ("reject".equals(action)) {
                request.putBoolean("pending", false);
                request.putBoolean("constructionAllowed", false);
                if ("legacy_autonomous_migration".equals(request.getString("source"))) {
                    if ("workshop".equals(request.getString("type"))) axo.setStage4WorkshopOrigin(null);
                    if ("storage".equals(request.getString("type"))) axo.setSettlementStorageOrigin(null);
                }
                request.putString("phase", "REJECTED");
                request.remove("blueprintPreview");
                learning.put(KEY, request);
                player.sendSystemMessage(Component.literal("[Axoluthu] Oki! I'll put that idea aside. ^u^"));
                return 1;
            }
            if ("approve".equals(action)) {
                request.putBoolean("designApproved", true);
                boolean buildReady = WaifuAxoluthuExactSiteBuilder.validWorkshop(request);
                request.putBoolean("constructionAllowed", buildReady);
                request.putString("phase", buildReady ? "APPROVED_FOR_CONSTRUCTION"
                        : "DESIGN_APPROVED_AWAITING_BUILD_INTEGRATION");
                learning.put(KEY, request);
                player.sendSystemMessage(Component.literal(buildReady
                        ? "[Axoluthu] Yippee! I'll gather materials for the approved building shell! :D"
                        : "[Axoluthu] Design approved, but the safe builder rejected it: "
                            + WaifuAxoluthuExactSiteBuilder.validationIssue(request)));
                return 1;
            }
            if ("rotate".equals(action)) {
                CompoundTag preview = request.getCompound("blueprintPreview");
                BlockPos pivot = BlockPos.of(request.getLong("target"));
                CompoundTag rotated;
                try {
                    rotated = WaifuAxoluthuBlueprintRotation.rotateClockwise(preview, pivot);
                } catch (IllegalArgumentException ex) {
                    source.sendFailure(Component.literal("[Axoluthu] Can't rotate: " + ex.getMessage()));
                    return 0;
                }
                ListTag blocks = rotated.getList("blocks", 10);
                for (int i = 0; i < blocks.size(); i++) {
                    BlockPos pos = BlockPos.of(blocks.getCompound(i).getLong("pos"));
                    if (pos.getY() < player.serverLevel().getMinBuildHeight()
                            || pos.getY() >= player.serverLevel().getMaxBuildHeight()
                            || !player.serverLevel().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                        source.sendFailure(Component.literal("[Axoluthu] Rotated blueprint reaches unloaded/out-of-bounds blocks."));
                        return 0;
                    }
                }
                // Rotate entrance metadata too; renderer relies on it for the gold marker.
                if (preview.contains("entrance", 4)) {
                    BlockPos entry = BlockPos.of(preview.getLong("entrance"));
                    int dx = entry.getX() - pivot.getX(), dz = entry.getZ() - pivot.getZ();
                    rotated.putLong("entrance", pivot.offset(-dz, entry.getY()-pivot.getY(), dx).asLong());
                }
                rotated.putString("facing", switch (preview.getString("facing")) {
                    case "north" -> "east";
                    case "east" -> "south";
                    case "south" -> "west";
                    default -> "north";
                });
                request.put("blueprintPreview", rotated);
                request.putInt("quarterTurns", (request.getInt("quarterTurns") + 1) % 4);
                request.putBoolean("designApproved", false);
                request.putBoolean("constructionAllowed", false);
                learning.put(KEY, request);
                player.sendSystemMessage(Component.literal("[Axoluthu] Rotated! Check the hologram again. :3"));
                showControls(player);
                return 1;
            }
        }
        source.sendFailure(Component.literal("[Axoluthu] No pending suggestion for your bonded Axoluthu within 48 blocks."));
        return 0;
    }
}
