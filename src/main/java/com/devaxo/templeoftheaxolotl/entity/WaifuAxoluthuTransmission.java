package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.nbt.CompoundTag;

/**
 * One inexpensive, read-only selector for the owner-approved exact-site project.
 * Construction is driven by the owner-tick builder; the entity's autonomous
 * planner must not start a competing job while that builder owns the project.
 */
public final class WaifuAxoluthuTransmission {
    private static final String KEY = "phase35OwnerSuggestion";
    private WaifuAxoluthuTransmission() {}

    public enum Gear { IDLE, REVIEW, BUILD, PAUSED }

    public static Gear gear(WaifuAxoluthuEntity axo) {
        CompoundTag memory = axo.getArchitecturalLearning();
        if (!memory.contains(KEY, 10)) return Gear.IDLE;
        CompoundTag request = memory.getCompound(KEY);
        if (!request.getBoolean("pending")) return Gear.IDLE;
        String phase = request.getString("phase");
        if ("PROPOSAL_READY".equals(phase) || "BLUEPRINT_CLAIMED".equals(phase)) return Gear.REVIEW;
        if ("AWAITING_STRUCTURE_INSPECTION".equals(phase)
                || "AWAITING_FURNISHING_INSPECTION".equals(phase)) return Gear.REVIEW;
        if ("COOP_BUILDING".equals(phase)) return Gear.BUILD;
        if ("COOP_PAUSED".equals(phase)) return Gear.PAUSED;
        if ("APPROVED_FOR_CONSTRUCTION".equals(phase)
                || "INSTALLING_FURNISHINGS".equals(phase)) {
            return request.getBoolean("designApproved") && request.getBoolean("constructionAllowed")
                    ? Gear.BUILD : Gear.PAUSED;
        }
        if ("AWAITING_SURVEY".equals(phase)
                || "AWAITING_BLUEPRINT".equals(phase)
                || "SITE_BLOCKED_REQUIRES_RELOCATION".equals(phase)) return Gear.REVIEW;
        if ("BUILD_PAUSED".equals(phase)
                || "DESIGN_APPROVED_AWAITING_BUILD_INTEGRATION".equals(phase)) return Gear.PAUSED;
        // Fail closed for unknown persisted phases while request is pending.
        return Gear.PAUSED;
    }

    public static boolean ownsPlanner(WaifuAxoluthuEntity axo) {
        return gear(axo) != Gear.IDLE;
    }
}
