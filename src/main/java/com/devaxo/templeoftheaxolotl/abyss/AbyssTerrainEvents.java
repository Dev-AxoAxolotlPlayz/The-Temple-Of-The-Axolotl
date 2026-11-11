package com.devaxo.templeoftheaxolotl.abyss;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

/** Experimental, deterministic post-load Abyss geology. Existing painted chunks require /tota abyss reset. */
public final class AbyssTerrainEvents {
    private static final String[] BIOMES = {"abyssal_seafloor", "abyssal_sandbars", "abyssal_ravines", "abyssal_plateaus", "abyssal_coral_shelves", "abyssal_kelp_seas"};
    private static final int MARKER_Y = -63, SEA = 200;
    private static AbyssTerrainEvents activePainter;
    private final Queue<ChunkPos> pending = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    // Scan loaded chunks in small batches; a single vanilla water block in the
    // false-seafloor layer indicates an incomplete terrain pass.
    private final Queue<ChunkPos> scanPending = new ArrayDeque<>();
    private final Set<Long> scanQueued = new HashSet<>();
    private long lastNearbyScan;
    private static final int SCAN_RADIUS = 4;
    private static final int SCANS_PER_TICK = 8;
    private static final int SCAN_INTERVAL = 10;
    private int playerScanCursor;
    // Work only on loaded chunks close to players; never force-load chunks for maintenance.
    private static final int THERMO_MIN_Y = 101, THERMO_MAX_Y = 110;
    private final Map<BlockPos, Long> vents = new HashMap<>();
    private final Map<BlockPos, Long> activePlumes = new HashMap<>();
    private long lastVentScan;
    // Chunk section writes bypass Level#setBlock; resend completed terrain to nearby clients.
    private final Map<Long, Long> pendingClientResync = new HashMap<>();

