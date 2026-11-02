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
    private long ticks;
    private boolean islandBuilt;

    @SubscribeEvent public void tick(ServerTickEvent.Post event) {
        if (++ticks % 5 != 0) return;
        MinecraftServer server = event.getServer();
        ServerLevel abyss = server.getLevel(ABYSS);
        if (abyss == null) return;
        if (!islandBuilt) { buildArrivalIsland(abyss); islandBuilt = true; }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (cooldowns.getOrDefault(player.getUUID(), 0L) > ticks) continue;
            if (player.serverLevel().dimension() == ABYSS) {
                BlockPos returnGatePos = new BlockPos(0, 201, 5);
                BlockState returnGateState = abyss.getBlockState(returnGatePos);
                // The return gate may be absent or replaced by geology; never read FACING from another block.
                if (returnGateState.is(ModBlocks.ABYSSAL_WATERGATE.get())
                        && returnGateState.hasProperty(WatergateBlock.FACING)
                        && insidePortal(player, returnGatePos, returnGateState.getValue(WatergateBlock.FACING))) {
                    ServerLevel home = server.overworld();
                    BlockPos respawn = player.getRespawnPosition();
                    if (respawn == null || player.getRespawnDimension() != Level.OVERWORLD) respawn = home.getSharedSpawnPos();
                    BlockPos safe = home.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, respawn);
                    if (player.teleportTo(home, safe.getX()+.5, safe.getY()+1, safe.getZ()+.5,
                            java.util.Set.of(), player.getYRot(), player.getXRot())) cooldowns.put(player.getUUID(), ticks + 80);
                }
                continue;
            }
            if (player.serverLevel().dimension() != Level.OVERWORLD) continue;
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3,-3,-3), center.offset(3,3,3))) {
                BlockState state = player.serverLevel().getBlockState(pos);
                if (!state.is(ModBlocks.ABYSSAL_WATERGATE.get())) continue;
                if (!insidePortal(player, pos, state.getValue(WatergateBlock.FACING))) continue;
                Direction facing = state.getValue(WatergateBlock.FACING);
                BlockPos jukebox = pos.relative(facing,2);
                if (!(player.serverLevel().getBlockEntity(jukebox) instanceof JukeboxBlockEntity jb)) continue;
                if (!jb.getSongPlayer().isPlaying()
                        || !jb.getTheItem().is(ModItems.DISC_COSMIC_SEAS.get())) {
                    continue;
                }
                if (player.teleportTo(abyss, .5, 202, .5, java.util.Set.of(), player.getYRot(), player.getXRot())) {
                    cooldowns.put(player.getUUID(), ticks + 80);
                    player.displayClientMessage(Component.literal("The Cosmic and the Seas awaken the Ethereal Abyss."), true);
                }
                break;
            }
        }
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

    private void buildArrivalIsland(ServerLevel level) {
        // The island is anchored at 0,0 and guarded by a physical marker to survive restarts.
        if (level.getBlockState(new BlockPos(0,198,0)).is(Blocks.DARK_PRISMARINE)) return;
        for (int x=-13; x<=13; x++) for (int z=-13; z<=13; z++) {
            double d = Math.sqrt(x*x+z*z);
            if (d > 12.5 + 1.1*Math.sin(x*1.4)*Math.cos(z*.9)) continue;
            for (int y=192; y<=200; y++) {
                if (y < 200 - Math.max(2, (int)((13-d)*.65))) continue;
                level.setBlock(new BlockPos(x,y,z), y==200 ? Blocks.DARK_PRISMARINE.defaultBlockState()
                        : Blocks.PRISMARINE.defaultBlockState(), 2);
            }
        }
        // Deliberately clear an arrival platform; avoid suffocation when teleporting.
        for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++)
            for (int y=201;y<=204;y++) level.setBlock(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(new BlockPos(0,198,0), Blocks.DARK_PRISMARINE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(0,201,5), ModBlocks.ABYSSAL_WATERGATE.get().defaultBlockState(), 3);
    }
}
