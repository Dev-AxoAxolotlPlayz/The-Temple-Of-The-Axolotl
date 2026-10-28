package com.devaxo.templeoftheaxolotl.loot;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.network.chat.ClickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import com.devaxo.templeoftheaxolotl.maze.TempleTravel;
import com.devaxo.templeoftheaxolotl.maze.TempleDimensionManager;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** DEV ONLY: reward outcome simulator. Does not teleport or reset the dungeon. */
public final class DungeonRewardDevCommands {
    private static final int EXTRACTION_TICKS = 600;
    private static final int GRACE_TICKS = 200;
    private record BossSession(UUID temple, long deadline, BlockPos rewardPos) {} // deadline includes 10-second grace and 30-second extraction
    private static final Map<UUID, BossSession> SESSIONS = new HashMap<>();
    private static MinecraftServer currentServer;

    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (currentServer != server) { currentServer = server; SESSIONS.clear(); }
        long now = server.overworld().getGameTime();
        for (UUID id : java.util.List.copyOf(SESSIONS.keySet())) {
            BossSession session = SESSIONS.get(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (session == null || player == null || player.serverLevel().dimension() != TempleDimensionManager.TEMPLE_DIMENSION) {
                SESSIONS.remove(id);
                continue;
            }
            long remaining = Math.max(0, session.deadline() - now);
            if (remaining > EXTRACTION_TICKS) {
                player.displayClientMessage(Component.literal("Collect the Boss Shulker! Extraction starts in " + ((remaining - EXTRACTION_TICKS + 19) / 20) + "s"), true);
            } else {
                player.displayClientMessage(Component.literal("Sanctum extraction: " + ((remaining + 19) / 20) + "s"), true);
            }
            if (remaining == 0 && TempleTravel.exit(player)) {
                DungeonRewardInventory.succeed(player);
                SESSIONS.remove(id);
            }
        }
    }

    /** Production boss victory entry point, reusing the existing reward and extraction timer. */
    public static void completeSanctumVictory(ServerLevel level, net.minecraft.world.entity.Entity boss) {
        if (level.dimension() != TempleDimensionManager.TEMPLE_DIMENSION) return;
        // The existing system is per-player. Only reward participants near the defeated boss.
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive() || player.distanceToSqr(boss) > 96.0 * 96.0) continue;
            bossKill(player, boss.blockPosition());
        }
    }

    private static int bossKill(ServerPlayer player) {
        return bossKill(player, player.blockPosition());
    }

    private static int bossKill(ServerPlayer player, BlockPos rewardOrigin) {
        ServerLevel level = player.serverLevel();
        if (level.dimension() != TempleDimensionManager.TEMPLE_DIMENSION) {
            player.sendSystemMessage(Component.literal("BossKill must be run inside the temple dimension."));
            return 0;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Boss reward already generated for this extraction."));
            return 0;
        }
        BlockPos found = null;
        BlockPos origin = rewardOrigin;
        for (int radius = 0; radius <= 6 && found == null; radius++) {
            for (int dx = -radius; dx <= radius && found == null; dx++) {
                for (int dz = -radius; dz <= radius && found == null; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        BlockPos below = pos.below();
                        if (!level.getBlockState(below).isCollisionShapeFullBlock(level, below)) continue;
                        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                        found = pos;
                        break;
                    }
                }
            }
        }
        if (found == null) {
            player.sendSystemMessage(Component.literal("No safe full-block floor found for the boss reward."));
            return 0;
        }
        if (!level.setBlock(found, Blocks.PURPLE_SHULKER_BOX.defaultBlockState(), 3)) return 0;
        if (!(level.getBlockEntity(found) instanceof RandomizableContainerBlockEntity box)) {
            level.removeBlock(found, false);
            return 0;
        }
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "chests/sanctum_boss_reward_phase62"));
        box.setLootTable(key, level.random.nextLong());
        box.setChanged();
        SESSIONS.put(player.getUUID(), new BossSession(player.getUUID(), player.getServer().overworld().getGameTime() + GRACE_TICKS + EXTRACTION_TICKS, found));
        player.sendSystemMessage(Component.literal("Boss defeated. Reward Shulker placed at " + found.toShortString() + "."));
        player.sendSystemMessage(Component.literal("You have 10 seconds before the 30-second extraction countdown begins!"));
        player.sendSystemMessage(Component.literal("[Extract Now]").withStyle(style -> style
                .withColor(net.minecraft.ChatFormatting.GREEN)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/temple extractnow"))));
        return 1;
    }

    private static int extractNow(ServerPlayer player) {
        BossSession session = SESSIONS.get(player.getUUID());
        if (session == null || player.serverLevel().dimension() != TempleDimensionManager.TEMPLE_DIMENSION) return 0;
        if (!TempleTravel.exit(player)) return 0;
        DungeonRewardInventory.succeed(player);
        SESSIONS.remove(player.getUUID());
        return 1;
    }

    @SubscribeEvent
    public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("temple")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("BossKill").executes(ctx -> bossKill(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("extractnow").executes(ctx -> extractNow(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("success").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int slots = DungeonRewardInventory.occupied(player);
                    DungeonRewardInventory.succeed(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Temple SUCCESS test: claimed " + slots + " reward stacks. No teleport/reset."), false);
                    return 1;
                }))
                .then(Commands.literal("failed").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int slots = DungeonRewardInventory.occupied(player);
                    DungeonRewardInventory.fail(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Temple FAILED test: forfeited " + slots + " staged reward stacks. No teleport/reset."), false);
                    return 1;
                }))
                .then(Commands.literal("rewards").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    TempleRewardMenus.open(player);
                    return 1;
                }))
                .then(Commands.literal("bindtest").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (DungeonRewardInventory.occupied(player) != 0) {
                        ctx.getSource().sendFailure(Component.literal("Clear staging before binding a test run."));
                        return 0;
                    }
                    DungeonRewardInventory.bind(player, java.util.UUID.randomUUID(), 12345L);
                    ctx.getSource().sendSuccess(() -> Component.literal("Bound a simulated temple run (seed 12345)."), false);
                    return 1;
                }))
                .then(Commands.literal("testloot").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack sample = new ItemStack(Items.DIAMOND, 3);
                    if (!DungeonRewardInventory.add(player, sample)) {
                        ctx.getSource().sendFailure(Component.literal(
                                "Dungeon reward staging is full; test loot was not inserted."));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Added 3 test diamonds to dungeon reward staging. Use /temple status, success, or failed."), false);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int slots = DungeonRewardInventory.occupied(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Dungeon reward staging: " + slots + "/" + DungeonRewardInventory.SLOTS + " occupied slots. " + DungeonRewardInventory.bindingStatus(player)), false);
                    return 1;
                })));
    }
}
