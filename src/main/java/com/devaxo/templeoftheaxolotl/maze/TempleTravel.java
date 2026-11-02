package com.devaxo.templeoftheaxolotl.maze;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import com.devaxo.templeoftheaxolotl.compass.TemplePortalRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.commands.Commands;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Stage 4 travel prototype. Does not generate the dungeon or manage shared sessions. */
public final class TempleTravel {
    private static final Set<UUID> WAITING = new HashSet<>();
    // Portal locations are collected only after successful dungeon teleportation.
    // All players share this session; one portal can be used by many players.
    private static final Set<BlockPos> SESSION_PORTALS = new HashSet<>();
    private static final String ANCHOR = "tota_return_anchor";
    private static final String ORIGINAL_MODE = "tota_original_gamemode";

    private static void restoreMode(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ORIGINAL_MODE)) return;
        GameType previous = GameType.byId(data.getInt(ORIGINAL_MODE));
        player.setGameMode(previous);
        data.remove(ORIGINAL_MODE);
    }
    private static boolean visitedThisSession = false;
    private static MinecraftServer startupServer;
    private static boolean startupRecoveryPending = true;
    private static boolean startupRecoveryAnnounced = false;
    private static int startupDelayTicks = 0;
    private static final int STARTUP_RECOVERY_DELAY = 100;
    private static boolean closingThisSession = false;
    private static long cleanupDeadline = -1L;
    private static final int CLEANUP_DELAY_TICKS = 100;
    private static final double ENTRY_X = 22.5, ENTRY_Y = 202, ENTRY_Z = 26.5;

    public static boolean enter(ServerPlayer player, BlockPos portal) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) {
            player.displayClientMessage(Component.literal("The temple entrance only works in the Overworld."), true);
            return false;
        }
        if (TemplePortalRegistry.get(player.serverLevel()).isConsumed(portal)) {
            player.displayClientMessage(Component.literal("This temple portal has already been consumed."), true);
            return false;
        }
        MinecraftServer server = player.getServer();
        ServerLevel dungeon = server == null ? null : server.getLevel(TempleDimensionManager.TEMPLE_DIMENSION);
        if (dungeon == null) {
            player.displayClientMessage(Component.literal("Temple dimension is unavailable."), true);
            return false;
        }
        if (player.getPersistentData().contains(ANCHOR) || WAITING.contains(player.getUUID())) {
            player.displayClientMessage(Component.literal("You already have an active temple visit or pending entry."), true);
            return false;
        }
        // A deep clean owns the temple until it has actually finished.
        if (closingThisSession && !AxoMazeCommands.isMazeOccupiedByBuild(dungeon)) {
            closingThisSession = false;
            visitedThisSession = false;
        }
        if (startupRecoveryPending || closingThisSession || AxoMazeCommands.isDeconstructing(dungeon)) {
            player.displayClientMessage(Component.literal("The temple is resetting. Please try again shortly."), true);
            return false;
        }
        // The existing generator places three modules per server tick. NEVER teleport into
        // the dimension until the entire build has completed and the entrance is safe.
        if (!AxoMazeCommands.isBuildComplete(dungeon)
                && !AxoMazeCommands.isMazeOccupiedByBuild(dungeon)) {
            int started = AxoMazeCommands.generateInTemple(ThreadLocalRandom.current().nextLong(),
                    player.createCommandSourceStack());
            if (started == 0) {
                player.displayClientMessage(Component.literal("Temple construction could not start. You remain safe outside."), true);
                return false;
            }
        }
        CompoundTag anchor = new CompoundTag();
        anchor.putInt("x", portal.getX());
        anchor.putInt("y", portal.getY());
        anchor.putInt("z", portal.getZ());
        player.getPersistentData().put(ANCHOR, anchor);
        WAITING.add(player.getUUID());
        player.displayClientMessage(Component.literal("The temple is preparing. Stay nearby; you will enter when it is ready."), true);
        return true;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        // The integrated server stops ticking when singleplayer closes. Recover a
        // previous dungeon on the next server start, before accepting new visits.
        if (startupServer != server) {
            startupServer = server;
            startupRecoveryPending = true;
            startupRecoveryAnnounced = false;
            startupDelayTicks = 0;
            WAITING.clear();
            SESSION_PORTALS.clear();
            visitedThisSession = false;
            closingThisSession = false;
            cleanupDeadline = -1L;
        }
        if (startupRecoveryPending) {
            ServerLevel temple = server.getLevel(TempleDimensionManager.TEMPLE_DIMENSION);
            if (temple == null) {
                startupDelayTicks = 0;
                return;
            }
            if (++startupDelayTicks < STARTUP_RECOVERY_DELAY) return;
            if (temple.players().isEmpty()
                    && AxoMazeCommands.beginAutomaticFullReset(temple)) {
                startupRecoveryPending = false;
                closingThisSession = true;
            }
        }
        if (startupRecoveryPending) return;
        if (!startupRecoveryAnnounced) {
            ServerLevel temple = server.getLevel(TempleDimensionManager.TEMPLE_DIMENSION);
            if (temple != null && !AxoMazeCommands.isDeconstructing(temple)) {
                startupRecoveryAnnounced = true;
                // Announced to each joining player below, including singleplayer. 
            }
        }
        checkEmptyTemple(server);
        if (WAITING.isEmpty()) return;
        ServerLevel dungeon = server.getLevel(TempleDimensionManager.TEMPLE_DIMENSION);
        if (dungeon == null) return;
        // Maze builder's tick callback may run before or after this listener.
        if (AxoMazeCommands.isBuildComplete(dungeon)) {
            // The entry is at (22.5,202,26.5), standing on a block at Y=201.
            BlockPos floor = BlockPos.containing(ENTRY_X, ENTRY_Y - 1, ENTRY_Z);
            if (dungeon.getBlockState(floor).isAir()
                    || !dungeon.getBlockState(floor.above()).isAir()
                    || !dungeon.getBlockState(floor.above(2)).isAir()) {
                cancelWaiting(server, "Temple entrance is obstructed; teleport cancelled.");
                return;
            }
            for (UUID id : Set.copyOf(WAITING)) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player == null) { WAITING.remove(id); continue; }
                if (player.serverLevel().dimension() != Level.OVERWORLD) {
                    WAITING.remove(id);
                    player.getPersistentData().remove(ANCHOR);
                    continue;
                }
                // Refuse entry if a previous run still owns staged rewards.
                UUID templeId = AxoMazeCommands.activeTempleId(dungeon);
                if (templeId == null || com.devaxo.templeoftheaxolotl.loot.DungeonRewardInventory.occupied(player) != 0) {
                    player.displayClientMessage(Component.literal(
                            "Temple entry blocked: no active temple or unclaimed staged rewards remain."), true);
                    player.getPersistentData().remove(ANCHOR);
                    WAITING.remove(id);
                    continue;
                }
                boolean moved = player.teleportTo(dungeon, ENTRY_X, ENTRY_Y, ENTRY_Z,
                        Set.of(), player.getYRot(), player.getXRot());
                if (moved) {
                    if (!player.getPersistentData().contains(ORIGINAL_MODE))
                        player.getPersistentData().putInt(ORIGINAL_MODE, player.gameMode.getGameModeForPlayer().getId());
                    player.setGameMode(GameType.ADVENTURE);
                    visitedThisSession = true;
                    com.devaxo.templeoftheaxolotl.loot.DungeonRewardInventory.bind(
                            player, templeId, AxoMazeCommands.activeTempleSeed(dungeon));
                    // A queued entry only counts once the player actually arrives.
                    CompoundTag used = player.getPersistentData().getCompound(ANCHOR);
                    SESSION_PORTALS.add(new BlockPos(used.getInt("x"), used.getInt("y"), used.getInt("z")));
                } else {
                    player.getPersistentData().remove(ANCHOR);
                    player.displayClientMessage(Component.literal("Temple teleport failed."), true);
                }
                WAITING.remove(id);
            }
        } else if (!AxoMazeCommands.isMazeOccupiedByBuild(dungeon)) {
            cancelWaiting(server, "Temple generation stopped before completion; entry cancelled.");
        }
    }

    private static void cancelWaiting(MinecraftServer server, String message) {
        for (UUID id : Set.copyOf(WAITING)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.getPersistentData().remove(ANCHOR);
                player.displayClientMessage(Component.literal(message), true);
            }
            WAITING.remove(id);
        }
    }

    public static boolean exit(ServerPlayer player) {
        if (!player.getPersistentData().contains(ANCHOR)) return false;
        MinecraftServer server = player.getServer();
        ServerLevel overworld = server == null ? null : server.overworld();
        if (overworld == null) return false;
        CompoundTag anchor = player.getPersistentData().getCompound(ANCHOR);
        BlockPos portal = new BlockPos(anchor.getInt("x"), anchor.getInt("y"), anchor.getInt("z"));
        // Load the destination chunk and choose a safe, unobstructed adjacent landing.
        overworld.getChunkAt(portal);
        BlockPos landing = null;
        for (int dx = -1; dx <= 1 && landing == null; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos feet = portal.offset(dx, 0, dz);
                if (overworld.getBlockState(feet.below()).is(Blocks.DARK_PRISMARINE)
                        && overworld.getBlockState(feet).isAir()
                        && overworld.getBlockState(feet.above()).isAir()) {
                    landing = feet;
                    break;
                }
            }
        }
        if (landing == null) {
            player.displayClientMessage(Component.literal("No safe landing beside your temple portal; return cancelled."), true);
            return false;
        }
        boolean moved = player.teleportTo(overworld, landing.getX() + .5, landing.getY(), landing.getZ() + .5,
                Set.of(), player.getYRot(), player.getXRot());
        if (moved) {
            restoreMode(player);
            com.devaxo.templeoftheaxolotl.traps.TempleProgress.finishRun(player);
            // The server tick schedules a delayed deep clean after the final exit.
            // Do not invoke snapshot-based undo here.
            player.getPersistentData().remove(ANCHOR);
            player.removeTag(TempleDimensionManager.IN_TEMPLE_TAG);
            player.removeTag(TempleDimensionManager.RETURN_PENDING_TAG);
        }
        return moved;
    }

    /** Restore an abandoned visitor to their own recorded Overworld entrance. */
    public static boolean returnAbandoned(ServerPlayer player) {
        if (!player.getPersistentData().contains(ANCHOR)) return false;
        if (player.serverLevel().dimension() != TempleDimensionManager.TEMPLE_DIMENSION) {
            player.removeTag(TempleDimensionManager.RETURN_PENDING_TAG);
            player.removeTag(TempleDimensionManager.IN_TEMPLE_TAG);
            player.getPersistentData().remove(ANCHOR);
            restoreMode(player);
            com.devaxo.templeoftheaxolotl.traps.TempleProgress.finishRun(player);
            return true;
        }
        return exit(player);
    }

    private static boolean noTaggedOccupants(ServerLevel dungeon) {
        return dungeon.players().stream().noneMatch(p -> p.getTags().contains(TempleDimensionManager.IN_TEMPLE_TAG));
    }

    /** Seal the portals immediately, then run the same deep-clean worker as /templefullreset. */
    private static void sealSessionPortals(MinecraftServer server) {
        TemplePortalRegistry registry = TemplePortalRegistry.get(server.overworld());
        for (BlockPos portal : Set.copyOf(SESSION_PORTALS)) {
            registry.consume(portal);
            // The return landing ring must survive. Only remove the portal block.
            ServerLevel overworld = server.overworld();
            overworld.getChunkAt(portal);
            if (overworld.getBlockState(portal).is(
                    com.devaxo.templeoftheaxolotl.registry.ModBlocks.TEMPLE_PORTAL.get())) {
                overworld.removeBlock(portal, false);
            }
        }
        SESSION_PORTALS.clear();
    }

    private static void checkEmptyTemple(MinecraftServer server) {
        ServerLevel dungeon = server.getLevel(TempleDimensionManager.TEMPLE_DIMENSION);
        if (dungeon == null) return;
        if (closingThisSession) {
            if (!AxoMazeCommands.isMazeOccupiedByBuild(dungeon)) {
                closingThisSession = false;
                visitedThisSession = false;
                cleanupDeadline = -1L;
            }
            return;
        }
        if (!visitedThisSession || !WAITING.isEmpty() || !AxoMazeCommands.isBuildComplete(dungeon)) return;
        // Never demolish while ANY player is physically present, even untagged.
        if (!noTaggedOccupants(dungeon) || !dungeon.players().isEmpty()) {
            cleanupDeadline = -1L;
            return;
        }
        if (cleanupDeadline < 0L) {
            sealSessionPortals(server);
            cleanupDeadline = server.overworld().getGameTime() + CLEANUP_DELAY_TICKS;
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "Last Crawler Exited: Commencing Temple Deep Cleaning in 5 Seconds"), false);
            return;
        }
        if (server.overworld().getGameTime() < cleanupDeadline) return;
        if (AxoMazeCommands.beginAutomaticFullReset(dungeon)) {
            closingThisSession = true;
            cleanupDeadline = -1L;
        }
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tota_devportal")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (player.serverLevel().dimension() != Level.OVERWORLD) {
                        ctx.getSource().sendFailure(Component.literal("Create test portals in the Overworld."));
                        return 0;
                    }
                    BlockPos pos = player.blockPosition().relative(player.getDirection(), 3).above();
                    ServerLevel level = player.serverLevel();
                    // Full 3x3 dark-prismarine landing ring at feet-level - 1.
                    // Never overwrite terrain, structures, or another portal.
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos floor = pos.offset(dx, -1, dz);
                            BlockPos feet = floor.above();
                            if (!level.getBlockState(floor).isAir()
                                    || !level.getBlockState(feet).isAir()
                                    || !level.getBlockState(feet.above()).isAir()) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "Dev portal needs a clear 3x3 platform and two blocks of headroom."));
                                return 0;
                            }
                        }
                    }
                    if (TemplePortalRegistry.get(level).isConsumed(pos)) {
                        ctx.getSource().sendFailure(Component.literal("This portal position is retired."));
                        return 0;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            level.setBlock(pos.offset(dx, -1, dz), Blocks.DARK_PRISMARINE.defaultBlockState(), 3);
                        }
                    }
                    level.setBlock(pos, com.devaxo.templeoftheaxolotl.registry.ModBlocks.TEMPLE_PORTAL.get().defaultBlockState(), 3);
                    ctx.getSource().sendSuccess(() -> Component.literal("Valid test portal created at " + pos.toShortString()), false);
                    return 1;
                }));
        event.getDispatcher().register(Commands.literal("templeexit")
                .requires(source -> source.hasPermission(2)) // temporary developer test command
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (player.serverLevel().dimension().equals(OriginTempleState.DIMENSION))
                        return OriginTempleTravel.devExit(player) ? 1 : 0;
                    if (!player.serverLevel().dimension().equals(TempleDimensionManager.TEMPLE_DIMENSION)) return 0;
                    return exit(player) ? 1 : 0;
                }));
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (WAITING.remove(player.getUUID())) player.getPersistentData().remove(ANCHOR);
        if (player.serverLevel().dimension() == TempleDimensionManager.TEMPLE_DIMENSION) {
            player.removeTag(TempleDimensionManager.IN_TEMPLE_TAG);
            player.addTag(TempleDimensionManager.RETURN_PENDING_TAG);
        }
    }

    @SubscribeEvent
    public void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.serverLevel().dimension() != TempleDimensionManager.TEMPLE_DIMENSION) return;
        player.removeTag(TempleDimensionManager.IN_TEMPLE_TAG);
        player.addTag(TempleDimensionManager.RETURN_PENDING_TAG);
        com.devaxo.templeoftheaxolotl.loot.DungeonRewardInventory.fail(player);
    }

    @SubscribeEvent
    public void clonePlayer(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)
                || !(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        if (!oldPlayer.getTags().contains(TempleDimensionManager.RETURN_PENDING_TAG)) return;
        newPlayer.addTag(TempleDimensionManager.RETURN_PENDING_TAG);
        if (oldPlayer.getPersistentData().contains(ANCHOR))
            newPlayer.getPersistentData().put(ANCHOR, oldPlayer.getPersistentData().getCompound(ANCHOR).copy());
        if (oldPlayer.getPersistentData().contains(ORIGINAL_MODE))
            newPlayer.getPersistentData().putInt(ORIGINAL_MODE,
                    oldPlayer.getPersistentData().getInt(ORIGINAL_MODE));
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getTags().contains(TempleDimensionManager.RETURN_PENDING_TAG)) return;
        player.getServer().execute(() -> returnAbandoned(player));
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (startupRecoveryAnnounced) {
            player.sendSystemMessage(Component.literal("The Temple Hungers"));
        }
        if (player.getTags().contains(TempleDimensionManager.RETURN_PENDING_TAG)) {
            // Defer one tick to avoid teleporting from inside the login event.
            player.getServer().execute(() -> returnAbandoned(player));
        }
    }
}
