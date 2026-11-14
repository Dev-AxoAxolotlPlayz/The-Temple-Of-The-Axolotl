package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Non-destructive autonomous expansion proposals. Never edits the world,
 * changes an active owner's request, or grants construction approval.
 */
public final class WaifuAxoluthuVillageExpansion {
    private static final String KEY = "phase35VillageExpansionProposal";
    private static final String OWNER_REQUEST = "phase35OwnerSuggestion";
    private WaifuAxoluthuVillageExpansion() {}

    private static boolean nowForPrompt(ServerLevel level, CompoundTag proposal) {
        return level.getGameTime() - proposal.getLong("lastReminderAt") >= 1200;
    }

    public static void planIfReady(WaifuAxoluthuEntity axo) {
        if (!(axo.level() instanceof ServerLevel level) || axo.getBondedPlayer() == null) return;
        CompoundTag memory = axo.getArchitecturalLearning();
        CompoundTag active = memory.getCompound(OWNER_REQUEST);
        if (active.getBoolean("pending")) return;
        CompoundTag previous = memory.getCompound(KEY);
        if (previous.getBoolean("pending")) {
            // A storage-hut proposal may have been made before the owner had
            // review controls. Re-present it without discarding the site.
            if ("storage".equals(previous.getString("type"))
                    && nowForPrompt(level, previous)) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
                if (owner != null && owner.serverLevel() == level) {
                    owner.sendSystemMessage(Component.literal("[Axoluthu] My storage hut proposal is still waiting! :3 ")
                            .append(Component.literal("[Review storage hut]").withStyle(
                                    net.minecraft.network.chat.Style.EMPTY.withColor(0x55FFAA)
                                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                                    net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                                    "/tota_suggestion village")))));
                    previous.putLong("lastReminderAt", level.getGameTime());
                    memory.put(KEY, previous);
                }
            }
            return;
        }
        long now = level.getGameTime();
        if (now < previous.getLong("nextProposalAt")) return;
        String type = chooseType(axo);
        BlockPos home = axo.getStage4WorkshopOrigin();
        if (home == null) return;
        int width = WaifuAxoluthuVillageStructures.width(type);
        String dimension = level.dimension().location().toString();
        BlockPos site = null;
        // Predictable rings and four axes, avoiding registered buildings.
        for (int radius = 24; radius <= 96 && site == null; radius += 16) {
            for (int direction = 0; direction < 4; direction++) {
                int dx = direction == 0 ? radius : direction == 1 ? -radius : 0;
                int dz = direction == 2 ? radius : direction == 3 ? -radius : 0;
                BlockPos candidate = home.offset(dx, 0, dz);
                if (WaifuAxoluthuVillageRegistry.overlaps(axo, dimension,
                        candidate.offset(-3, 0, -4),
                        candidate.offset(width + 3, 7, width + 3), 4)) continue;
                // Read-only check: do not load chunks or change terrain.
                if (!level.getChunkSource().hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4))
                    continue;
                if (!level.getBlockState(candidate).isAir()) continue;
                if (!level.getBlockState(candidate.below()).isSolidRender(level, candidate.below()))
                    continue;
                site = candidate;
                break;
            }
        }
        if (site == null) {
            previous.putLong("nextProposalAt", now + 2400);
            memory.put(KEY, previous);
            return;
        }
        CompoundTag proposal = new CompoundTag();
        proposal.putString("type", type);
        proposal.putString("dimension", dimension);
        proposal.putLong("site", site.asLong());
        proposal.putLong("createdAt", now);
        proposal.putLong("nextProposalAt", now + 2400);
        proposal.putBoolean("pending", true);
        proposal.putBoolean("ownerApproved", false);
        proposal.putBoolean("constructionAllowed", false);
        proposal.putString("status", "SITE_SUGGESTION_REQUIRES_SURVEY_AND_APPROVAL");
        proposal.put("preview", WaifuAxoluthuVillageStructures.create(type, site));
        memory.put(KEY, proposal);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
        if (owner != null && owner.serverLevel() == level)
            owner.sendSystemMessage(Component.literal("[Axoluthu] I have a " + type
                + " idea near " + site.toShortString()
                + "! :3 Site survey and your approval are needed before building.")
                .append(Component.literal(" [Review proposal]").withStyle(
                        net.minecraft.network.chat.Style.EMPTY.withColor(0x55FFAA)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                "/tota_suggestion village")))));
    }

    public static String chooseType(WaifuAxoluthuEntity axo) {
        int homes = WaifuAxoluthuVillageRegistry.count(axo, "small_house")
                + WaifuAxoluthuVillageRegistry.count(axo, "medium_house")
                + WaifuAxoluthuVillageRegistry.count(axo, "large_house");
        if (homes < 2) return "small_house";
        if (WaifuAxoluthuVillageRegistry.count(axo, "infirmary") == 0) return "infirmary";
        if (WaifuAxoluthuVillageRegistry.count(axo, "smithy") == 0) return "smithy";
        if (WaifuAxoluthuVillageRegistry.count(axo, "library") == 0) return "library";
        if (homes < 5) return "medium_house";
        if (WaifuAxoluthuVillageRegistry.count(axo, "town_center") == 0) return "town_center";
        return "large_house";
    }
}
