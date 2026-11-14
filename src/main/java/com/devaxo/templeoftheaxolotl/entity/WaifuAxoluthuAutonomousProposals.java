package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/** Read-only autonomous project creation; construction always requires owner approval. */
public final class WaifuAxoluthuAutonomousProposals {
    private static final String KEY = "phase35ProposalPlanner";
    private WaifuAxoluthuAutonomousProposals() {}

    public static void tick(WaifuAxoluthuEntity axo) {
        if (!(axo.level() instanceof ServerLevel level) || axo.getBondedPlayer() == null) return;
        if (!axo.isStage4WorkshopComplete() || !axo.isSettlementStorageComplete()) return;
        WaifuAxoluthuVillageExpansion.planIfReady(axo);
        CompoundTag memory = axo.getArchitecturalLearning();
        CompoundTag state = memory.getCompound(KEY);
        // Never overwrite a previously created or approved project.
        if (memory.contains("stage5HousingProject", 10)) return;
        long now = level.getGameTime();
        if (now < state.getLong("retryAfter")) return;
        state.putLong("retryAfter", now + 1200L);
        memory.put(KEY, state);

        if (!"house".equals(WaifuAxoluthuVillageRegistry.recommendNext(axo))) return;
        String result = WaifuAxoluthuStage5Journal.plan(axo);
        if (!memory.contains("stage5HousingProject", 10)) return;
        WaifuAxoluthuStage5Journal.blueprint(axo);
        if (!memory.getCompound("stage5HousingProject").contains("blueprintPreview", 10)) return;
        WaifuAxoluthuStage5Journal.survey(axo);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
        if (owner != null && owner.serverLevel() == level) {
            String review = WaifuAxoluthuStage5SiteApproval.propose(axo, owner);
            owner.sendSystemMessage(Component.literal(
                "[Axoluthu] I designed a house for our village! :3 "
                + review));
        }
        // If owner is offline, the saved blueprint remains pending review.
        // No materials are gathered, no blocks placed, and no approval is granted.
    }
}
