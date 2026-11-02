package com.devaxo.templeoftheaxolotl.maze;

import com.devaxo.templeoftheaxolotl.block.TemplePortalBlock;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Independent Origin travel and queued safe arrival. */
public final class OriginTempleTravel {
    private static final Set<UUID> WAITING = new HashSet<>();
    private static MinecraftServer serverSeen;
    private static final double X = 22.5, Y = 202, Z = 26.5;
    public static boolean enter(ServerPlayer player, BlockPos portal, ItemStack stack) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return false;
        if (!stack.is(ModItems.CELESTIAL_KEY.get())) {
            player.displayClientMessage(Component.literal("A Celestial Key is required."), true); return false;
        }
        MinecraftServer server = player.getServer();
        if (server != null && OriginMazeCommands.isWipePending(server)) {
            player.displayClientMessage(Component.literal(
                    "Fractured Origin reset is queued. Restart the world before reentering."), true);
            return false;
        }

        ServerLevel origin = server == null ? null : server.getLevel(OriginTempleState.DIMENSION);
        if (origin == null || WAITING.contains(player.getUUID())) return false;
        OriginTempleState data = OriginTempleState.get(origin);
        if (!data.started()) {
            long seed = new Random().nextLong();
            data.lockTrigger(player);
            data.start(seed);
            if (OriginMazeCommands.generateInTemple(seed, player.createCommandSourceStack()) == 0) {
                player.displayClientMessage(Component.literal("Origin Temple Generation could not start."), true);
                return false;
            }
        } else if (!data.complete() && !OriginMazeCommands.isMazeOccupiedByBuild(origin)) {
            if (OriginMazeCommands.generateInTemple(data.seed(), player.createCommandSourceStack()) == 0) return false;
        }
        if (!player.isCreative()) stack.shrink(1);
        player.serverLevel().setBlock(portal, player.serverLevel().getBlockState(portal)
                .setValue(TemplePortalBlock.ORIGIN, true), 3);
        OriginPortalRegistry.get(player.serverLevel()).convert(portal);
        WAITING.add(player.getUUID());
        player.displayClientMessage(Component.literal("Origin Temple Generation: preparing entrance..."), true);
        return true;
    }
    /** Developer-only emergency return; never triggers regular Temple cleanup. */
    public static boolean devExit(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(OriginTempleState.DIMENSION)) return false;
        ServerLevel overworld = player.getServer().overworld();
        if (overworld == null) return false;
        BlockPos spawn = overworld.getSharedSpawnPos();
        overworld.getChunkAt(spawn);
        // Use the surface at world spawn, rather than teleporting to an arbitrary Y.
        BlockPos landing = overworld.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
        if (!overworld.getBlockState(landing).isAir()
                || !overworld.getBlockState(landing.above()).isAir()) return false;
        if (!player.teleportTo(overworld, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot())) return false;
        var data = player.getPersistentData();
        if (data.contains("tota_origin_previous_mode")) {
            player.setGameMode(GameType.byId(data.getInt("tota_origin_previous_mode")));
            data.remove("tota_origin_previous_mode");
        }
        player.displayClientMessage(Component.literal("Exited Fractured Origin (developer escape)."), false);
        return true;
    }

    public static void cancelPendingEntries() { WAITING.clear(); }

    @SubscribeEvent public void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (serverSeen != server) { serverSeen = server; WAITING.clear(); }
        ServerLevel origin = server.getLevel(OriginTempleState.DIMENSION);
        if (origin == null) return;
        OriginTempleState data = OriginTempleState.get(origin);
        if (data.started() && !data.complete() && !OriginMazeCommands.isMazeOccupiedByBuild(origin)) {
            ServerPlayer source = server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
            if (source != null) OriginMazeCommands.generateInTemple(data.seed(), source.createCommandSourceStack());
        }
        if (!data.complete() || WAITING.isEmpty()) return;
        BlockPos floor = BlockPos.containing(X, Y - 1, Z);
        if (!origin.getBlockState(floor).is(Blocks.EMERALD_BLOCK)
                || !origin.getBlockState(floor.above()).isAir()
                || !origin.getBlockState(floor.above(2)).isAir()) return;
        for (UUID id : Set.copyOf(WAITING)) {
            WAITING.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || player.serverLevel().dimension() != Level.OVERWORLD) continue;
            if (player.teleportTo(origin, X, Y, Z, Set.of(), player.getYRot(), player.getXRot())) {
                player.getPersistentData().putInt("tota_origin_previous_mode", player.gameMode.getGameModeForPlayer().getId());
                player.setGameMode(GameType.ADVENTURE);
                player.displayClientMessage(Component.literal("Fractured Origin"), true);
            }
        }
    }
}
