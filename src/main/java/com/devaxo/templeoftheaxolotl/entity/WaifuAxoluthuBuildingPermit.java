package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Phase 3.5: persistent, owner-only approval for the legacy autonomous builders.
 * This gate never places or removes blocks. */
public final class WaifuAxoluthuBuildingPermit {
    private WaifuAxoluthuBuildingPermit() {}
    private static final String ROOT = "phase35BuildingPermits";

    private static CompoundTag records(WaifuAxoluthuEntity axo) {
        CompoundTag learning = axo.getArchitecturalLearning();
        if (!learning.contains(ROOT, 10)) learning.put(ROOT, new CompoundTag());
        return learning.getCompound(ROOT);
    }

    /** Called after the actual builder reports completion; safe to call again after reload. */
    public static void complete(WaifuAxoluthuEntity axo, String type) {
        if (!"workshop".equals(type) && !"storage".equals(type)) return;
        CompoundTag record = records(axo).getCompound(type);
        if (!record.contains("origin")) return;
        record.putBoolean("completed", true);
        record.putBoolean("announced", true);
    }

    public static boolean permitted(WaifuAxoluthuEntity axo, String type, BlockPos origin) {
        return false; // Retired: only the shared blueprint can authorize construction.
    }

    /** Legacy autonomous builders now submit to the shared blueprint review.
     * Never grants a legacy construction permit or places a legacy block. */
    public static String await(WaifuAxoluthuEntity axo, ServerLevel level, String type, BlockPos origin) {
        if (!"workshop".equals(type) && !"storage".equals(type)) return "UNSUPPORTED_BUILDING";
        CompoundTag learning = axo.getArchitecturalLearning();
        CompoundTag request = learning.getCompound("phase35OwnerSuggestion");
        if (request.getBoolean("pending")) return "UNIFIED_BLUEPRINT_" + request.getString("phase");
        if ("COMPLETE".equals(request.getString("phase"))
                && type.equals(WaifuAxoluthuVillageStructures.normalize(request.getString("type"))))
            return "UNIFIED_BUILD_COMPLETE";
        // A partially built legacy shell must be inspected manually rather than overwritten.
        if (("workshop".equals(type) && axo.getStage4WorkshopStep() > 0)
                || ("storage".equals(type) && axo.getSettlementStorageStep() > 0))
            return "LEGACY_PARTIAL_BUILD_REQUIRES_MANUAL_MIGRATION";
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
        if (owner == null || owner.serverLevel() != level) return "WAITING_FOR_OWNER";
        CompoundTag proposal = new CompoundTag();
        proposal.putBoolean("pending", true);
        proposal.putBoolean("designApproved", false);
        proposal.putBoolean("constructionAllowed", false);
        proposal.putUUID("owner", owner.getUUID());
        proposal.putString("type", type);
        proposal.putLong("target", origin.asLong());
        proposal.putString("dimension", level.dimension().location().toString());
        proposal.putLong("createdAt", level.getGameTime());
        proposal.putString("phase", "AWAITING_SURVEY");
        proposal.putString("source", "legacy_autonomous_migration");
        learning.put("phase35OwnerSuggestion", proposal);
        owner.sendSystemMessage(Component.literal("[Axoluthu] I have a " + type
                + " idea! I'll survey it and show you the new cyan blueprint. :3"));
        return "UNIFIED_SURVEY_QUEUED";
    }

    /** Kept only to explain stale chat buttons; never authorizes legacy construction. */
    public static String decide(WaifuAxoluthuEntity axo, ServerPlayer owner, String type, boolean approve) {
        return "That old permit is retired! Use /tota_suggestion review and approve the cyan blueprint instead. :3";
    }
}
