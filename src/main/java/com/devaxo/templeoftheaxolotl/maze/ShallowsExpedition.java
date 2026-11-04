package com.devaxo.templeoftheaxolotl.maze;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** Separate three-floor / 5x5 introductory dungeon; never calls regular Temple reset. */
public final class ShallowsExpedition {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID, "the_shallows"));
    private static final String RETURN = "tota_shallows_return";
    private static final String MODE = "tota_shallows_mode";
    private static final BlockPos ORIGIN = new BlockPos(0, 200, 0);
    private static final int WIDTH = 5, FLOORS = 3, CELL = 15, HEIGHT = 16;
    private static final int PER_TICK = 3;
    private static final Set<UUID> QUEUED = new HashSet<>();
    private static final List<Task> TASKS = new ArrayList<>();
    private static MinecraftServer owner;
    private static int cursor;
    private static boolean built;
    private static int idleTicks;
    private static boolean clearing;
    private static int clearCursor;
    private record Task(BlockPos pos, String template, Rotation rotation) {}

    public static void enter(ServerPlayer player, BlockPos portal) {
        if (player.serverLevel().dimension() != Level.OVERWORLD) return;
        if (player.getPersistentData().contains(RETURN)) {
            player.displayClientMessage(Component.literal("You already have a Shallows visit."), true); return;
        }
        ServerLevel level = player.getServer().getLevel(DIMENSION);
        if (level == null || clearing) {
            player.displayClientMessage(Component.literal("The Shallows are unavailable or resetting."), true); return;
        }
        CompoundTag returnTag = new CompoundTag();
        returnTag.putInt("x", portal.getX()); returnTag.putInt("y", portal.getY()); returnTag.putInt("z", portal.getZ());
        player.getPersistentData().put(RETURN, returnTag);
        QUEUED.add(player.getUUID());
        if (!built && TASKS.isEmpty()) prepare();
        player.displayClientMessage(Component.literal("The Shallows are preparing their chambers..."), true);
    }

    private static void prepare() {
        TASKS.clear(); cursor = 0;
        // A fully connected orthogonal floor grid: corners, T junctions and fourways.
        // The entrance replaces one ordinary cell on floor one; the exit is on floor three.
        for (int floor = 0; floor < FLOORS; floor++) {
            for (int z = 0; z < WIDTH; z++) for (int x = 0; x < WIDTH; x++) {
                int y = ORIGIN.getY() - floor * HEIGHT;
                BlockPos pos = new BlockPos(ORIGIN.getX()+x*CELL, y, ORIGIN.getZ()+z*CELL);
                String name; Rotation rotation;
                if (floor == 0 && x == 1 && z == 1) { name="shallows_chamber_entrance"; rotation=Rotation.NONE; }
                else if (floor == FLOORS-1 && x == 3 && z == 3) { name="shallows_deadend_exit"; rotation=Rotation.NONE; }
                else if ((x==0 || x==WIDTH-1) && (z==0 || z==WIDTH-1)) {
                    name="tota_corner";
                    if (x==0 && z==0) rotation=Rotation.NONE; // base E,S
                    else if (x==WIDTH-1 && z==0) rotation=Rotation.CLOCKWISE_90;
                    else if (x==WIDTH-1) rotation=Rotation.CLOCKWISE_180;
                    else rotation=Rotation.COUNTERCLOCKWISE_90;
                } else if (x==0 || x==WIDTH-1 || z==0 || z==WIDTH-1) {
                    name="tota_tjunction"; // base W,E,S
                    if (z==0) rotation=Rotation.NONE;
                    else if (x==WIDTH-1) rotation=Rotation.CLOCKWISE_90;
                    else if (z==WIDTH-1) rotation=Rotation.CLOCKWISE_180;
                    else rotation=Rotation.COUNTERCLOCKWISE_90;
                } else { name="tota_fourway"; rotation=Rotation.NONE; }
                TASKS.add(new Task(pos, name, rotation));
            }
        }
        // Vertical traversal: bubble elevators bridge consecutive 16-block layers.
        // Replace the two matching floor cells with a single 32-high template each.
        // A fresh, non-world-seeded choice for every expedition. Both shafts stay
        // on interior cells, avoiding the fixed entrance and exit templates.
        // Each floor's orthogonal grid remains connected, so either shaft is reachable.
        Random random = new Random();
        List<int[]> candidates = new ArrayList<>();
        for (int z = 1; z < WIDTH - 1; z++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                if ((x == 1 && z == 1) || (x == 3 && z == 3)) continue;
                candidates.add(new int[]{x, z});
            }
        }
        Collections.shuffle(candidates, random);
        int[] first = candidates.get(0);
        int[] second = candidates.get(1);
        replaceWithElevator(first[0], first[1], 0);
        replaceWithElevator(second[0], second[1], 1);
        TempleOfTheAxolotl.LOGGER.info("Shallows fresh layout: elevator 1 at ({}, {}), elevator 2 at ({}, {})",
                first[0], first[1], second[0], second[1]);
        built=false;
    }
    private static void replaceWithElevator(int x, int z, int upperFloor) {
        int upperY = ORIGIN.getY() - upperFloor*HEIGHT;
        int lowerY = upperY-HEIGHT;
        int wx = ORIGIN.getX()+x*CELL, wz = ORIGIN.getZ()+z*CELL;
        TASKS.removeIf(t -> t.pos.getX()==wx && t.pos.getZ()==wz
                && (t.pos.getY()==upperY || t.pos.getY()==lowerY));
        TASKS.add(new Task(new BlockPos(wx,lowerY,wz),"tota_bubbleelevator",Rotation.NONE));
    }
    private static void place(ServerLevel level, Task task) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"maze/"+task.template);
        StructureTemplate template = level.getStructureManager().get(id)
                .orElseThrow(() -> new IllegalStateException("Missing Shallows NBT: "+id));
        BlockPos offset = switch (task.rotation) {
            case NONE -> BlockPos.ZERO;
            case CLOCKWISE_90 -> new BlockPos(14,0,0);
            case CLOCKWISE_180 -> new BlockPos(14,0,14);
            case COUNTERCLOCKWISE_90 -> new BlockPos(0,0,14);
        };
        BlockPos at=task.pos.offset(offset);
        StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE)
                .setRotation(task.rotation).setIgnoreEntities(false);
        if (!template.placeInWorld(level,at,at,settings,level.random,2))
            throw new IllegalStateException("Could not place "+id);
        int tier = Math.min(2, 1 + (ORIGIN.getY()-task.pos.getY())/HEIGHT);
        for (BlockPos bp : BlockPos.betweenClosed(task.pos, task.pos.offset(14,31,14))) {
            BlockEntity be=level.getBlockEntity(bp);
            if (be instanceof com.devaxo.templeoftheaxolotl.pre07.TrialTrapSpawnerEntity trap) trap.setTier(tier);
            if (level.getBlockState(bp).is(Blocks.CHEST)) {
                level.setBlock(bp,com.devaxo.templeoftheaxolotl.pre07.Pre07Blocks.CELESTIAL_CACHE.get().defaultBlockState(),2);
                if (level.getBlockEntity(bp) instanceof com.devaxo.templeoftheaxolotl.pre07.CelestialCacheEntity cache) cache.setTier(tier);
            } else if (be instanceof RandomizableContainerBlockEntity container && level.getBlockState(bp).is(Blocks.BARREL)) {
                ResourceKey<LootTable> key=ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,"chests/sushi_barrel_layer_"+tier));
                container.setLootTable(key,level.random.nextLong()); container.setChanged();
            }
        }
    }
    @SubscribeEvent public void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (owner!=server) {
            owner=server; TASKS.clear(); QUEUED.clear(); built=false;
            clearing=false; cursor=0; idleTicks=0; clearCursor=0;
        }
        ServerLevel level = server.getLevel(DIMENSION);
        if (level==null) return;
        if (clearing) {
            // Never clear occupied chambers or an expedition about to admit players.
            if (!level.players().isEmpty() || !QUEUED.isEmpty()) return;
            for (int i=0;i<3 && clearCursor<WIDTH*WIDTH*FLOORS;i++,clearCursor++) clearCell(level,clearCursor);
            if (clearCursor>=WIDTH*WIDTH*FLOORS) {clearing=false;built=false;clearCursor=0;}
            return;
        }
        if (!built && !TASKS.isEmpty()) {
            try {
                for (int i=0;i<PER_TICK && cursor<TASKS.size();i++) place(level,TASKS.get(cursor++));
                if (cursor>=TASKS.size()) { built=true; TASKS.clear(); cursor=0; }
            } catch (Exception ex) {
                TempleOfTheAxolotl.LOGGER.error("Shallows maze construction failed",ex);
                for (UUID queuedId : Set.copyOf(QUEUED)) {
                    ServerPlayer queuedPlayer = server.getPlayerList().getPlayer(queuedId);
                    if (queuedPlayer != null) {
                        queuedPlayer.getPersistentData().remove(RETURN);
                        queuedPlayer.displayClientMessage(Component.literal(
                                "The Shallows could not generate. Check latest.log for the missing template."), false);
                    }
                }
                TASKS.clear(); QUEUED.clear(); cursor=0;
                built=false;
                // Clear partially placed modules before another attempt.
                clearing=true; clearCursor=0; idleTicks=0;
            }
        }
        if (built && !QUEUED.isEmpty()) {
            for (UUID id : Set.copyOf(QUEUED)) {
                QUEUED.remove(id);
                ServerPlayer player=server.getPlayerList().getPlayer(id);
                if (player==null || player.serverLevel().dimension()!=Level.OVERWORLD) continue;
                boolean moved=player.teleportTo(level,22.5,202,26.5,Set.of(),player.getYRot(),player.getXRot());
                if (moved) {
                    player.getPersistentData().putInt(MODE,player.gameMode.getGameModeForPlayer().getId());
                    player.setGameMode(GameType.ADVENTURE);
                } else player.getPersistentData().remove(RETURN);
            }
        }
        if (built && level.players().isEmpty() && QUEUED.isEmpty()) {
            if (++idleTicks>=2) { clearing=true; clearCursor=0; idleTicks=0; }
        } else idleTicks=0;
    }
    // The Shallows only owns its own 75x75x48 footprint; never cleans the main Temple.
    private static void clearCell(ServerLevel level,int cell) {
        int xCell=cell%25%5, zCell=cell%25/5, floor=cell/25;
        // Load the target chunk(s) before removing blocks; don't depend on nearby players.
        int baseX = ORIGIN.getX() + xCell * CELL;
        int baseZ = ORIGIN.getZ() + zCell * CELL;
        for (int cx = baseX >> 4; cx <= (baseX + CELL - 1) >> 4; cx++)
            for (int cz = baseZ >> 4; cz <= (baseZ + CELL - 1) >> 4; cz++)
                level.getChunk(cx, cz);
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for (int x=xCell*15;x<xCell*15+15;x++) for (int z=zCell*15;z<zCell*15+15;z++)
            for (int y=ORIGIN.getY()-floor*HEIGHT;y<ORIGIN.getY()-floor*HEIGHT+HEIGHT;y++) {
                pos.set(x,y,z);
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
    }
    public static void exit(ServerPlayer player) {
        if (player.serverLevel().dimension()!=DIMENSION) return;
        MinecraftServer server=player.getServer(); if(server==null) return;
        ServerLevel overworld=server.overworld();
        CompoundTag data=player.getPersistentData().getCompound(RETURN);
        BlockPos portal=data.isEmpty()?overworld.getSharedSpawnPos():new BlockPos(data.getInt("x"),data.getInt("y"),data.getInt("z"));
        overworld.getChunkAt(portal);
        BlockPos safe=portal.above();
        for (Direction d:Direction.Plane.HORIZONTAL) {
            BlockPos candidate=portal.relative(d);
            if (overworld.getBlockState(candidate).isSolidRender(overworld,candidate)
                    && overworld.getBlockState(candidate.above()).isAir()
                    && overworld.getBlockState(candidate.above(2)).isAir()) {safe=candidate.above();break;}
        }
        if (player.teleportTo(overworld,safe.getX()+0.5,safe.getY(),safe.getZ()+0.5,
                Set.of(),player.getYRot(),player.getXRot())) {
            if(player.getPersistentData().contains(MODE)) player.setGameMode(GameType.byId(player.getPersistentData().getInt(MODE)));
            player.getPersistentData().remove(MODE); player.getPersistentData().remove(RETURN);
            // Leaving the Shallows ends its RNGesus curse.
            player.getPersistentData().remove("axoRngesusCurseStacks");
            player.removeEffect(
                    com.devaxo.templeoftheaxolotl.effect.ModEffects.RNGESUS_CURSE
            );
            // Reset only when the final explorer has departed; pending entries postpone it.
            ServerLevel shallows = server.getLevel(DIMENSION);
            if (shallows != null && shallows.players().isEmpty() && QUEUED.isEmpty() && built) {
                clearing = true;
                clearCursor = 0;
                idleTicks = 0;
            }
        }
    }
    @SubscribeEvent public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.serverLevel().dimension()==DIMENSION)
            exit(player); // safe return on reload; no orphaned adventure mode
    }
}
