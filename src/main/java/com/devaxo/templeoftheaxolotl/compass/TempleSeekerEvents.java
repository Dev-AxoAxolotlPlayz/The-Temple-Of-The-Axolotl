package com.devaxo.templeoftheaxolotl.compass;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

/** Periodically refreshes the lodestone tracker, not every render frame. */
public final class TempleSeekerEvents {
    private static final TagKey<Structure> TEMPLES = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "seeker_targets"));
    private static final String TARGET = "tota_seeker_target";
    private static final String STATE = "tota_seeker_state"; // 0 dormant, 1 tracking, 2 arrived, 3 unavailable

    private static int state(ServerPlayer player) {
        return player.getPersistentData().getInt(STATE);
    }

    private static BlockPos target(ServerPlayer player) {
        if (!player.getPersistentData().contains(TARGET)) return null;
        return BlockPos.of(player.getPersistentData().getLong(TARGET));
    }

    public static boolean arrived(ServerPlayer player) { return state(player) == 2; }
    public static boolean unavailable(ServerPlayer player) { return state(player) == 3; }

    @SubscribeEvent
    public void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        BlockPos locked = target(player);
        if (state(player) == 1 && locked != null) {
            double dx = locked.getX() + .5 - player.getX();
            double dz = locked.getZ() + .5 - player.getZ();
            if (Math.hypot(dx, dz) <= 5.0) {
                player.getPersistentData().putInt(STATE, 2);
            } else if (TemplePortalRegistry.get(player.serverLevel()).isConsumed(locked)) {
                player.getPersistentData().putInt(STATE, 3);
            }
        }
        updateCompassStacks(player, state(player) == 1 ? locked : null);
    }

    public static void showDistance(ServerPlayer player) {
        int mode = state(player);
        if (mode == 2) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Temple Arrived. The Axolotl Absolute Watches"), true);
            return;
        }
        if (mode == 3) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Temple target unavailable. Right-click to locate another."), true);
            return;
        }
        BlockPos locked = target(player);
        if (mode != 1 || locked == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Temple Seeker dormant. Right-click to locate a temple."), true);
            return;
        }
        long blocks = Math.round(Math.hypot(locked.getX() + .5 - player.getX(),
                locked.getZ() + .5 - player.getZ()));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "Temple distance: " + blocks + " blocks"), true);
    }

    /** Only a deliberate item use can start a new search. */
    public static boolean refresh(ServerPlayer player) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return false;
        BlockPos found = findNext(player.serverLevel(), player.blockPosition(),
                TemplePortalRegistry.get(player.serverLevel()));
        if (found == null) {
            player.getPersistentData().remove(TARGET);
            player.getPersistentData().putInt(STATE, 0);
        } else {
            player.getPersistentData().putLong(TARGET, found.asLong());
            player.getPersistentData().putInt(STATE, 1);
        }
        updateCompassStacks(player, found);
        return found != null;
    }

    public static void updateModuleStack(ServerPlayer player, ItemStack module) {
        updateStack(module, state(player) == 1 ? target(player) : null);
    }

    private static void updateCompassStacks(ServerPlayer player, BlockPos target) {
        for (ItemStack stack : player.getInventory().items) updateStack(stack, target);
        updateStack(player.getOffhandItem(), target);
    }

    private static void updateStack(ItemStack stack, BlockPos target) {
        if (!stack.is(ModItems.TEMPLE_SEEKER.get())) return;
        if (target == null) stack.remove(DataComponents.LODESTONE_TRACKER);
        else stack.set(DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(GlobalPos.of(Level.OVERWORLD, target)), false));
    }

    private static BlockPos findNext(ServerLevel level, BlockPos origin, TemplePortalRegistry consumed) {
        var tag = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(TEMPLES);
        if (tag.isEmpty()) return null;
        HolderSet<Structure> holders = tag.get();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        // Multiple independent locate origins avoid getting stuck on one consumed structure.
        // Search remains bounded and runs only when a target expires or the player moves far.
        for (int radius : new int[]{0, 1024, 3072, 6144, 10000}) {
            int directions = radius == 0 ? 1 : 12;
            for (int i = 0; i < directions; i++) {
                double angle = Math.PI * 2 * i / directions;
                BlockPos probe = origin.offset((int)(radius * Math.cos(angle)), 0,
                        (int)(radius * Math.sin(angle)));
                var result = level.getChunkSource().getGenerator().findNearestMapStructure(
                        level, holders, probe, 1024, false);
                if (result == null) continue;
                // Structure locate positions refer to the structure's start, not its portal.
                // The compass targets the temple footprint until exact portal positions are indexed.
                BlockPos candidate = result.getFirst();
                if (consumed.isConsumed(candidate)) continue;
                double distance = candidate.distSqr(origin);
                if (distance < bestDistance) { bestDistance = distance; best = candidate; }
            }
        }
        return best;
    }
}
