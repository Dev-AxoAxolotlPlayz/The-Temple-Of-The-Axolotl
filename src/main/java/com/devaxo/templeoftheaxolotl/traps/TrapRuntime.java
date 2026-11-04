
package com.devaxo.templeoftheaxolotl.traps;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Trap runtime independent of maze templates. Floor transitions are assigned by maze code. */
public final class TrapRuntime {
    private TrapRuntime() {}

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** Developer reset: forget per-player trap activation/cooldown state. */
    public static void resetDev(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    private static final class State {
        BlockPos curse, spike;
        long curseReady, spikeReady, teleportReady, warningAt;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        State s = STATES.computeIfAbsent(player.getUUID(), k -> new State());
        long now = level.getGameTime();
        // Only a successful RNGesus curse warp schedules the apparition.
        if (s.warningAt > 0 && now >= s.warningAt) {
            s.warningAt = 0;
            if (level.dimension() == com.devaxo.templeoftheaxolotl.maze.TempleDimensionManager.TEMPLE_DIMENSION
                    && TempleProgress.curse(player) > 0) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.devaxo.templeoftheaxolotl.network.WrathWarningPayload());
            }
        }

        // Restore persistent temple effect markers once per second.
        if (now % 20 == 0) {
            TempleProgress.refresh(player);
        }

        BlockPos feet = player.blockPosition();
        BlockPos beneath = feet.below();
        boolean standingOnTop = player.getY() >= beneath.getY() + 0.98
                && player.getY() < beneath.getY() + 1.35;

        // CORAL SPIKE TRAP
        BlockPos spike = standingOnTop
                && level.getBlockState(beneath).is(ModTrapBlocks.SPIKE_TRAP.get())
                ? beneath : null;

        if (spike == null) {
            s.spike = null;
        } else if (!spike.equals(s.spike) && now >= s.spikeReady) {
            s.spike = spike;
            s.spikeReady = now + 60;
            player.hurt(level.damageSources().magic(), 6.0F);
        }

        // BURN TRAP (legacy SNEAK_TRAP registry ID)
        if (standingOnTop && level.getBlockState(beneath).is(ModTrapBlocks.SNEAK_TRAP.get())) {
            player.igniteForSeconds(3.0F);
        }

        // RNGESUS CURSE TRAP
        BlockPos nearest = null;
        double nearestSq = 4.0;
        BlockPos center = player.blockPosition();

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos at = center.offset(dx, dy, dz);

                    if (!level.hasChunk(at.getX() >> 4, at.getZ() >> 4)
                            || !level.getBlockState(at).is(ModTrapBlocks.RNGESUS_TRAP.get())) {
                        continue;
                    }

                    double dd = player.position().distanceToSqr(
                            at.getX() + 0.5,
                            at.getY() + 0.5,
                            at.getZ() + 0.5
                    );

                    if (dd <= nearestSq) {
                        nearestSq = dd;
                        nearest = at;
                    }
                }
            }
        }

        if (nearest == null) {
            s.curse = null;
        } else if (!nearest.equals(s.curse)
                && now >= s.curseReady
                && now >= s.teleportReady) {

            s.curse = nearest;
            s.curseReady = now + 60;

            // Nerf #2: 10% chance per eligible activation, not per tick.
            if (level.random.nextFloat() < 0.05F) {
                BlockPos marker = findWarpMarkerAbove(level, nearest);

                if (marker != null) {
                    s.teleportReady = now + 60;

                    player.teleportTo(
                            marker.getX() + 0.5,
                            marker.getY() + 1.0,
                            marker.getZ() + 0.5
                    );

                    // Nerf #3: Only successful teleports increase curse stacks.
                    // Also applies the 1-second blink effects.
                    addCurse(player);
                    s.warningAt = now + 20L;

                    // Floor tracking must be updated separately
                    // by the maze/portal transition system.
                }
            }
        }
    }

    private static void addCurse(ServerPlayer player) {
        TempleProgress.addCurse(player);

        // Blink of Betrayal: both effects last 1 second (20 ticks).
        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS, 20, 0, true, false
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS, 20, 0, true, false
        ));
    }

    /** Find nearest safe chiseled-stone-brick warp marker at least 15 blocks above the trap. */
    private static BlockPos findWarpMarkerAbove(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        double bestSq = Double.POSITIVE_INFINITY;
        final int horizontal = 32;
        final int maxUp = 64;

        for (int dy = 15; dy <= maxUp; dy++) {
            for (int dx = -horizontal; dx <= horizontal; dx++) {
                for (int dz = -horizontal; dz <= horizontal; dz++) {
                    if (dx * dx + dz * dz > horizontal * horizontal) continue;

                    BlockPos at = origin.offset(dx, dy, dz);

                    if (!level.hasChunk(at.getX() >> 4, at.getZ() >> 4)
                            || !level.getBlockState(at).is(Blocks.CHISELED_STONE_BRICKS)) {
                        continue;
                    }
                    if (!safe(level, at.above())) continue;

                    double dist = dx * dx + dy * dy + dz * dz;
                    if (dist < bestSq) {
                        bestSq = dist;
                        best = at;
                    }
                }
            }
        }
        return best;
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        if (feet.getY() < level.getMinBuildHeight()
                || feet.getY() + 1 >= level.getMaxBuildHeight()) return false;

        if (!level.hasChunk(feet.getX() >> 4, feet.getZ() >> 4)) return false;

        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above())
                .getCollisionShape(level, feet.above()).isEmpty();
    }
}
  