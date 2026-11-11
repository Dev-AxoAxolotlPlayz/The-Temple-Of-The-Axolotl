package com.devaxo.templeoftheaxolotl.abyss;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side first-pass portal travel. Does not modify the existing Origin dungeon. */
public final class AbyssTrialEvents {
    public static final ResourceKey<Level> ABYSS = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "ethereal_abyss"));
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final java.util.Set<UUID> announced = new java.util.HashSet<>();
    private long ticks;
    private int preparationStep;
    private boolean preparing;
    private boolean ready;
    private int failedPreparationTicks;
    private int awakeningStage;
    private long shutdownStart = -1;
    // Independent per-chunk statuses: 0=not requested, 1=attempting, 2=verified.
    private final int[] chunkStatuses = new int[4];
    // Per-quadrant progress; the Watergate still renders four diagnostic beam pairs.
    private long chunkAttemptTick = -1;
    private int reportedProgress = -1;
    private int encodedProgress() {
        int result = 0;
        for (int i = 0; i < 4; i++) result |= (chunkStatuses[i] << (i * 2));
        return result;
    }
    private static final BlockPos ARRIVAL = new BlockPos(0, 201, 0);
    private static final int[][] ARRIVAL_CHUNKS = {
        {-2,-2},{-2,-1},{-1,-2},{-1,-1}, // NW
        {0,-2},{0,-1},{1,-2},{1,-1},     // NE
        {-2,0},{-2,1},{-1,0},{-1,1},     // SW
        {0,0},{0,1},{1,0},{1,1}          // SE
    };

    @SubscribeEvent public void tick(ServerTickEvent.Post event) {
        if (++ticks % 5 != 0) return;
        MinecraftServer server = event.getServer();
        ServerLevel abyss = server.getLevel(ABYSS);
        if (abyss == null) return;
        // The 3x3 sea-lantern marker is persistent. No per-restart island rebuild.
        if (!ready && !preparing && shutdownStart < 0 && preparationStep == 0 && platformPresent(abyss)) {
            ready = true;
            awakeningStage = 14;
            TempleOfTheAxolotl.LOGGER.info("Abyss arrival: previously completed platform detected; portal ready");
        }
        // A playing Cosmic Seas disc at an existing Watergate starts preparation,
        // BEFORE the player walks through the portal. The player remains in Overworld.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel().dimension() != Level.OVERWORLD) continue;
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6,-4,-6),center.offset(6,4,6))) {
                BlockState state = player.serverLevel().getBlockState(pos);
                if (!state.is(ModBlocks.ABYSSAL_WATERGATE.get())) continue;
                Direction facing = state.getValue(WatergateBlock.FACING);
                BlockPos jukebox = pos.relative(facing,2);
                boolean keyPlaying = player.serverLevel().getBlockEntity(jukebox) instanceof JukeboxBlockEntity jb
                        && jb.getSongPlayer().isPlaying()
                        && jb.getTheItem().is(ModItems.DISC_COSMIC_SEAS.get());
                boolean portalActive = ready && awakeningStage == 14 && keyPlaying;
                int progress = encodedProgress();
                if (player.serverLevel().getBlockEntity(pos) instanceof WatergateEntity gate) {
                    gate.setProgress(awakeningStage, progress);
                }
                if (state.getValue(WatergateBlock.READY) != portalActive) {
                    player.serverLevel().setBlock(pos, state.setValue(WatergateBlock.READY, portalActive), 3);
                }
                if (!keyPlaying && !ready) continue;
                if (!ready && keyPlaying) {
                    preparing = true;
                    if (announced.add(player.getUUID())) player.displayClientMessage(
                            Component.literal("The music bends the seas... something ancient can be felt..."),true);
                }
                if (cooldowns.getOrDefault(player.getUUID(),0L) <= ticks && insidePortal(player,pos,facing)) {
                    if (!ready) {
                        player.displayClientMessage(Component.literal("The Ethereal Abyss is still awakening..."),true);
                    } else if (!keyPlaying) {
                        player.displayClientMessage(Component.literal("The Watergate needs the Cosmic Seas disc playing."), true);
                    } else if (player.teleportTo(abyss,.5,202,.5,java.util.Set.of(),player.getYRot(),player.getXRot())) {
                        cooldowns.put(player.getUUID(),ticks+80);
                        giveAnchor(player);
                        // Existing entrance action-bar wording is intentionally unchanged.
                        player.displayClientMessage(Component.literal("The Cosmic and the Seas awaken the Ethereal Abyss."),true);
                    }
                }
                break;
            }
        }
        // Process one initial destination chunk per server tick batch. The terrain
        // painter owns its own completion markers; never construct the platform early.
        if (shutdownStart >= 0 && !ready) {
            int elapsed = (int)(ticks - shutdownStart);
            int next = elapsed >= 100 ? 14 : 9 + elapsed / 20;
            if (next != awakeningStage) {
                awakeningStage = next;
                TempleOfTheAxolotl.LOGGER.info("Abyss Watergate awakening stage {}", awakeningStage);
            }
            if (elapsed >= 100) {
                ready = true;
                preparing = false;
                shutdownStart = -1;
            }
        }
        if (preparing && !ready && shutdownStart < 0) {
            if (preparationStep < ARRIVAL_CHUNKS.length) {
                int index = preparationStep;
                int quadrant = index / 4;
                int[] cp = ARRIVAL_CHUNKS[index];
                // Send a distinct violet state before attempting synchronous terrain painting.
                // This also makes the state visible for at least one server-tick batch.
                if (chunkStatuses[quadrant] == 0) {
                    chunkStatuses[quadrant] = 1;
                    chunkAttemptTick = ticks;
                    TempleOfTheAxolotl.LOGGER.info("Abyss arrival: START chunk {},{} (pair {} violet)", cp[0], cp[1], quadrant + 1);
                } else if (ticks > chunkAttemptTick) {
                    try {
                        boolean painted = AbyssTerrainEvents.ensureArrivalChunk(abyss, cp[0], cp[1]);
                        boolean verified = painted && abyss.getBlockState(
                                new BlockPos(cp[0] * 16, -63, cp[1] * 16)).is(Blocks.OBSIDIAN);
                        if (verified) {
                            preparationStep++;
                            if (preparationStep % 4 == 0) chunkStatuses[quadrant] = 2;
                            chunkAttemptTick = -1;
                            failedPreparationTicks = 0;
                            TempleOfTheAxolotl.LOGGER.info("Abyss arrival: VERIFIED chunk {},{} (pair {} cyan)",
                                    cp[0], cp[1], quadrant + 1);
                        } else if (++failedPreparationTicks % 20 == 1) {
                            TempleOfTheAxolotl.LOGGER.warn("Abyss arrival: WAIT chunk {},{}; painter={}, marker={}",
                                    cp[0], cp[1], painted, abyss.getBlockState(
                                            new BlockPos(cp[0] * 16, -63, cp[1] * 16)).is(Blocks.OBSIDIAN));
                        }
                    } catch (Exception ex) {
                        if (++failedPreparationTicks % 20 == 1)
                            TempleOfTheAxolotl.LOGGER.error("Abyss arrival: FAILED chunk {},{}", cp[0], cp[1], ex);
                    }
                }
            } else {
                if (terrainReady(abyss)) {
                    buildArrivalPlatform(abyss);
                    if (!platformPresent(abyss)) {
                        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                            BlockPos check = ARRIVAL.offset(x, 0, z);
                            if (!abyss.getBlockState(check).is(Blocks.SEA_LANTERN))
                                TempleOfTheAxolotl.LOGGER.error("Abyss arrival: PLATFORM VERIFY FAILED at {}: {}",
                                        check, abyss.getBlockState(check));
                        }
                    }
                    if (platformPresent(abyss)) {
                        awakeningStage = 9; // Fifth pair lights, shutdown starts
                        shutdownStart = ticks;
                        preparing = false;
                        TempleOfTheAxolotl.LOGGER.info("Abyss arrival: platform complete; Watergate ready");
                    }
                } else {
                    // Revisit missing markers instead of waiting indefinitely.
                    preparationStep = 0;
                    // Preserve verified chunks; retry only the first missing marker.
                    for (int i = 0; i < ARRIVAL_CHUNKS.length; i++) {
                        int[] cp = ARRIVAL_CHUNKS[i];
                        if (!abyss.getBlockState(new BlockPos(cp[0]*16,-63,cp[1]*16)).is(Blocks.OBSIDIAN)) {
                            preparationStep = i;
                            chunkStatuses[i / 4] = 1;
                            chunkAttemptTick = ticks - 1;
                            break;
                        }
                    }
                    TempleOfTheAxolotl.LOGGER.warn("Abyss arrival: marker mismatch; rechecking incomplete chunk (verified beam pairs remain cyan)");
                }
            }
        }
    }

    private static boolean terrainReady(ServerLevel level) {
        for (int[] cp : ARRIVAL_CHUNKS) {
            // hasChunk() only reports chunks currently cached/loaded, not whether they
            // were generated. Reload the exact chunk before checking its persistent marker.
            level.getChunk(cp[0], cp[1]);
            BlockPos marker = new BlockPos(cp[0]*16,-63,cp[1]*16);
            if (!level.getBlockState(marker).is(Blocks.OBSIDIAN)) return false;
        }
        return true;
    }

    private static boolean platformPresent(ServerLevel level) {
        if (!terrainReady(level)) return false;
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++)
            if (!level.getBlockState(ARRIVAL.offset(x,0,z)).is(Blocks.SEA_LANTERN)) return false;
        return true;
    }

    private static void buildArrivalPlatform(ServerLevel level) {
        if (!terrainReady(level)) return;
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            BlockPos pos = ARRIVAL.offset(x,0,z);
            level.setBlock(pos,Blocks.SEA_LANTERN.defaultBlockState(),3);
            // Only clear the two player-height blocks directly above the platform.
            level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
            level.setBlock(pos.above(2),Blocks.AIR.defaultBlockState(),3);
        }
    }

    private static void giveAnchor(ServerPlayer player) {
        var anchor = new net.minecraft.world.item.ItemStack(ModBlocks.ABYSS_EXTRACTION_ANCHOR_ITEM.get());
        if (!player.getInventory().add(anchor)) player.drop(anchor,false);
    }

    /** Used only after the personal extraction anchor has validated its owner. */
    public static boolean extract(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || player.serverLevel().dimension() != ABYSS) return false;
        ServerLevel home = server.overworld();
        BlockPos safe = null;
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null && player.getRespawnDimension() == Level.OVERWORLD
                && home.getBlockState(respawn).is(net.minecraft.tags.BlockTags.BEDS)) {
            safe = safeStandNear(home,respawn,2);
        }
        if (safe == null) {
            BlockPos spawn = home.getSharedSpawnPos();
            safe = safeStandNear(home,spawn,12);
            if (safe == null) safe = safeStandNear(home,home.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn),12);
        }
        if (safe == null) {
            player.sendSystemMessage(Component.literal("Extraction failed: no safe Overworld destination found."));
            return false;
        }
        return player.teleportTo(home,safe.getX()+.5,safe.getY(),safe.getZ()+.5,
                java.util.Set.of(),player.getYRot(),player.getXRot());
    }
    /** Search the nearby standing spaces; never substitute a hardcoded Y coordinate. */
    private static BlockPos safeStandNear(ServerLevel level, BlockPos anchor, int radius) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos feet = anchor.offset(dx, dy, dz);
                    if (feet.getY() <= level.getMinBuildHeight()+1 || feet.getY() >= level.getMaxBuildHeight()-2) continue;
                    var below = level.getBlockState(feet.below());
                    if (!below.isFaceSturdy(level, feet.below(), Direction.UP)) continue;
                    if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) continue;
                    if (!level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
                    if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
                    if (below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CAMPFIRE)
                            || below.is(Blocks.SOUL_CAMPFIRE)) continue;
                    return feet;
                }
            }
        }
        return null;
    }
    /** Three-wide, four-tall, 2/16-block-thick plane centered on the controller. */
    private static boolean insidePortal(ServerPlayer player, BlockPos pos, Direction facing) {
        double dx = player.getX() - (pos.getX() + .5);
        double dz = player.getZ() - (pos.getZ() + .5);
        double lateral = facing.getAxis() == Direction.Axis.Z ? dx : dz;
        double depth = facing.getAxis() == Direction.Axis.Z ? dz : dx;
        return Math.abs(lateral) <= 1.5 && Math.abs(depth) <= .22
                && player.getBoundingBox().maxY > pos.getY()
                && player.getBoundingBox().minY < pos.getY() + 4;
    }

}
