package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Owner-first, single-target Warden sonic boom. Runs every server tick, before
 * autonomous task planning. No projectile, splash, collision or knockback.
 */
public final class WaifuAxoluthuCombat {
    private static final double RANGE = 15.0;
    private static final double RANGE_SQR = RANGE * RANGE;
    private static final int WINDUP = 12;
    private static final int COOLDOWN = 40;
    private static final int OWNER_EVENT_WINDOW = 100;
    private static final java.util.Map<java.util.UUID, State> STATES = new java.util.WeakHashMap<>();

    private static final class State {
        LivingEntity target;
        int impactTick;
        int nextAttackTick;
        boolean winding;
    }

    private WaifuAxoluthuCombat() {}

    public static boolean tick(WaifuAxoluthuEntity axo, ServerLevel level) {
        if (axo.getBondedPlayer() == null) return false;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
        if (owner == null || owner.level() != level || !owner.isAlive()) return false;
        State state = STATES.computeIfAbsent(axo.getUUID(), unused -> new State());
        LivingEntity selected = select(axo, owner, level);
        if (state.winding) {
            // The locked target can move, but cannot evade a valid sonic impact.
            if (axo.tickCount >= state.impactTick) {
                LivingEntity target = state.target;
                if (valid(axo, owner, target) && axo.distanceToSqr(target) <= RANGE_SQR) {
                    target.hurt(level.damageSources().sonicBoom(axo), 10.0F);
                    axo.beginRailBeam(target);
                    // Deliberately no push, no explosion, no AoE.
                }
                state.winding = false;
                state.target = null;
                state.nextAttackTick = axo.tickCount + COOLDOWN;
                axo.finishAction();
            }
            return true; // Preserve current task; combat takes precedence.
        }
        if (selected == null || axo.tickCount < state.nextAttackTick) return false;
        state.target = selected;
        state.winding = true;
        state.impactTick = axo.tickCount + WINDUP;
        axo.getNavigation().stop();
        axo.getLookControl().setLookAt(selected, 90.0F, 90.0F);
        axo.startAction(WaifuAxoluthuEntity.ACTION_ATTACK, WINDUP + 2, selected.blockPosition());
        return true;
    }

    private static LivingEntity select(WaifuAxoluthuEntity axo, ServerPlayer owner, ServerLevel level) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (owner.tickCount - owner.getLastHurtByMobTimestamp() <= OWNER_EVENT_WINDOW
                && valid(axo, owner, attacker) && axo.distanceToSqr(attacker) <= RANGE_SQR)
            return attacker;
        LivingEntity attacked = owner.getLastHurtMob();
        if (owner.tickCount - owner.getLastHurtMobTimestamp() <= OWNER_EVENT_WINDOW
                && valid(axo, owner, attacked) && axo.distanceToSqr(attacked) <= RANGE_SQR)
            return attacked;
        // Construction obstruction takes priority over ambient threats.
        LivingEntity builderTarget = axo.getTarget();
        if (valid(axo, owner, builderTarget) && axo.distanceToSqr(builderTarget) <= RANGE_SQR)
            return builderTarget;
        LivingEntity closest = null;
        double nearest = RANGE_SQR;
        for (Monster mob : level.getEntitiesOfClass(Monster.class,
                new AABB(axo.blockPosition()).inflate(RANGE), mob -> mob.isAlive())) {
            if (!valid(axo, owner, mob)) continue;
            double distance = axo.distanceToSqr(mob);
            if (distance < nearest) { nearest = distance; closest = mob; }
        }
        // Bats are nuisance targets, but never override owner defense, active
        // construction combat, or a hostile mob already within attack range.
        if (closest != null) return closest;
        for (Bat bat : level.getEntitiesOfClass(Bat.class,
                new AABB(axo.blockPosition()).inflate(RANGE), bat -> bat.isAlive())) {
            if (!valid(axo, owner, bat)) continue;
            double distance = axo.distanceToSqr(bat);
            if (distance < nearest) { nearest = distance; closest = bat; }
        }
        return closest;
    }

    private static boolean valid(WaifuAxoluthuEntity axo, ServerPlayer owner, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()
                || target == axo || target == owner || target.level() != axo.level()) return false;
        if (target instanceof Player || target instanceof Villager
                || target instanceof TamableAnimal pet && pet.isTame()) return false;
        // Don't strike other Axothulu companions.
        if (target instanceof WaifuAxoluthuEntity) return false;
        return true;
    }
}
