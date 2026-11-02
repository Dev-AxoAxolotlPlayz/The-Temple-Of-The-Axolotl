package com.devaxo.templeoftheaxolotl.traps;

import com.devaxo.templeoftheaxolotl.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Server-side run markers. Maze/portal/sanctum code must call the transitions;
 * never infer a floor number from world Y or a decorative marker.
 * State persists in player persistent NBT. Copy tags in a clone handler if needed.
 */
public final class TempleProgress {
    private TempleProgress() {}

    private static final String FLOOR = "axoTempleFloor";
    private static final String STATE = "axoTempleState"; // 0=none, 1=exploring, 2=fighting, 3=save-me
    private static final String CURSE = "axoRngesusCurseStacks";

    public static int floor(ServerPlayer p) { return p.getPersistentData().getInt(FLOOR); }
    public static int state(ServerPlayer p) { return p.getPersistentData().getInt(STATE); }
    public static int curse(ServerPlayer p) { return p.getPersistentData().getInt(CURSE); }

    private static final String DECOY_WRATH_DEFEATS = "axoDecoyWrathDefeats";
    /** Lifetime per-player victories, independent of Temple run progress. */
    public static int decoyWrathDefeats(ServerPlayer player) {
        return Math.max(0, player.getPersistentData().getInt(DECOY_WRATH_DEFEATS));
    }
    public static void recordDecoyWrathDefeat(ServerPlayer player) {
        int old = decoyWrathDefeats(player);
        if (old < Integer.MAX_VALUE) player.getPersistentData().putInt(DECOY_WRATH_DEFEATS, old + 1);
    }
    public static int originWrathTier(ServerPlayer player) {
        int count = decoyWrathDefeats(player);
        return count <= 10 ? 1 : count <= 20 ? 2 : 3;
    }
    /** Developer testing only; never reset by finishRun or resetDev. */
    public static void setDecoyWrathDefeatsDev(ServerPlayer player, int value) {
        player.getPersistentData().putInt(DECOY_WRATH_DEFEATS, Math.max(0, value));
    }

    /** Call on entry and on each floor change, including curse teleports; floors 1..10. */
    public static void setFloor(ServerPlayer p, int floor) {
        p.getPersistentData().putInt(FLOOR, Math.max(1, Math.min(10, floor)));
        if (state(p) == 0) p.getPersistentData().putInt(STATE, 1);
        refresh(p);
    }

    /** Call only after Wrath successfully spawns. */
    public static void wrathSpawned(ServerPlayer p) {
        p.getPersistentData().putInt(STATE, 2);
        refresh(p);
    }

    /** Original temple only: after Wrath is defeated. */
    public static void originalWrathDefeated(ServerPlayer p) {
        p.getPersistentData().putInt(STATE, 3);
        refresh(p);
    }

    /** Ordinary temple: on sanctum chest opening; original temple: after rescue. */
    public static void finishRun(ServerPlayer p) {
        p.getPersistentData().remove(FLOOR);
        p.getPersistentData().remove(STATE);
        p.getPersistentData().remove(CURSE);
        p.removeEffect(ModEffects.RNGESUS_CURSE);
        p.removeEffect(ModEffects.HE_SAYS_LEAVE);
        p.removeEffect(ModEffects.YOU_SHOULDNT_HAVE);
        p.removeEffect(ModEffects.SAVE_ME);
    }

    /** Operator-only command helper: clear saved run data and all temple-related effects. */
    public static void resetDev(ServerPlayer p) {
        finishRun(p);
        p.removeEffect(MobEffects.BLINDNESS);
        p.removeEffect(MobEffects.DARKNESS);
        TrapRuntime.resetDev(p);
    }

    public static void addCurse(ServerPlayer p) {
        int stacks = Math.min(255, curse(p) + 1);
        p.getPersistentData().putInt(CURSE, stacks);
        refresh(p);
    }

    /** Reapply absent/incorrect markers only; never retrigger blindness/darkness. */
    public static void refresh(ServerPlayer p) {
        int stacks = curse(p);
        marker(p, ModEffects.RNGESUS_CURSE, stacks > 0, Math.max(0, stacks - 1));
        int s = state(p);
        marker(p, ModEffects.HE_SAYS_LEAVE, s == 1 && floor(p) > 0,
                Math.max(0, floor(p) - 1));
        marker(p, ModEffects.YOU_SHOULDNT_HAVE, s == 2, 0);
        marker(p, ModEffects.SAVE_ME, s == 3, 0);
    }

    private static void marker(ServerPlayer p, DeferredHolder<MobEffect, MobEffect> effect,
                               boolean wanted, int amplifier) {
        MobEffectInstance current = p.getEffect(effect);
        if (!wanted) {
            if (current != null) p.removeEffect(effect);
        } else if (current == null || current.getAmplifier() != amplifier
                || current.getDuration() < 100) {
            p.addEffect(new MobEffectInstance(effect, 1200, amplifier, true, true));
        }
    }
}
