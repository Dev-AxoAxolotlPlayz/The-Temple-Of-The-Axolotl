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

/** Experimental, deterministic post-load Abyss geology. Existing painted chunks require /tota abyss reset. */
public final class AbyssTerrainEvents {
    private static final String[] BIOMES = {"abyssal_seafloor", "abyssal_sandbars", "abyssal_ravines", "abyssal_plateaus", "abyssal_coral_shelves", "abyssal_kelp_seas"};
    private static final int MARKER_Y = -63, SEA = 200;
    private final Queue<ChunkPos> pending = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    private final Map<BlockPos, Long> vents = new HashMap<>();
    private final Map<BlockPos, Long> activePlumes = new HashMap<>();
    private long lastVentScan;

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
    private void queue(ChunkPos cp) { if (queued.add(cp.toLong())) pending.add(cp); }
    @SubscribeEvent public void onChunk(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(AbyssTrialEvents.ABYSS)) {
            ChunkPos cp=event.getChunk().getPos(); queue(cp); registerExistingVents(level,cp);
        }
    }
    @SubscribeEvent public void onTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().getLevel(AbyssTrialEvents.ABYSS);
        if (level == null) return;
        int budget=1;
        while(budget-->0 && !pending.isEmpty()) {
            ChunkPos cp=pending.poll(); queued.remove(cp.toLong());
            if(!level.hasChunk(cp.x,cp.z)) continue;
            LevelChunk chunk=level.getChunk(cp.x,cp.z);
            if(chunk.getBlockState(new BlockPos(cp.getMinBlockX(),MARKER_Y,cp.getMinBlockZ())).is(Blocks.OBSIDIAN)) continue;
            paint(level,chunk,cp);
        }
        tickVents(level);
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
                        : y<=f ? (biome==2?Blocks.GRAVEL:Blocks.SAND).defaultBlockState() : Blocks.WATER.defaultBlockState();
                // The central island is a placed structure. Keep its upper
                // blocks intact while still painting the seabed beneath it.
                // This also eliminates the old 3x3-chunk rectangular hole.
                if (Math.abs(x) <= 15 && Math.abs(z) <= 15 && y >= 190) continue;
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
                    BlockPos spawn = overworld.getSharedSpawnPos();
                    overworld.getChunkAt(spawn);
                    BlockPos landing = overworld.getHeightmapPos(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
                    if (!overworld.getBlockState(landing).isAir() || !overworld.getBlockState(landing.above()).isAir()) {
                        ctx.getSource().sendFailure(Component.literal("Abyss reset cancelled: overworld spawn is obstructed."));
                        return 0;
                    }
                    for (ServerPlayer player : List.copyOf(abyss.players())) {
                        if (!player.teleportTo(overworld, landing.getX()+.5, landing.getY(), landing.getZ()+.5,
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
