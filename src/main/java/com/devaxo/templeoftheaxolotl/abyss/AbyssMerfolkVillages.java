package com.devaxo.templeoftheaxolotl.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerLevel;
import com.devaxo.templeoftheaxolotl.entity.ModEntities;
import com.devaxo.templeoftheaxolotl.entity.AbyssalMerfolkEntity;
import java.util.function.BiConsumer;

/** Deterministic, chunk-local underwater merfolk archipelagos.
 * Called after the Abyss terrain painter, before its completed-chunk marker is set.
 * Each island is entirely within one chunk; no cross-chunk write or chunk load.
 */
public final class AbyssMerfolkVillages {
    private AbyssMerfolkVillages() {}
    private static final int CELL = 24;
    private static double random(int x, int z, long salt) {
        long n = ((long)x * 341873128712L) ^ ((long)z * 132897987541L) ^ salt;
        n ^= n >>> 33; n *= 0xff51afd7ed558ccdL;
        n ^= n >>> 33; n *= 0xc4ceb9fe1a85ec53L;
        n ^= n >>> 33;
        return ((n >>> 11) & ((1L << 53)-1)) / (double)(1L << 53);
    }
    private static int anchorX(int cellX,int cellZ) { return cellX*CELL + 9 + (int)(random(cellX,cellZ,72002)*5); }
    private static int anchorZ(int cellX,int cellZ) { return cellZ*CELL + 9 + (int)(random(cellX,cellZ,72003)*5); }
    private static int villageCount(int cellX, int cellZ) {
        return 5 + (int)(random(cellX, cellZ, 72020) * 6);
    }
    // Larger settlements occupy wider archipelagos; all islands stay inside the cell.
    private static int islandStep(int count) {
        return 2; // Keep neighboring village chunks within 32 blocks; house count still varies.
    }
    /** 5-10 islands, one per selected chunk, with a guaranteed central hall. */
    private static boolean occupied(int cellX,int cellZ,int dx,int dz) {
        if (Math.abs(dx)>2 || Math.abs(dz)>2) return false;
        if (dx==0 && dz==0) return true;
        int count=villageCount(cellX,cellZ);
        int rank=0;
        double value=random(cellX*17+dx,cellZ*19+dz,72021);
        for(int a=-2;a<=2;a++) for(int b=-2;b<=2;b++) {
            if(a==0 && b==0) continue;
            if(random(cellX*17+a,cellZ*19+b,72021)<value) rank++;
        }
        return rank<count-1;
    }
    private static boolean isVillageChunk(ChunkPos cp) {
        int cx=Math.floorDiv(cp.x,CELL),cz=Math.floorDiv(cp.z,CELL);
        if(random(cx,cz,72001)>.55) return false;
        if(Math.abs(cp.x)<7 && Math.abs(cp.z)<7) return false;
        int count=villageCount(cx,cz);
        int step=islandStep(count);
        int dx=cp.x-anchorX(cx,cz), dz=cp.z-anchorZ(cx,cz);
        if (dx%step!=0 || dz%step!=0) return false;
        return occupied(cx,cz,dx/step,dz/step);
    }
    public static void paint(LevelChunk chunk, ChunkPos cp) {
        if(!isVillageChunk(cp))return;
        int cx=Math.floorDiv(cp.x,CELL),cz=Math.floorDiv(cp.z,CELL);
        boolean central=cp.x==anchorX(cx,cz)&&cp.z==anchorZ(cx,cz);
        int x=cp.getMinBlockX()+8,z=cp.getMinBlockZ()+8;
        int y=145+(int)(random(cp.x,cp.z,72006)*17);
        buildIsland((pos,state)->chunk.setBlockState(pos,state,false),x,y,z,central,cp.x,cp.z);
    }
    private static void put(BiConsumer<BlockPos,BlockState> writer,int x,int y,int z,BlockState state) {
        writer.accept(new BlockPos(x,y,z),state);
    }
    private static void buildIsland(BiConsumer<BlockPos,BlockState> writer,int x,int y,int z,boolean central,int seedX,int seedZ) {
        int radius=central?6:5;
        // Round, noisy rock underside with an organically tapered tip.
        for(int ox=-radius;ox<=radius;ox++) for(int oz=-radius;oz<=radius;oz++) {
            double r=Math.sqrt(ox*ox+oz*oz);
            double edge=radius-.55 + (random(x+ox,z+oz,72007)-.5)*1.2;
            if(r>edge) continue;
            int lower=y-3-(int)Math.max(0,(radius-r)*1.45);
            int upper=y-1;
            for(int h=lower;h<=upper;h++) {
                double narrowing = Math.max(0, (lower+2-h)*.24);
                if(r+narrowing>edge) continue;
                BlockState rock = random(x+ox+h,z+oz,72008)<.20
                    ? Blocks.PRISMARINE.defaultBlockState()
                    : random(x+ox+h,z+oz,72009)<.35
                        ? Blocks.ANDESITE.defaultBlockState():Blocks.STONE.defaultBlockState();
                put(writer,x+ox,h,z+oz,rock);
            }
            put(writer,x+ox,y,z+oz,
                    random(x+ox,z+oz,72010)<.25 ? Blocks.PRISMARINE.defaultBlockState()
                    : Blocks.DARK_PRISMARINE.defaultBlockState());
        }
        // Compact, open-water dwelling. No dry interior, doors or obstructed swimming entry.
        int half=central?3:2;
        // Weighted building types: cottages common; smithies and storehouses repeat freely.
        int building = buildingType(central, seedX, seedZ);
        BlockState wall = central ? Blocks.PRISMARINE_BRICKS.defaultBlockState()
                : building == 1 ? Blocks.PRISMARINE_BRICKS.defaultBlockState()
                : building == 2 ? Blocks.CUT_SANDSTONE.defaultBlockState()
                : random(seedX,seedZ,72011)<.5 ? Blocks.CUT_SANDSTONE.defaultBlockState()
                : Blocks.PRISMARINE.defaultBlockState();
        for(int ox=-half;ox<=half;ox++) for(int oz=-half;oz<=half;oz++) {
            boolean rim=Math.abs(ox)==half || Math.abs(oz)==half;
            if(!rim) continue;
            for(int h=1;h<=3;h++) {
                // Wide, three-block-high swim entrance on the south face.
                if(oz==half && Math.abs(ox)<=1) continue;
                // Open windows on other sides.
                if(h==2 && ((ox==0 && Math.abs(oz)==half) || (oz==0 && Math.abs(ox)==half))) continue;
                put(writer,x+ox,y+h,z+oz,wall);
            }

        }
        // Tiered roofs, closed against the sky but still water-filled underneath.
        // Three variants: stepped pavilion, shallow crown, and dark-prismarine cap.
        int roofStyle=(int)(random(seedX,seedZ,72031)*3);
        BlockState trim=Blocks.DARK_PRISMARINE.defaultBlockState();
        BlockState roof=roofStyle==1?Blocks.PRISMARINE_BRICKS.defaultBlockState():trim;
        for(int ox=-half-1;ox<=half+1;ox++) for(int oz=-half-1;oz<=half+1;oz++) {
            // Slight overhang, with ornamental corner notches.
            if(Math.abs(ox)==half+1 && Math.abs(oz)==half+1) continue;
            put(writer,x+ox,y+4,z+oz,trim);
        }
        if(roofStyle==0) {
            for(int ox=-half;ox<=half;ox++) for(int oz=-half;oz<=half;oz++)
                put(writer,x+ox,y+5,z+oz,roof);
            for(int ox=-half+1;ox<=half-1;ox++) for(int oz=-half+1;oz<=half-1;oz++)
                put(writer,x+ox,y+6,z+oz,Blocks.PRISMARINE_BRICKS.defaultBlockState());
        } else if(roofStyle==1) {
            for(int ox=-half;ox<=half;ox++) for(int oz=-half;oz<=half;oz++)
                put(writer,x+ox,y+5,z+oz,roof);
            put(writer,x,y+6,z,Blocks.SEA_LANTERN.defaultBlockState());
        } else {
            for(int ox=-half;ox<=half;ox++) for(int oz=-half;oz<=half;oz++)
                put(writer,x+ox,y+5,z+oz,trim);
            for(int ox=-half;ox<=half;ox++) {
                put(writer,x+ox,y+6,z,Blocks.PRISMARINE_BRICKS.defaultBlockState());
            }
        }
        // Chest alcoves inside the rear wall; loot is attached after terrain painting.
        BlockState inwardChest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        put(writer,x-half+1,y+1,z-half+1,inwardChest);
        if(central || building == 1 || building == 2 || random(seedX,seedZ,72032)<.35)
            put(writer,x+half-1,y+1,z-half+1,inwardChest);
        // Functional decoration makes the specialty houses recognizable.
        if (building == 1) {
            put(writer,x,y+1,z-half+1,Blocks.SMITHING_TABLE.defaultBlockState());
        } else if (building == 2) {
            put(writer,x,y+1,z-half+1,Blocks.BARREL.defaultBlockState());
        }
        put(writer,x-half,y+2,z-half,Blocks.SEA_LANTERN.defaultBlockState());
        put(writer,x+half,y+2,z-half,Blocks.SEA_LANTERN.defaultBlockState());
        // Small illuminated coral garden, kept away from the entry.
        put(writer,x-half-1,y+1,z,Blocks.SEA_PICKLE.defaultBlockState());
        put(writer,x+half+1,y+1,z,Blocks.SEA_PICKLE.defaultBlockState());
    }
    // 0: residence, 1: smithy, 2: storehouse, 3: gathering hall.
    private static int buildingType(boolean central, int sx, int sz) {
        if (central) return 3;
        double roll = random(sx, sz, 72040);
        return roll < .58 ? 0 : roll < .80 ? 1 : roll < .94 ? 2 : 0;
    }
    /** Spawn residents and initialize loot after each island is painted. */
    public static void populate(ServerLevel level, ChunkPos cp) {
        if(!isVillageChunk(cp))return;
        int x=cp.getMinBlockX()+8,z=cp.getMinBlockZ()+8;
        int y=145+(int)(random(cp.x,cp.z,72006)*17);
        int cx=Math.floorDiv(cp.x,CELL),cz=Math.floorDiv(cp.z,CELL);
        boolean central=cp.x==anchorX(cx,cz)&&cp.z==anchorZ(cx,cz);
        placeLoot(level,x,y,z,central,cp.x,cp.z);
        spawnResident(level,x+.5,y+2,z+.5);
    }
    private static void placeLoot(ServerLevel level,int x,int y,int z,boolean central,int sx,int sz) {
        int half=central?3:2;
        setLoot(level,new BlockPos(x-half+1,y+1,z-half+1),sx,sz,0);
        if(central || buildingType(central,sx,sz)==1 || buildingType(central,sx,sz)==2 || random(sx,sz,72032)<.35)
            setLoot(level,new BlockPos(x+half-1,y+1,z-half+1),sx,sz,1);
    }
    private static void setLoot(ServerLevel level,BlockPos pos,int sx,int sz,int slot) {
        BlockEntity entity=level.getBlockEntity(pos);
        if(entity instanceof RandomizableContainerBlockEntity container) {
            // Smithies have better odds; ordinary houses remain mostly T3.
            boolean smithy = buildingType(false,sx,sz)==1;
            boolean rare=random(sx+slot*71,sz,72033)<(smithy ? .30 : .16);
            ResourceLocation id=ResourceLocation.fromNamespaceAndPath(
                "temple_of_the_axolotl",rare?"chests/merfolk_village_t4":"chests/merfolk_village_t3");
            ResourceKey<LootTable> key=ResourceKey.create(Registries.LOOT_TABLE,id);
            container.setLootTable(key,level.getRandom().nextLong());
            container.setChanged();
        }
    }
    private static void spawnResident(ServerLevel level,double x,double y,double z) {
        AbyssalMerfolkEntity merfolk=ModEntities.ABYSSAL_MERFOLK.get().create(level);
        if(merfolk==null)return;
        merfolk.moveTo(x,y,z,0,0);
        level.addFreshEntity(merfolk);
    }
    /** Development preview: generates the SAME island/house geometry as natural villages. */
    public static int preview(ServerLevel level, BlockPos center, long seed) {
        java.util.Random rng=new java.util.Random(seed);
        int count=5+rng.nextInt(6);
        // Compact, airy archipelago: larger villages grow, but no isolated outlier.
        // Keep island centers at least 20 blocks apart (island radius <= 6).
        int baseRadius = count <= 6 ? 26 : count <= 8 ? 36 : 46;
        java.util.List<BlockPos> placed = new java.util.ArrayList<>();
        for(int i=0;i<count;i++) {
            int x=center.getX(), z=center.getZ();
            if(i>0) {
                double angle=2*Math.PI*(i-1)/(count-1) + (rng.nextDouble()-.5)*.65;
                int distance=baseRadius-6+rng.nextInt(13);
                x+=(int)Math.round(Math.cos(angle)*distance);
                z+=(int)Math.round(Math.sin(angle)*distance);
                // Resolve rare collisions from the randomized angles.
                for(int attempt=0;attempt<12;attempt++) {
                    boolean collision=false;
                    for(BlockPos other:placed) {
                        int dx=x-other.getX(), dz=z-other.getZ();
                        if(dx*dx+dz*dz<20*20) { collision=true; break; }
                    }
                    if(!collision) break;
                    x+=(int)Math.round(Math.cos(angle)*8);
                    z+=(int)Math.round(Math.sin(angle)*8);
                }
            }
            int y=center.getY()+(i==0?0:rng.nextInt(13)-6);
            placed.add(new BlockPos(x,y,z));
            boolean central=i==0;
            int sx=rng.nextInt(),sz=rng.nextInt();
            buildIsland((pos,state)->level.setBlock(pos,state,3),x,y,z,central,sx,sz);
            placeLoot(level,x,y,z,central,sx,sz);
            spawnResident(level,x+.5,y+2,z+.5);
        }
        return count;
    }

}
