package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/** Starts the normal Phase 3.5 owner-review pipeline, never a legacy shell. */
public final class WaifuAxoluthuUnifiedStarter {
    private static final String KEY = "phase35OwnerSuggestion";
    private WaifuAxoluthuUnifiedStarter() {}

    public static void tick(WaifuAxoluthuEntity axo, ServerLevel level, String type) {
        CompoundTag memory = axo.getArchitecturalLearning();
        CompoundTag request = memory.getCompound(KEY);
        // An approved, paused, or reviewable project is authoritative. Never
        // reset its blueprint, materials, owner approval or build cursor.
        if (request.getBoolean("pending")) return;
        if ("COMPLETE".equals(request.getString("phase"))
                && type.equals(request.getString("type"))) return;
        // A rejected proposal must not reappear immediately every AI decision.
        if (level.getGameTime() < memory.getLong("phase35StarterRetryAfter")) return;
        // Rejected designs can be proposed again after the retry cooldown.
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
        if (owner == null || owner.serverLevel() != level) return;
        BlockPos home = axo.getHomePosition();
        if (home == null) home = axo.blockPosition();
        BlockPos site = "workshop".equals(type) ? home.offset(9, 0, -9) : home.offset(20, 0, 0);
        // The survey may reject the candidate; only the owner can approve it.
        CompoundTag proposal = new CompoundTag();
        proposal.putBoolean("pending", true);
        proposal.putBoolean("designApproved", false);
        proposal.putBoolean("constructionAllowed", false);
        proposal.putUUID("owner", owner.getUUID());
        proposal.putString("type", type);
        proposal.putLong("target", site.asLong());
        proposal.putString("dimension", level.dimension().location().toString());
        proposal.putLong("createdAt", level.getGameTime());
        proposal.putString("phase", "AWAITING_SURVEY");
        proposal.putString("source", "unified_starter");
        memory.put(KEY, proposal);
        owner.sendSystemMessage(Component.literal("[Axoluthu] I have a " + type
                + " idea! I'll survey it and show you a cyan blueprint. :3"));
    }
}
