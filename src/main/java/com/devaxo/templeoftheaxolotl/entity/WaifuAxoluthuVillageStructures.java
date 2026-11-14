package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import java.io.InputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Owner-reviewable shell-only village designs. No world edits. */
public final class WaifuAxoluthuVillageStructures {
    private WaifuAxoluthuVillageStructures() {}
    public static String normalize(String raw) {
        return switch (raw.toLowerCase(java.util.Locale.ROOT).trim().replace(' ', '_')) {
            case "storage_hut", "storage" -> "storage";
            case "small_house", "medium_house", "large_house", "workshop",
                 "infirmary", "library", "smithy", "town_center" -> raw.toLowerCase(java.util.Locale.ROOT).trim().replace(' ', '_');
            default -> "";
        };
    }
    public static int width(String type) {
        return switch(type) {
            case "small_house", "storage", "workshop", "infirmary", "smithy" -> 7;
            case "medium_house", "library" -> 11;
            case "large_house", "town_center" -> 15;
            default -> 0;
        };
    }
    private static void put(Map<Long,String> map, BlockPos origin, int x,int y,int z,String id) {
        map.put(origin.offset(x,y,z).asLong(), "minecraft:"+id);
    }
    public static CompoundTag create(String requestedType, BlockPos origin) {
        return create(requestedType, origin, java.util.concurrent.ThreadLocalRandom.current().nextLong());
    }
    public static CompoundTag create(String requestedType, BlockPos origin, long seed) {
        return createExpanded(requestedType, origin, seed, 0);
    }
    private static CompoundTag createExpanded(String requestedType, BlockPos origin, long seed, int extra) {
        String type=normalize(requestedType);
        int w=width(type);
        if(w==0) throw new IllegalArgumentException("Unsupported building: "+requestedType);
        if ("workshop".equals(type)) return createWorkshop(origin, seed, extra);
        Map<Long,String> map=new LinkedHashMap<>();
        java.util.Set<Long> upperRoofSlabs = new java.util.HashSet<>();
        // Functional furnishing core remains stable; the outer envelope is generated
        // independently of building type. A seed + revision produces reproducible art.
        java.util.Random random = new java.util.Random(seed);
        // Allocate enough usable interior for large plot-wide furnishing requirements.
        // This is an initial size, not a fixed architectural template.
        int core = extra + switch (type) {
            case "library" -> 19;
            case "storage", "large_house", "town_center" -> 13;
            case "medium_house" -> 11;
            default -> 9;
        };
        boolean[][] footprint = new boolean[33][33];
        int ox = 5, oz = 5;
        for (int x=0;x<core;x++) for (int z=0;z<core;z++) footprint[ox+x][oz+z]=true;
        // Add connected wings and bays of different proportions. No fixed shape catalog.
        int wings = 1 + random.nextInt(4);
        for (int k=0;k<wings;k++) {
            int side=random.nextInt(4), length=2+random.nextInt(4);
            int span=3+random.nextInt(Math.max(1,core-3));
            int start=1+random.nextInt(Math.max(1,core-span));
            for(int d=0;d<length;d++) for(int q=0;q<span;q++) {
                int x= side==0 ? ox-1-d : side==1 ? ox+core+d : ox+start+q;
                int z= side==2 ? oz-1-d : side==3 ? oz+core+d : oz+start+q;
                if(side<2) z=oz+start+q;
                if(x>=0&&x<33&&z>=0&&z<33) footprint[x][z]=true;
            }
        }
        // Preserve an accessible, 3-block-wide front entrance into the furnishing core.
        int doorX=ox+core/2;
        boolean doubleEntrance = core >= 7 && random.nextBoolean();
        // Keep the front entrance unobstructed even when a wing joins the front wall.
        for (int x=doorX;x<=doorX+(doubleEntrance?1:0);x++) footprint[x][oz-1]=false;
        // Recursively subdivide the usable core into connected rooms.
        // Every cut is internal, and every separating wall has a 2-wide
        // opening. The partitions are saved as an explicit room manifest.
        ListTag rooms = new ListTag();
        java.util.ArrayList<int[]> pending = new java.util.ArrayList<>();
        pending.add(new int[]{1,1,core-2,core-2});
        int targetRooms = Math.min(7, 2 + random.nextInt(Math.max(2, core/3)));
        for (int attempt=0;attempt<24 && pending.size()<targetRooms;attempt++) {
            int best=-1, bestArea=0;
            for(int i=0;i<pending.size();i++) {
                int[] r=pending.get(i);
                int area=r[2]*r[3];
                if(area>bestArea && (r[2]>=9 || r[3]>=9)) {best=i;bestArea=area;}
            }
            if(best<0) break;
            int[] r=pending.remove(best);
            boolean vertical=r[2]>=9 && (r[3]<9 || random.nextBoolean());
            if(vertical) {
                int left=4+random.nextInt(r[2]-8);
                int wall=r[0]+left;
                int opening=r[1]+1+random.nextInt(r[3]-2);
                for(int z=r[1];z<r[1]+r[3];z++)
                    if(z!=opening && z!=opening+1)
                        for(int y=1;y<=3;y++) put(map,origin,wall,y,z,"oak_planks");
                pending.add(new int[]{r[0],r[1],left,r[3]});
                pending.add(new int[]{wall+1,r[1],r[2]-left-1,r[3]});
            } else if(r[3]>=9) {
                int front=4+random.nextInt(r[3]-8);
                int wall=r[1]+front;
                int opening=r[0]+1+random.nextInt(r[2]-2);
                for(int x=r[0];x<r[0]+r[2];x++)
                    if(x!=opening && x!=opening+1)
                        for(int y=1;y<=3;y++) put(map,origin,x,y,wall,"oak_planks");
                pending.add(new int[]{r[0],r[1],r[2],front});
                pending.add(new int[]{r[0],wall+1,r[2],r[3]-front-1});
            } else pending.add(r);
        }
        for(int[] r:pending) {
            CompoundTag room=new CompoundTag();
            room.putInt("x",r[0]); room.putInt("z",r[1]);
            room.putInt("width",r[2]); room.putInt("depth",r[3]);
            rooms.add(room);
        }
        int roomCount=rooms.size();
        int partitionZ=core/2, passageX=core/2;
        boolean extraPartition=false, rearPartition=false;
        int partitionX=core/2,crossPassageZ=core/3,rearZ=core*3/4,rearPassageX=core/2;
        for(int x=0;x<33;x++) for(int z=0;z<33;z++) if(footprint[x][z]) {
            int rx=x-ox, rz=z-oz;
            put(map,origin,rx,0,rz,"cobblestone");
            boolean edge = x==0||z==0||x==32||z==32
                ||!footprint[x-1][z]||!footprint[x+1][z]
                ||!footprint[x][z-1]||!footprint[x][z+1];
            if(edge) for(int y=1;y<=4;y++) {
                if(z==oz && (x==doorX || (doubleEntrance && x==doorX+1)) && y<=2) continue;
                boolean xEdge = x==0 || x==32 || !footprint[x-1][z] || !footprint[x+1][z];
                boolean zEdge = z==0 || z==32 || !footprint[x][z-1] || !footprint[x][z+1];
                boolean corner = xEdge && zEdge;
                boolean nearCorner = corner;
                for (int dx=-1; dx<=1 && !nearCorner; dx++)
                    for (int dz=-1; dz<=1 && !nearCorner; dz++) {
                        int xx=x+dx, zz=z+dz;
                        if (xx<0 || zz<0 || xx>=33 || zz>=33 || !footprint[xx][zz]) continue;
                        boolean xe=xx==0||xx==32||!footprint[xx-1][zz]||!footprint[xx+1][zz];
                        boolean ze=zz==0||zz==32||!footprint[xx][zz-1]||!footprint[xx][zz+1];
                        if (xe && ze) nearCorner=true;
                    }
                boolean nearEntrance = z==oz && Math.abs(x-doorX)<=2;
                String material=corner?"stripped_oak_log":
                    y==2 && !nearCorner && !nearEntrance && (x+z)%4==0 ? "glass":"oak_planks";
                put(map,origin,rx,y,rz,material);
            }
        }
        // Stair-free half-step gable: bottom slab, full block, bottom slab,
        // full block, repeating toward the ridge. Each inward course rises
        // by half a block, with no facing/shape state or neighbor updates.
        int ridgeDirection = random.nextBoolean() ? 0 : 1;
        int maxCourse = (core - 1) / 2;
        int roofRise = maxCourse / 2;
        for (int x = ox - 1; x <= ox + core; x++) {
            for (int z = oz - 1; z <= oz + core; z++) {
                int cross = ridgeDirection == 0 ? x - ox : z - oz;
                int rx = x - ox, rz = z - oz;
                if (cross < 0 || cross >= core) {
                    // A half-height overhang at both gable edges.
                    put(map, origin, rx, 5, rz, "spruce_slab");
                    continue;
                }
                int course = Math.min(cross, core - 1 - cross);
                int y = 5 + course / 2;
                // Even course: slab (top is y+0.5); odd: full block (top y+1).
                put(map, origin, rx, y, rz,
                        (course & 1) == 0 ? "spruce_slab" : "spruce_planks");
            }
        }
        // Roof every connected wing as a small gable that meets the main roof.
        // The gable axis follows the direction the wing projects away from the core.
        // Keep the central roof authoritative at intersections.
        for (int x=1;x<32;x++) for (int z=1;z<32;z++) {
            if (!footprint[x][z] || (x>=ox && x<ox+core && z>=oz && z<oz+core)) continue;
            int dx = x<ox ? -1 : x>=ox+core ? 1 : 0;
            int dz = z<oz ? -1 : z>=oz+core ? 1 : 0;
            // Determine a local wing width across its projection axis.
            int lo=dx!=0?z:x, hi=lo;
            while(lo>0 && footprint[dx!=0?x:lo-1][dx!=0?lo-1:z]
                    && !(dx!=0?(x>=ox&&x<ox+core):(lo-1>=oz&&lo-1<oz+core))) lo--;
            while(hi<32 && footprint[dx!=0?x:hi+1][dx!=0?hi+1:z]
                    && !(dx!=0?(x>=ox&&x<ox+core):(hi+1>=oz&&hi+1<oz+core))) hi++;
            int course=Math.min((dx!=0?z:x)-lo,hi-(dx!=0?z:x));
            int roofY=5+course/2;
            long key=origin.offset(x-ox,roofY,z-oz).asLong();
            map.putIfAbsent(key,"minecraft:"+((course&1)==0?"spruce_slab":"spruce_planks"));
            // Blend exposed wing edges into the lower eaves with a top slab.
            if(course==0 && roofY==5) {
                long edgeKey=origin.offset(x-ox,5,z-oz).asLong();
                if(!map.containsKey(edgeKey)) { map.put(edgeKey,"minecraft:spruce_slab"); upperRoofSlabs.add(edgeKey); }
            }
        }
        // Project a one-block eave around each wing without flattening its gable.
        for(int x=1;x<32;x++) for(int z=1;z<32;z++) {
            if(!footprint[x][z] || (x>=ox&&x<ox+core&&z>=oz&&z<oz+core)) continue;
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
                int xx=x+dx,zz=z+dz;
                if(xx<0||xx>=33||zz<0||zz>=33||footprint[xx][zz]) continue;
                long key=origin.offset(xx-ox,5,zz-oz).asLong();
                if(map.putIfAbsent(key,"minecraft:spruce_slab")==null) upperRoofSlabs.add(key);
            }
        }
        // Covered outdoor patio. Keep the entrance's first two vertical
        // blocks open, use support posts and a roof attached to the facade.
        boolean patio = random.nextBoolean();
        int patioDepth = patio ? 2 + random.nextInt(2) : 0;
        int patioHalfWidth = Math.min(3,core/2-1);
        if(patio) {
            for(int x=core/2-patioHalfWidth;x<=core/2+patioHalfWidth;x++) {
                for(int z=-patioDepth;z<=-1;z++) {
                    put(map,origin,x,0,z,"cobblestone");
                    put(map,origin,x,5,z,"spruce_slab");
                }
            }
            for(int x:new int[]{core/2-patioHalfWidth,core/2+patioHalfWidth}) {
                for(int y=1;y<=4;y++)
                    put(map,origin,x,y,-patioDepth,"oak_fence");
            }
        }
        // Seal the actual wall-to-roof junctions AFTER all roof courses exist.
        // Only columns inside the enclosed footprint are sealed. Patio and
        // eaves are deliberately excluded; entrance clearance stays open.
        for (int x=0;x<33;x++) for(int z=0;z<33;z++) {
            if (!footprint[x][z]) continue;
            int rx=x-ox, rz=z-oz;
            boolean edge=x==0||z==0||x==32||z==32
                    ||!footprint[x-1][z]||!footprint[x+1][z]
                    ||!footprint[x][z-1]||!footprint[x][z+1];
            // The underside is the first roof block at/above wall height.
            int roofY=-1;
            for(int y=5;y<=5+roofRise;y++) {
                if(map.containsKey(origin.offset(rx,y,rz).asLong())) {
                    roofY=y;break;
                }
            }
            if(roofY<0) continue;
            if(edge) {
                for(int y=5;y<roofY;y++)
                    put(map,origin,rx,y,rz,"oak_planks");
            } else {
                // Existing interior dividers have solid blocks at Y+3.
                // Leave every two-wide passage unblocked.
                String partition=map.get(origin.offset(rx,3,rz).asLong());
                if("minecraft:oak_planks".equals(partition)) {
                    for(int y=4;y<roofY;y++)
                        put(map,origin,rx,y,rz,"oak_planks");
                }
            }
        }
        // Lantern fixtures are chosen only after roof and gable geometry exists.
        for(int x=1;x<core-1;x++) for(int z=-2;z<=-1;z++)
            put(map,origin,x,0,z,"cobblestone");
        addLanternFixtures(map, origin, core, core, 5, true);
        ListTag blocks=new ListTag();
        for(var entry:map.entrySet()) {
            CompoundTag block=new CompoundTag();
            block.putLong("pos",entry.getKey());
            block.putString("block",entry.getValue());
            BlockPos p = BlockPos.of(entry.getKey());
            int rx = p.getX() - origin.getX();
            int rz = p.getZ() - origin.getZ();
            if (entry.getValue().endsWith(":spruce_slab")) {
                block.putString("slabType", upperRoofSlabs.contains(entry.getKey()) ? "top" : "bottom");
            } else if (entry.getValue().equals("minecraft:lantern")) {
                block.putBoolean("hanging", map.containsKey(BlockPos.of(entry.getKey()).above().asLong())
                        && "minecraft:chain".equals(map.get(BlockPos.of(entry.getKey()).above().asLong())));
            }
            blocks.add(block);
        }
        CompoundTag preview=new CompoundTag();
        preview.putInt("schema",23);
        // The hologram's gold entrance marker and rotation controls depend
        // on these two fields being present in every fresh blueprint.
        preview.putLong("entrance",origin.offset(core/2,1,0).asLong());
        preview.putString("facing","north");
        preview.putBoolean("doubleEntrance",doubleEntrance);
        preview.putInt("partitionZ",partitionZ-oz);
        preview.putInt("passageX",passageX-ox);
        preview.putInt("roomCount",roomCount);
        preview.put("designRooms",rooms);
        preview.putBoolean("extraPartition",extraPartition);
        preview.putInt("partitionX",partitionX-ox);
        preview.putInt("crossPassageZ",crossPassageZ-oz);
        preview.putBoolean("rearPartition",rearPartition);
        preview.putInt("rearPartitionZ",rearZ-oz);
        preview.putInt("rearPassageX",rearPassageX-ox);
        preview.putLong("designSeed",seed);
        preview.putString("type",type);
        preview.putString("status","OWNER_REVIEW_REQUIRED");
        preview.putLong("origin",origin.asLong());
        preview.putInt("width",core);
        preview.putInt("depth",core+2);
        preview.putInt("height",6+roofRise);
        preview.putBoolean("coveredPatio",patio);
        preview.putInt("patioDepth",patioDepth);
        preview.putInt("roofRise",roofRise);
        preview.putInt("ridgeDirection",ridgeDirection);
        preview.put("blocks",blocks);
        preview.putInt("count",blocks.size());
        // Do not show an unbuildable hologram. Expand the SAME design with
        // additional floor area until the room-aware furnishing manifest fits.
        try {
            WaifuAxoluthuFurnishingLayouts.plan(preview, origin, 0);
        } catch (IllegalStateException capacity) {
            if (extra >= 8) throw new IllegalStateException(
                    "Unable to fit all required furniture even after footprint expansion: " + type, capacity);
            return createExpanded(requestedType, origin, seed, extra + 2);
        }
        return preview;
    }
    /** Merge a saved entrance module as an authoritative 3D volume.
     * The Structure Block templates were authored with local X as depth and
     * local Z as facade width. World-facing north maps (x,z) -> (z,x-1).
     * Every missing template coordinate is explicitly AIR, not "don't care".
     */
    private static void mergeEntrance(Map<Long,String> map, BlockPos origin,
                                      int worldLeft, boolean doubleDoor) {
        String filename = doubleDoor ? "double_door_entrance.nbt" : "one_door_entrance.nbt";
        String resource = "data/temple_of_the_axolotl/structure/axoluthu/" + filename;
        try (InputStream in = WaifuAxoluthuVillageStructures.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("Missing Axoluthu entrance template: " + resource);
            CompoundTag nbt = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            ListTag size = nbt.getList("size", 3);
            if (size.size()!=3) throw new IllegalStateException("Malformed entrance size: " + filename);
            int depth = size.getInt(0), height = size.getInt(1), width = size.getInt(2);
            if (depth != 3 || height != 4 || width != (doubleDoor ? 4 : 3))
                throw new IllegalStateException("Unexpected entrance dimensions: " + filename);
            // First wipe the entire volume, including absent NBT blocks.
            for (int dx=0;dx<depth;dx++) for(int dy=0;dy<height;dy++)
                for(int dz=0;dz<width;dz++)
                    put(map,origin,worldLeft+dz,dy,dx-1,"air");
            ListTag palette=nbt.getList("palette",10);
            ListTag blocks=nbt.getList("blocks",10);
            for(int i=0;i<blocks.size();i++) {
                CompoundTag block=blocks.getCompound(i);
                ListTag pos=block.getList("pos", 3);
                if(pos.size()!=3) throw new IllegalStateException("Invalid entrance block position");
                int dx=pos.getInt(0),dy=pos.getInt(1),dz=pos.getInt(2);
                int stateIndex=block.getInt("state");
                if(dx<0||dx>=depth||dy<0||dy>=height||dz<0||dz>=width
                        ||stateIndex<0||stateIndex>=palette.size())
                    throw new IllegalStateException("Invalid entrance block in " + filename);
                String id=palette.getCompound(stateIndex).getString("Name");
                if(!id.equals("minecraft:air") && !id.equals("minecraft:cobblestone")
                        && !id.equals("minecraft:oak_planks"))
                    throw new IllegalStateException("Unexpected entrance palette block: " + id);
                map.put(origin.offset(worldLeft+dz,dy,dx-1).asLong(),id);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read entrance template: " + filename,ex);
        }
    }

    /** Choose supported fixtures; no floating blocks, torches, or sea lanterns. */
    private static void addLanternFixtures(Map<Long,String> map, BlockPos origin,
                                            int width, int depth, int ceilingY, boolean standing) {
        // Plan each fixture as a complete assembly, not a fixed-length chain.
        // Ground is relative Y=0 in these generated structures. A hanging lantern
        // occupies one block, and its bottom must be above the two-block walking
        // clearance (lantern Y >= 3). Never hang in a three-block-high room.
        for(int x=2;x<width-2;x+=5) for(int z=2;z<depth-2;z+=5) {
            int support=-1;
            for(int y=ceilingY;y<=ceilingY+5;y++) {
                String id=map.get(origin.offset(x,y,z).asLong());
                if(id!=null && (id.endsWith("planks") || id.endsWith("slab"))) {support=y;break;}
            }
            if(support<5) continue;
            // At Y=5, for example, one chain + lantern at Y=3 is the
            // longest safe assembly. A second chain would put the lamp at Y=2.
            int maxChains=Math.min(3,support-4);
            if(maxChains<1) continue;
            int chains=maxChains;
            int lampY=support-chains-1;
            if(lampY<3) continue;
            boolean clear=true;
            for(int y=lampY;y<support;y++)
                if(map.containsKey(origin.offset(x,y,z).asLong())) {clear=false;break;}
            if(!clear) continue;
            for(int i=1;i<=chains;i++) put(map,origin,x,support-i,z,"chain");
            put(map,origin,x,lampY,z,"lantern");
        }
        if(!standing) return;
        // Wall/fence-top lights, only on existing supports and only in empty positions.
        // Space them out and leave the center doorway / approach untouched.
        java.util.ArrayList<Long> supports=new java.util.ArrayList<>();
        for(var e:map.entrySet()) {
            String id=e.getValue();
            if(id.equals("minecraft:cobblestone_wall") || id.equals("minecraft:oak_fence"))
                supports.add(e.getKey());
        }
        supports.sort(Long::compare);
        java.util.ArrayList<BlockPos> installed=new java.util.ArrayList<>();
        for(long key:supports) {
            BlockPos support=BlockPos.of(key), top=support.above();
            if(map.containsKey(top.asLong())) continue;
            if(support.getY()<origin.getY()+1 || support.getY()>origin.getY()+3) continue;
            if(Math.abs(support.getX()-origin.getX()-width/2)<=1
                    && support.getZ()<=origin.getZ()) continue;
            boolean spaced=true;
            for(BlockPos prev:installed)
                if(prev.distSqr(top)<25) {spaced=false;break;}
            if(!spaced) continue;
            map.put(top.asLong(),"minecraft:lantern");
            installed.add(top);
        }
    }

    /** Open-plan workshop: one envelope, one roof, and an unobstructed patio exit.
     * Deterministic from the saved design seed. Never mutates an issued blueprint. */
    private static CompoundTag createWorkshop(BlockPos origin, long seed, int extra) {
        java.util.Random random = new java.util.Random(seed);
        int core = 9 + extra;
        int center = core / 2;
        boolean doubleDoor = random.nextBoolean();
        int doorLeft = doubleDoor ? center - 1 : center;
        boolean covered = random.nextBoolean();
        int patioDepth = 3;
        int patioLeft = Math.max(1, center - 3);
        int patioRight = Math.min(core - 2, center + 3);
        Map<Long,String> map = new LinkedHashMap<>();
        // A single enclosed rectangle, without wings or internal partitions.
        for (int x=0;x<core;x++) for (int z=0;z<core;z++) {
            put(map,origin,x,0,z,"cobblestone");
            boolean edge = x==0 || x==core-1 || z==0 || z==core-1;
            if (!edge) continue;
            for (int y=1;y<=4;y++) {
                if (z==0 && (x==doorLeft || (doubleDoor && x==doorLeft+1)) && y<=2) continue;
                boolean corner=(x==0||x==core-1)&&(z==0||z==core-1);
                boolean window=y==2 && !corner &&
                    ((z==core-1 && (x==2||x==core-3)) ||
                     ((x==0||x==core-1)&&(z==2||z==core-3)));
                put(map,origin,x,y,z,corner?"stripped_oak_log":window?"glass":"oak_planks");
            }
        }
        // Full ceiling prevents exposed gaps below the decorative gable.
        for(int x=0;x<core;x++) for(int z=0;z<core;z++)
            put(map,origin,x,5,z,"spruce_planks");
        // Mirror-symmetric, half-step gable. Ridge runs front-to-back.
        // Separate the decorative roof from the ceiling so no interior beams intrude.
        for(int x=-1;x<=core;x++) for(int z=-1;z<=core;z++) {
            int course = x<0||x>=core ? 0 : Math.min(x,core-1-x);
            int roofY=6+course/2;
            put(map,origin,x,roofY,z,(course&1)==0?"spruce_slab":"spruce_planks");
        }
        // Open patio floor with a two-block-wide path to the front door.
        for(int x=patioLeft;x<=patioRight;x++) for(int z=-patioDepth;z<0;z++)
            put(map,origin,x,0,z,"cobblestone");
        if(covered) {
            // Canopy is independent of the main gable. Posts only at outer corners.
            for(int x=patioLeft-1;x<=patioRight+1;x++)
                for(int z=-patioDepth-1;z<=-1;z++)
                    put(map,origin,x,5,z,"spruce_slab");
            for(int x:new int[]{patioLeft,patioRight})
                for(int y=1;y<=4;y++) put(map,origin,x,y,-patioDepth,"stripped_oak_log");
        } else {
            // Low decorative perimeter, with the central approach left open.
            for(int x=patioLeft;x<=patioRight;x++) {
                if(x!=doorLeft && x!=center)
                    put(map,origin,x,1,-patioDepth,"cobblestone_wall");
            }
            for(int z=-patioDepth+1;z<0;z++) {
                put(map,origin,patioLeft,1,z,"cobblestone_wall");
                put(map,origin,patioRight,1,z,"cobblestone_wall");
            }
        }
        // Clear the outdoor approach, then insert the user-authored NBT LAST.
        // Its entire 3D volume, including implicit air and cobblestone floor,
        // wins over walls, roofing, posts and any other procedural geometry.
        for(int x=doorLeft;x<=doorLeft+(doubleDoor?1:0);x++)
            for(int z=-patioDepth;z<=-2;z++)
                for(int y=1;y<=2;y++) put(map,origin,x,y,z,"air");
        mergeEntrance(map,origin,doorLeft-1,doubleDoor);
        // Door halves are separate blueprint entries. Add after NBT override.
        for(int x=doorLeft;x<=doorLeft+(doubleDoor?1:0);x++) {
            put(map,origin,x,1,0,"oak_door");
            put(map,origin,x,2,0,"oak_door");
        }
        // Entrances are authoritative; fixtures are planned around their final geometry.
        addLanternFixtures(map, origin, core, core, 5, true);
        // Close the two gable-end triangles underneath the stepped roof.
        for (int x=0;x<core;x++) {
            int roofY=6+Math.min(x,core-1-x)/2;
            for(int z:new int[]{0,core-1})
                for(int y=6;y<roofY;y++)
                    map.putIfAbsent(origin.offset(x,y,z).asLong(),"minecraft:oak_planks");
        }
        ListTag blocks=new ListTag();
        for(var entry:map.entrySet()) {
            CompoundTag block=new CompoundTag();
            block.putLong("pos",entry.getKey());
            block.putString("block",entry.getValue());
            if(entry.getValue().endsWith(":spruce_slab")) block.putString("slabType","bottom");
            if(entry.getValue().equals("minecraft:lantern")) block.putBoolean("hanging",
                    "minecraft:chain".equals(map.get(BlockPos.of(entry.getKey()).above().asLong())));
            if(entry.getValue().equals("minecraft:oak_door")) {
                int x=BlockPos.of(entry.getKey()).getX()-origin.getX();
                int y=BlockPos.of(entry.getKey()).getY()-origin.getY();
                block.putString("facing","north");
                block.putString("doorHalf",y==2?"upper":"lower");
                block.putString("hinge",doubleDoor && x==doorLeft+1?"right":"left");
            }
            blocks.add(block);
        }
        ListTag rooms=new ListTag();
        CompoundTag room=new CompoundTag();
        room.putInt("x",1);room.putInt("z",1);
        room.putInt("width",core-2);room.putInt("depth",core-2);
        rooms.add(room);
        CompoundTag preview=new CompoundTag();
        preview.putInt("schema",23);
        preview.putLong("entrance",origin.offset(doorLeft,1,0).asLong());
        preview.putString("facing","north");
        preview.putBoolean("doubleEntrance",doubleDoor);
        preview.putInt("partitionZ",core/2);
        preview.putInt("passageX",center);
        preview.putInt("roomCount",1);
        preview.put("designRooms",rooms);
        preview.putBoolean("extraPartition",false);
        preview.putInt("partitionX",center);
        preview.putInt("crossPassageZ",core/3);
        preview.putBoolean("rearPartition",false);
        preview.putInt("rearPartitionZ",core*3/4);
        preview.putInt("rearPassageX",center);
        preview.putLong("designSeed",seed);
        preview.putString("type","workshop");
        preview.putString("status","OWNER_REVIEW_REQUIRED");
        preview.putLong("origin",origin.asLong());
        preview.putInt("width",core);
        preview.putInt("depth",core+patioDepth);
        preview.putInt("height",8+core/4);
        preview.putBoolean("coveredPatio",covered);
        preview.putString("patioVariant",covered?"COVERED_POSTS":"OPEN_LOW_WALL");
        preview.putInt("patioDepth",patioDepth);
        preview.putInt("roofRise",(core-1)/4);
        preview.putInt("ridgeDirection",0);
        preview.put("blocks",blocks);
        preview.putInt("count",blocks.size());
        try {
            WaifuAxoluthuFurnishingLayouts.plan(preview,origin,0);
        } catch(IllegalStateException capacity) {
            if(extra>=8) throw new IllegalStateException("Workshop furnishing capacity unavailable",capacity);
            return createWorkshop(origin,seed,extra+2);
        }
        return preview;
    }

}