    private static double hash(int x, int z, long salt) {
        long h = ((long)x * 341873128712L) ^ ((long)z * 132897987541L) ^ salt;
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L; h ^= h >>> 33;
        return ((h >>> 11) & ((1L << 53)-1)) / (double)(1L << 53);
    }
    private static double wave(double x, double z, double scale, double phase) {
        return Math.sin(x*scale + Math.sin(z*scale*.72+phase)*1.4 + phase)
             * Math.cos(z*scale*.73 - Math.sin(x*scale*.6)*.8);
    }
    private static double field(int x, int z) {
        return .65*wave(x,z,.0087,.3)+.35*wave(x,z,.018,2.1);
    }
    private static double smooth(double a, double b, double x) {
        double t = Math.max(0,Math.min(1,(x-a)/(b-a)));
        return t*t*(3-2*t);
    }
    private static int biomeIndex(int x, int z) {
        double v = field(x,z) + .23*wave(x,z,.006,3.2);
        if (v > .53) return 3;
        if (v > .28) return 4;
        if (v > .10) return 1;
        if (v < -.48) return 2;
        if (v < -.16) return 5;
        return 0;
    }
    private static int floor(int x, int z) {
        double v = field(x,z) + .23*wave(x,z,.006,3.2);
        double sandbar = smooth(.06,.50,v);
        double ravine = 1.0-smooth(-.66,-.23,v);
        double broad = 9*wave(x,z,.017,.8);
        double current = 3.5*Math.sin(x*.13+z*.048+2.2*wave(x,z,.012,1.4));
        double ripples = 1.2*wave(x,z,.39,1.1);
        return (int)Math.round(Math.max(-60,Math.min(73,40+19*sandbar-92*ravine+broad+current+ripples)));
    }
    private static BlockState stone(int x, int y, int z) {
        double h = hash(x*13+y, z*7-y, 19013);
        return h < .70 ? Blocks.STONE.defaultBlockState() : h < .90 ? Blocks.ANDESITE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
    }
    /** Explicit server-thread arrival painting: does not depend on ChunkEvent.Load firing. */
    public static boolean ensureArrivalChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (activePainter == null) return false;
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        ChunkPos cp = new ChunkPos(chunkX, chunkZ);
        BlockPos marker = new BlockPos(cp.getMinBlockX(), MARKER_Y, cp.getMinBlockZ());
        if (!chunk.getBlockState(marker).is(Blocks.OBSIDIAN) || hasBrokenThermocline(chunk, cp)) {
            activePainter.paint(level, chunk, cp);
            activePainter.queued.remove(cp.toLong());
            activePainter.pending.remove(cp);
        }
        return chunk.getBlockState(marker).is(Blocks.OBSIDIAN) && !hasBrokenThermocline(chunk, cp);
    }

    private void queue(ChunkPos cp) { if (queued.add(cp.toLong())) pending.add(cp); }
    private static long playerDistanceSq(ChunkPos cp, List<ServerPlayer> players) {
        long nearest = Long.MAX_VALUE;
        for (ServerPlayer player : players) {
            ChunkPos center = player.chunkPosition();
            long dx = (long) cp.x - center.x, dz = (long) cp.z - center.z;
            nearest = Math.min(nearest, dx * dx + dz * dz);
        }
        return nearest;
    }
    private static boolean nearPlayer(ChunkPos cp, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            ChunkPos center = player.chunkPosition();
            if (Math.abs(cp.x - center.x) <= SCAN_RADIUS && Math.abs(cp.z - center.z) <= SCAN_RADIUS)
                return true;
        }
        return false;
    }
    private static ChunkPos takeNearest(Queue<ChunkPos> work, Set<Long> ids, List<ServerPlayer> players) {
        ChunkPos best = null;
        long distance = Long.MAX_VALUE;
        for (ChunkPos cp : work) {
            long d = playerDistanceSq(cp, players);
            if (d < distance) { best = cp; distance = d; }
        }
        if (best != null) { work.remove(best); ids.remove(best.toLong()); }
        return best;
    }
    private void queueScan(ChunkPos cp) {
        if (scanQueued.add(cp.toLong())) scanPending.add(cp);
    }
    private static boolean hasBrokenThermocline(LevelChunk chunk, ChunkPos cp) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = THERMO_MIN_Y; y <= THERMO_MAX_Y; y++)
            for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++)
                for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                    pos.set(x, y, z);
                    if (chunk.getBlockState(pos).is(Blocks.WATER)) return true;
                }
        return false;
    }
    @SubscribeEvent public void onChunk(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(AbyssTrialEvents.ABYSS)) {
            ChunkPos cp=event.getChunk().getPos(); queue(cp); queueScan(cp); registerExistingVents(level,cp);
        }
    }
    @SubscribeEvent public void onTick(ServerTickEvent.Post event) {
        activePainter = this;
        ServerLevel level = event.getServer().getLevel(AbyssTrialEvents.ABYSS);
        if (level == null) return;
        List<ServerPlayer> players = level.players();
        if (!players.isEmpty()) {
            // Discard old maintenance work that is no longer near any player.
            // The chunk-load event or next player-centered sweep will rediscover it.
            scanPending.removeIf(cp -> {
                if (nearPlayer(cp, players) && level.getChunkSource().hasChunk(cp.x, cp.z)) return false;
                scanQueued.remove(cp.toLong());
                return true;
            });
            // Every sweep, schedule loaded chunks in concentric rings. Rotate the
            // starting player to avoid starving other players on multiplayer servers.
            if (level.getGameTime() - lastNearbyScan >= SCAN_INTERVAL) {
                lastNearbyScan = level.getGameTime();
                for (int i = 0; i < players.size(); i++) {
                    ServerPlayer player = players.get((playerScanCursor + i) % players.size());
                    ChunkPos center = player.chunkPosition();
                    for (int radius = 0; radius <= SCAN_RADIUS; radius++)
                        for (int dx = -radius; dx <= radius; dx++)
                            for (int dz = -radius; dz <= radius; dz++) {
                                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                                int cx = center.x + dx, cz = center.z + dz;
                                if (level.getChunkSource().hasChunk(cx, cz)) queueScan(new ChunkPos(cx, cz));
                            }
                }
                playerScanCursor = (playerScanCursor + 1) % players.size();
            }
            for (int i = 0; i < SCANS_PER_TICK && !scanPending.isEmpty(); i++) {
                ChunkPos cp = takeNearest(scanPending, scanQueued, players);
                if (cp == null || !nearPlayer(cp, players) || !level.getChunkSource().hasChunk(cp.x, cp.z)) continue;
                LevelChunk chunk = level.getChunk(cp.x, cp.z);
                if (hasBrokenThermocline(chunk, cp)) {
                    TempleOfTheAxolotl.LOGGER.warn("Abyss false-seafloor water found in chunk {}, {}; scheduling full repaint", cp.x, cp.z);
                    queue(cp);
                }
            }
        }
        // Only one expensive full repaint per tick. Prioritize the closest
        // loaded chunk rather than old FIFO entries from distant chunk loads.
        if (!players.isEmpty() && !pending.isEmpty()) {
            pending.removeIf(cp -> {
                if (nearPlayer(cp, players) && level.getChunkSource().hasChunk(cp.x, cp.z)) return false;
                queued.remove(cp.toLong());
                return true;
            });
            ChunkPos cp = takeNearest(pending, queued, players);
            if (cp != null && level.getChunkSource().hasChunk(cp.x, cp.z)) {
                LevelChunk chunk = level.getChunk(cp.x, cp.z);
                boolean marked = chunk.getBlockState(new BlockPos(cp.getMinBlockX(), MARKER_Y, cp.getMinBlockZ())).is(Blocks.OBSIDIAN);
                if (!marked || hasBrokenThermocline(chunk, cp)) {
                    paint(level, chunk, cp);
                    if (hasBrokenThermocline(chunk, cp)) {
                        TempleOfTheAxolotl.LOGGER.error("Abyss repaint did not repair thermocline in chunk {}, {}; will retry", cp.x, cp.z);
                        queue(cp);
                    } else {
                        TempleOfTheAxolotl.LOGGER.info("Abyss terrain verified in chunk {}, {}", cp.x, cp.z);
                    }
                }
            }
        }
        flushClientResync(level);
        tickVents(level);
    }

    private void flushClientResync(ServerLevel level) {
        if (pendingClientResync.isEmpty()) return;
        long now = level.getGameTime();
        int view = level.getServer().getPlayerList().getViewDistance();
        Iterator<Map.Entry<Long, Long>> it = pendingClientResync.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue() > now) continue;
            it.remove();
            ChunkPos cp = new ChunkPos(entry.getKey());
            if (!level.getChunkSource().hasChunk(cp.x, cp.z)) continue;
            LevelChunk chunk = level.getChunk(cp.x, cp.z);
            ClientboundLevelChunkWithLightPacket packet = null;
            for (ServerPlayer player : level.players()) {
                int dx = Math.abs(player.chunkPosition().x - cp.x);
                int dz = Math.abs(player.chunkPosition().z - cp.z);
                if (dx > view || dz > view) continue;
                if (packet == null) packet = new ClientboundLevelChunkWithLightPacket(
                    chunk, level.getLightEngine(), null, null);
                player.connection.send(packet);
            }
        }
    }
    private void paint(ServerLevel level,LevelChunk chunk,ChunkPos cp) {
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        @SuppressWarnings("unchecked") Holder<Biome>[] holders=new Holder[BIOMES.length];
        for(int i=0;i<BIOMES.length;i++) holders[i]=registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(TempleOfTheAxolotl.MODID,BIOMES[i])));
        chunk.fillBiomesFromNoise((qx,qy,qz,sampler)->holders[biomeIndex(qx*4+2,qz*4+2)],level.getChunkSource().randomState().sampler());
        for(int lx=0;lx<16;lx++) for(int lz=0;lz<16;lz++) {
            int x=cp.getMinBlockX()+lx,z=cp.getMinBlockZ()+lz, f=floor(x,z), biome=biomeIndex(x,z);
            for(int y=-62;y<=SEA;y++) {
                BlockState block = y<=f-4 ? (y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState()
                        : y<=f ? (biome==2?Blocks.GRAVEL:Blocks.SAND).defaultBlockState()
                        : (y>=101 && y<=110 ? com.devaxo.templeoftheaxolotl.pre07.Pre07Fluids.THERMOCLINE_BLOCK.get().defaultBlockState() : Blocks.WATER.defaultBlockState());
                chunk.setBlockState(new BlockPos(x,y,z),block,false);
            }
            // Plateaus are independent eroded masses; sparse mesas extend to the seafloor.
            double cloud=field(x,z)+.24*wave(x,z,.041,2.8);
            double presence=smooth(.34,.68,cloud);
            if(biome==3 && presence>0 && (Math.abs(x)>16 || Math.abs(z)>16)) {
                int top=(int)Math.round(107+11*wave(x,z,.025,1.9)+5*wave(x,z,.092,2.2));
                int bottom=(int)Math.round(top-7-22*presence+7*wave(x,z,.064,.7));
                boolean mesa=hash(Math.floorDiv(x,96),Math.floorDiv(z,96),445)<.20;
                if(mesa) bottom=f-2;
                for(int y=Math.max(f+1,bottom);y<=Math.min(125,top);y++) {
                    double taper=mesa ? smooth(f,top-12,y)*.35+.65 : smooth(bottom-2,bottom+9,y);
                    double edge=presence*taper+.12*wave(x+y,z-y,.11,2.3);
                    if(edge>.20) chunk.setBlockState(new BlockPos(x,y,z),
                            hash(x+y,z-y,39)<.14?Blocks.PRISMARINE.defaultBlockState():stone(x,y,z),false);
                }
            }
            // Sparse kelp, coral and seafloor vegetation. Never obstruct the protected arrival island.
            if(biome==5 && f>0 && hash(x,z,802)<.085) {
                int height=2+(int)(hash(x,z,803)*8);
                for(int y=f+1;y<=Math.min(SEA-2,f+height);y++)
                    chunk.setBlockState(new BlockPos(x,y,z),y==f+height?Blocks.KELP.defaultBlockState():Blocks.KELP_PLANT.defaultBlockState(),false);
            }
            if(biome==4 && f>0 && hash(x,z,901)<.075)
                chunk.setBlockState(new BlockPos(x,f+1,z),hash(x,z,902)<.5?Blocks.TUBE_CORAL_BLOCK.defaultBlockState():Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),false);
        }
        // World-coordinate grid: identical formations regardless of chunk paint order.
        for(int gx=Math.floorDiv(cp.getMinBlockX()-18,32);gx<=Math.floorDiv(cp.getMaxBlockX()+18,32);gx++)
            for(int gz=Math.floorDiv(cp.getMinBlockZ()-18,32);gz<=Math.floorDiv(cp.getMaxBlockZ()+18,32);gz++) {
                int x=gx*32+5+(int)(hash(gx,gz,11)*22),z=gz*32+5+(int)(hash(gx,gz,12)*22);
                if(hash(gx,gz,13)<.32) boulder(chunk,cp,x,floor(x,z),z,3,31);
            }
        // Geothermal micro-clusters: three offset boulders surrounding a clear vent shaft.
        for(int gx=Math.floorDiv(cp.getMinBlockX()-24,96);gx<=Math.floorDiv(cp.getMaxBlockX()+24,96);gx++)
            for(int gz=Math.floorDiv(cp.getMinBlockZ()-24,96);gz<=Math.floorDiv(cp.getMaxBlockZ()+24,96);gz++) {
                if(hash(gx,gz,71)>.29) continue;
                int x=gx*96+20+(int)(hash(gx,gz,72)*56),z=gz*96+20+(int)(hash(gx,gz,73)*56);
                if(biomeIndex(x,z)==3) continue;
                int f=floor(x,z);
                int[][] offsets={{-6,-3},{6,-3},{0,7}};
                for(int i=0;i<3;i++) {
                    int bx=x+offsets[i][0],bz=z+offsets[i][1];
                    boulder(chunk,cp,bx,floor(bx,bz),bz,3,100+i);
                }
                if(cp.x==Math.floorDiv(x,16)&&cp.z==Math.floorDiv(z,16)) {
                    BlockPos vent=new BlockPos(x,f+1,z);
                    // Vent location is water; bubbles are created dynamically, never with permanent soul sand.
                    if(f+21<SEA) vents.put(vent,level.getGameTime()+100+(long)(hash(x,z,85)*400));
                }
            }
        // Place naturally generated merfolk archipelagos after geological painting.
        AbyssMerfolkVillages.paint(chunk, cp);
        AbyssMerfolkVillages.populate(level, cp);
        chunk.setBlockState(new BlockPos(cp.getMinBlockX(),MARKER_Y,cp.getMinBlockZ()),Blocks.OBSIDIAN.defaultBlockState(),false);
        chunk.setUnsaved(true);
        // Allow the bulk chunk edits to settle before sending full terrain/biome/light data.
        pendingClientResync.put(cp.toLong(), level.getGameTime() + 10L);
    }
    private static void boulder(LevelChunk chunk,ChunkPos cp,int cx,int floor,int cz,int radius,long seed) {
        if(biomeIndex(cx,cz)==3) return;
        int cy=floor+2;
        for(int dx=-4;dx<=4;dx++) for(int dz=-4;dz<=4;dz++) for(int dy=-3;dy<=4;dy++) {
            int x=cx+dx,z=cz+dz,y=cy+dy;
            if(Math.floorDiv(x,16)!=cp.x||Math.floorDiv(z,16)!=cp.z||y< -62||y>SEA) continue;
            double distortion=.75*(hash(x+cx,z+cz,seed+dy*37)-.5);
            if(dx*dx+dz*dz+dy*dy <= Math.pow(radius+distortion,2))
                chunk.setBlockState(new BlockPos(x,y,z),stone(x,y,z),false);
        }
    }
    private void registerExistingVents(ServerLevel level,ChunkPos cp) {
        for(int gx=Math.floorDiv(cp.getMinBlockX()-24,96);gx<=Math.floorDiv(cp.getMaxBlockX()+24,96);gx++)
            for(int gz=Math.floorDiv(cp.getMinBlockZ()-24,96);gz<=Math.floorDiv(cp.getMaxBlockZ()+24,96);gz++) {
                if(hash(gx,gz,71)>.29) continue;
                int x=gx*96+20+(int)(hash(gx,gz,72)*56),z=gz*96+20+(int)(hash(gx,gz,73)*56);
                if(Math.floorDiv(x,16)!=cp.x||Math.floorDiv(z,16)!=cp.z||biomeIndex(x,z)==3) continue;
                BlockPos vent=new BlockPos(x,floor(x,z)+1,z);
                if(vent.getY()+21<SEA)
                    vents.putIfAbsent(vent,level.getGameTime()+100+(long)(hash(x,z,85)*400));
            }
    }
    private void tickVents(ServerLevel level) {
        long now=level.getGameTime();
        // Remove plumes only where our bubble blocks remain; don't touch structures.
        if(now%5==0) {
            Iterator<Map.Entry<BlockPos,Long>> it=activePlumes.entrySet().iterator();
            while(it.hasNext()) {
                var e=it.next(); if(e.getValue()>now) continue;
                BlockPos pos=e.getKey();
                if(level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)&&level.getBlockState(pos).is(Blocks.BUBBLE_COLUMN)) level.setBlock(pos,Blocks.WATER.defaultBlockState(),3);
                it.remove();
            }
        }
        if(now-lastVentScan<20) return;
        lastVentScan=now;
        Iterator<Map.Entry<BlockPos,Long>> it=vents.entrySet().iterator();
        while(it.hasNext()) {
            var e=it.next(); BlockPos origin=e.getKey();
            if(!level.getChunkSource().hasChunk(origin.getX() >> 4, origin.getZ() >> 4)) {it.remove();continue;}
            if(e.getValue()>now) continue;
            int length=0;
            for(int i=0;i<20;i++) {
                BlockPos pos=origin.above(i);
                if(!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)||!level.getBlockState(pos).is(Blocks.WATER)) break;
                level.setBlock(pos,Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(net.minecraft.world.level.block.BubbleColumnBlock.DRAG_DOWN,false),3);
                activePlumes.put(pos,now+60);
                length++;
            }
            e.setValue(now+220+(long)(hash(origin.getX(),origin.getZ(),now/20)*380));
        }
    }
    private static final String WIPE_MARKER = "tota_abyss_wipe.pending";
    private static Path marker(net.minecraft.server.MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(WIPE_MARKER);
    }
    @SubscribeEvent public void onServerStopped(ServerStoppedEvent event) {
        Path root = event.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        Path marker = root.resolve(WIPE_MARKER);
        if (!Files.isRegularFile(marker)) return;
        Path target = root.resolve("dimensions").resolve(TempleOfTheAxolotl.MODID)
                .resolve("ethereal_abyss").normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            TempleOfTheAxolotl.LOGGER.error("Unsafe Abyss wipe target: {}", target);
            return;
        }
        try {
            if (Files.exists(target)) {
                try (var walk = Files.walk(target)) {
                    for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                }
            }
            Files.deleteIfExists(marker);
            TempleOfTheAxolotl.LOGGER.warn("Ethereal Abyss data wiped after server stop: {}", target);
        } catch (IOException ex) {
            TempleOfTheAxolotl.LOGGER.error("Ethereal Abyss wipe failed; marker retained: {}", target, ex);
        }
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tota").then(Commands.literal("abyss")
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    var server = ctx.getSource().getServer();
                    ServerLevel abyss = server.getLevel(AbyssTrialEvents.ABYSS);
                    if (abyss == null) {
                        ctx.getSource().sendFailure(Component.literal("Ethereal Abyss unavailable."));
                        return 0;
                    }
                    ServerLevel overworld = server.overworld();
                    if (overworld == null) return 0;
                    // The exact shared-spawn column is not guaranteed to be an empty
                    // two-block landing space. Search nearby instead of rejecting a reset.
                    BlockPos spawn = overworld.getSharedSpawnPos();
                    BlockPos landing = null;
                    for (int radius = 0; radius <= 16 && landing == null; radius++) {
                        for (int dx = -radius; dx <= radius && landing == null; dx++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                                BlockPos probe = overworld.getHeightmapPos(
                                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                        spawn.offset(dx, 0, dz));
                                if (probe.getY() <= overworld.getMinBuildHeight() + 1) continue;
                                BlockPos floor = probe.below();
                                if (overworld.getBlockState(floor).getCollisionShape(overworld, floor).isEmpty()) continue;
                                if (!overworld.getBlockState(probe).getCollisionShape(overworld, probe).isEmpty()) continue;
                                if (!overworld.getBlockState(probe.above()).getCollisionShape(overworld, probe.above()).isEmpty()) continue;
                                if (!overworld.getFluidState(probe).isEmpty()
                                        || !overworld.getFluidState(probe.above()).isEmpty()) continue;
                                landing = probe;
                                break;
                            }
                        }
                    }
                    if (landing == null) {
                        ctx.getSource().sendFailure(Component.literal(
                                "Abyss reset cancelled: no safe Overworld landing within 16 blocks of world spawn."));
                        return 0;
                    }
                    final BlockPos safeLanding = landing;
                    for (ServerPlayer player : List.copyOf(abyss.players())) {
                        if (!player.teleportTo(overworld, safeLanding.getX()+.5, safeLanding.getY(), safeLanding.getZ()+.5,
                                Set.of(), player.getYRot(), player.getXRot())) {
                            ctx.getSource().sendFailure(Component.literal("Abyss reset cancelled: evacuation failed."));
                            return 0;
                        }
                    }
                    if (!abyss.players().isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("Abyss reset cancelled: players remain inside."));
                        return 0;
                    }
                    try {
                        Files.writeString(marker(server), "Ethereal Abyss only\n",
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException ex) {
                        TempleOfTheAxolotl.LOGGER.error("Could not queue Abyss wipe", ex);
                        ctx.getSource().sendFailure(Component.literal("Abyss wipe could not be scheduled: " + ex.getMessage()));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Ethereal Abyss wipe queued. Close world / stop server, then restart to regenerate. No manual deletion."), true);
                    return 1;
                }))));
    }
}
