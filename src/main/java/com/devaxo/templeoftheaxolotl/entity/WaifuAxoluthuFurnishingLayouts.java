package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.Set;

/**
 * Data-driven furnishing placement manifest. Planning only: no world edits,
 * no owner approval changes, and no material spawning.
 */
public final class WaifuAxoluthuFurnishingLayouts {
    private WaifuAxoluthuFurnishingLayouts() {}

    public static final Set<String> TYPES = Set.of(
        "workshop", "infirmary", "small_house", "medium_house",
        "large_house", "library", "smithy", "storage", "town_center");

    public static ListTag plan(String type, BlockPos origin, int turns) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unknown type: " + type);
        ListTag out = new ListTag();
        // Door is the first furnishing so the building can be closed and inspected.
        add(out, origin, "oak_door", WaifuAxoluthuVillageStructures.width(type)/2, 1, 0, turns);
        switch (type) {
        case "workshop" -> {
            add(out, origin, "grindstone", 1, 1, 1, turns);
            add(out, origin, "composter", 5, 1, 1, turns);
            add(out, origin, "brewing_stand", 1, 1, 5, turns);
            add(out, origin, "crafting_table", 5, 1, 5, turns);
        }
        case "infirmary" -> {
            add(out, origin, "red_bed", 1, 1, 1, turns);
            add(out, origin, "cauldron", 5, 1, 1, turns);
            add(out, origin, "brewing_stand", 5, 1, 5, turns);
            add(out, origin, "stone_slab", 1, 1, 5, turns);
            add(out, origin, "flower_pot", 1, 2, 5, turns);
        }
        case "small_house" -> {
            add(out, origin, "red_bed", 1, 1, 1, turns);
            add(out, origin, "chest", 5, 1, 1, turns);
            add(out, origin, "smoker", 5, 1, 5, turns);
        }
        case "medium_house" -> {
            add(out, origin, "red_bed", 1, 1, 1, turns);
            add(out, origin, "red_bed", 3, 1, 1, turns);
            add(out, origin, "chest", 5, 1, 4, turns);
            add(out, origin, "chest", 5, 1, 5, turns);
            add(out, origin, "furnace", 1, 1, 5, turns);
            add(out, origin, "smoker", 3, 1, 5, turns);
        }
        case "large_house" -> {
            add(out, origin, "red_bed", 1, 1, 1, turns);
            add(out, origin, "red_bed", 3, 1, 1, turns);
            add(out, origin, "red_bed", 5, 1, 1, turns);
            add(out, origin, "red_bed", 1, 1, 3, turns);
            add(out, origin, "chest", 4, 1, 4, turns);
            add(out, origin, "chest", 5, 1, 4, turns);
            add(out, origin, "chest", 4, 1, 5, turns);
            add(out, origin, "chest", 5, 1, 5, turns);
            add(out, origin, "barrel", 1, 1, 5, turns);
            add(out, origin, "barrel", 2, 1, 5, turns);
            add(out, origin, "furnace", 3, 1, 5, turns);
            add(out, origin, "furnace", 3, 1, 3, turns);
            add(out, origin, "smoker", 5, 1, 2, turns);
            add(out, origin, "smoker", 5, 1, 3, turns);
        }
        case "library" -> {
            add(out, origin, "lectern", 3, 1, 3, turns);
            add(out, origin, "bookshelf", 1, 1, 1, turns);
            add(out, origin, "bookshelf", 1, 1, 8, turns);
            add(out, origin, "bookshelf", 2, 1, 1, turns);
            add(out, origin, "bookshelf", 2, 1, 8, turns);
            add(out, origin, "bookshelf", 3, 1, 1, turns);
            add(out, origin, "bookshelf", 3, 1, 8, turns);
            add(out, origin, "bookshelf", 4, 1, 1, turns);
            add(out, origin, "bookshelf", 4, 1, 8, turns);
            add(out, origin, "bookshelf", 5, 1, 1, turns);
            add(out, origin, "bookshelf", 5, 1, 8, turns);
            add(out, origin, "bookshelf", 6, 1, 1, turns);
            add(out, origin, "bookshelf", 6, 1, 8, turns);
            add(out, origin, "bookshelf", 7, 1, 1, turns);
            add(out, origin, "bookshelf", 7, 1, 8, turns);
            add(out, origin, "bookshelf", 8, 1, 1, turns);
            add(out, origin, "bookshelf", 8, 1, 8, turns);
            add(out, origin, "bookshelf", 1, 1, 2, turns);
            add(out, origin, "bookshelf", 1, 1, 3, turns);
            add(out, origin, "bookshelf", 1, 1, 4, turns);
            add(out, origin, "bookshelf", 1, 1, 5, turns);
            add(out, origin, "bookshelf", 1, 1, 6, turns);
            add(out, origin, "bookshelf", 1, 1, 7, turns);
            add(out, origin, "bookshelf", 8, 1, 2, turns);
            add(out, origin, "bookshelf", 8, 1, 3, turns);
            add(out, origin, "bookshelf", 8, 1, 4, turns);
            add(out, origin, "bookshelf", 8, 1, 5, turns);
            add(out, origin, "bookshelf", 8, 1, 6, turns);
            add(out, origin, "bookshelf", 8, 1, 7, turns);
        }
        case "smithy" -> {
            add(out, origin, "anvil", 1, 1, 1, turns);
            add(out, origin, "furnace", 5, 1, 1, turns);
            add(out, origin, "blast_furnace", 1, 1, 5, turns);
            add(out, origin, "smithing_table", 5, 1, 5, turns);
        }
        case "storage" -> {
            add(out, origin, "lectern", 3, 1, 3, turns);
            add(out, origin, "chest", 1, 1, 1, turns);
            add(out, origin, "chest", 2, 1, 1, turns);
            add(out, origin, "chest", 4, 1, 1, turns);
            add(out, origin, "chest", 5, 1, 1, turns);
            add(out, origin, "chest", 1, 1, 5, turns);
            add(out, origin, "chest", 2, 1, 5, turns);
            add(out, origin, "chest", 4, 1, 5, turns);
            add(out, origin, "chest", 5, 1, 5, turns);
        }
        case "town_center" -> {
            add(out, origin, "jukebox", 2, 1, 2, turns);
            add(out, origin, "bell", 4, 1, 2, turns);
        }
            default -> throw new IllegalArgumentException(type);
        }
        return out;
    }


    /**
     * Furnish the WHOLE approved plot. Rooms are design zones, not MCA-style
     * independent qualification boundaries. Required quantities are never reduced.
     * Keep the central aisle and the partition passage clear.
     */
    /** Furnish across the approved plot, preserving atomic bed/chest installers. */
    public static ListTag plan(CompoundTag preview, BlockPos origin, int turns) {
        if (preview.getInt("schema") != 22 || !preview.contains("partitionZ"))
            return plan(preview.getString("type"), origin, turns);
        String type = preview.getString("type");
        int w = preview.getInt("width");
        int part = preview.getInt("partitionZ");
        int passage = preview.getInt("passageX");
        int center = w / 2;
        ListTag out = new ListTag();
        add(out, origin, "oak_door", center, 1, 0, turns);
        if (preview.getBoolean("doubleEntrance"))
            add(out, origin, "oak_door", center + 1, 1, 0, turns);

        java.util.ArrayList<String> needs = new java.util.ArrayList<>();
        int requiredPairs = 0;
        switch (type) {
            case "workshop" -> java.util.Collections.addAll(needs, "grindstone", "composter", "brewing_stand", "crafting_table");
            case "smithy" -> java.util.Collections.addAll(needs, "anvil", "furnace", "blast_furnace", "smithing_table");
            case "infirmary" -> java.util.Collections.addAll(needs, "red_bed", "cauldron", "brewing_stand", "stone_slab", "flower_pot");
            case "small_house" -> java.util.Collections.addAll(needs, "red_bed", "chest", "smoker");
            case "medium_house" -> { java.util.Collections.addAll(needs, "red_bed", "red_bed", "furnace", "smoker"); requiredPairs = 1; }
            case "large_house" -> { java.util.Collections.addAll(needs, "red_bed", "red_bed", "red_bed", "red_bed", "barrel", "barrel", "furnace", "furnace", "smoker", "smoker"); requiredPairs = 2; }
            case "library" -> { needs.add("lectern"); for (int i = 0; i < 32; i++) needs.add("bookshelf"); }
            case "storage" -> { needs.add("lectern"); requiredPairs = 4; }
            case "town_center" -> java.util.Collections.addAll(needs, "jukebox", "bell");
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }

        // Plan in local coordinates. Every occupied footprint cell is grounded in
        // the actual approved shell, not in a guessed rectangular bounding box.
        java.util.HashSet<Long> floor = new java.util.HashSet<>();
        java.util.HashSet<Long> occupied = new java.util.HashSet<>();
        ListTag shell = preview.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < shell.size(); i++) {
            CompoundTag b = shell.getCompound(i);
            BlockPos pos = BlockPos.of(b.getLong("pos"));
            int x = pos.getX() - origin.getX(), z = pos.getZ() - origin.getZ();
            int y = pos.getY() - origin.getY();
            long key = key(x,z);
            if (y == 0 && "minecraft:cobblestone".equals(b.getString("block"))) floor.add(key);
            if (y == 1 && !"minecraft:air".equals(b.getString("block"))) occupied.add(key);
        }
        // Discover real rooms from the approved shell. Partitions are solid
        // at floor height; two-wide openings connect the navigable regions.
        // Design zones are derived from the wall layout rather than a fixed
        // front/rear split. Furniture counts still belong to the whole plot.
        java.util.ArrayList<java.util.ArrayList<int[]>> rooms = new java.util.ArrayList<>();
        java.util.ArrayList<int[]> candidates = new java.util.ArrayList<>();
        for (int z=1;z<w-1;z++) for(int x=1;x<w-1;x++) {
            long k=key(x,z);
            if (!floor.contains(k) || occupied.contains(k)) continue;
            // Reserve the main access spine and the doorway approaches.
            if (x==center || x==center+1) continue;
            if (Math.abs(z-part)<=1 && (x==passage || x==passage+1)) continue;
            // A furnishing position must border a wall/partition or an outer
            // boundary, and must leave at least one accessible neighbor.
            boolean edge=false;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                long neighbor=key(x+d[0],z+d[1]);
                if (!floor.contains(neighbor) || occupied.contains(neighbor)) edge=true;
            }
            if (edge) candidates.add(new int[]{x,z});
        }
        // Assign furniture to actual recursively generated room rectangles.
        // Each room has its own furnishings, but requirements are plot-wide.
        java.util.LinkedHashMap<Integer,java.util.ArrayList<int[]>> zones=new java.util.LinkedHashMap<>();
        ListTag designRooms=preview.getList("designRooms",net.minecraft.nbt.Tag.TAG_COMPOUND);
        if (!designRooms.isEmpty()) {
            for(int i=0;i<designRooms.size();i++)
                zones.put(i,new java.util.ArrayList<>());
            for(int[] cell:candidates) {
                for(int i=0;i<designRooms.size();i++) {
                    CompoundTag r=designRooms.getCompound(i);
                    if(cell[0]>=r.getInt("x") && cell[0]<r.getInt("x")+r.getInt("width")
                            && cell[1]>=r.getInt("z") && cell[1]<r.getInt("z")+r.getInt("depth")) {
                        zones.get(i).add(cell);break;
                    }
                }
            }
        } else {
            int crossX=preview.getInt("partitionX");
            int rearZ=preview.getInt("rearPartitionZ");
            for(int[] cell:candidates) {
                int x=cell[0],z=cell[1],zone;
                if(z<part) zone=preview.getBoolean("extraPartition") && x>crossX ? 1:0;
                else zone=preview.getBoolean("rearPartition") && z>rearZ ? 3:2;
                zones.computeIfAbsent(zone,unused->new java.util.ArrayList<>()).add(cell);
            }
        }
        rooms.addAll(zones.values());
        if (rooms.isEmpty()) throw new IllegalStateException("NO_FURNISHABLE_ROOMS:"+type);
        // Round-robin across all zones; no single room must individually meet
        // a building's furnishing quota.
        java.util.ArrayList<int[]> slots=new java.util.ArrayList<>();
        int max=0;
        for(var room:rooms) max=Math.max(max,room.size());
        for(int i=0;i<max;i++) for(var room:rooms)
            if(i<room.size()) slots.add(room.get(i));
        java.util.HashSet<Long> used = new java.util.HashSet<>();
        for (String need : needs) {
            int[] chosen = null;
            for (int i = 0; i < slots.size(); i++) {
                int[] cell = slots.get(i);
                if (used.contains(key(cell[0],cell[1]))) continue;
                // The existing bed installer puts the head one block SOUTH of its foot.
                if ("red_bed".equals(need) && (!floor.contains(key(cell[0],cell[1]+1))
                        || occupied.contains(key(cell[0],cell[1]+1))
                        || used.contains(key(cell[0],cell[1]+1))
                        || cell[1]+1 == part || cell[0] == center || cell[0] == center+1
                        || !sameRoom(cell[0],cell[1],cell[0],cell[1]+1,designRooms))) continue;
                chosen = cell;
                break;
            }
            if (chosen == null) throw new IllegalStateException("INSUFFICIENT_FURNISHING_CAPACITY:"+type+":"+need);
            add(out,origin,need,chosen[0],1,chosen[1],turns);
            used.add(key(chosen[0],chosen[1]));
            if ("red_bed".equals(need)) used.add(key(chosen[0],chosen[1]+1));
        }
        // Paired chests MUST be adjacent AND consecutive in the manifest, because
        // the existing atomic installer consumes exactly two consecutive entries.
        for (int pair=0;pair<requiredPairs;pair++) {
            int[] first=null,second=null;
            // Try a different room first for each pair. The existing atomic
            // chest installer still receives consecutive adjacent entries.
            for(int shift=0;shift<rooms.size() && first==null;shift++) {
                var room=rooms.get((pair+shift)%rooms.size());
                for(int[] a:room) {
                    if(used.contains(key(a[0],a[1]))) continue;
                    for(int[] b:room) {
                        if(Math.abs(a[0]-b[0])+Math.abs(a[1]-b[1])!=1 ||
                                used.contains(key(b[0],b[1]))) continue;
                        first=a;second=b;break;
                    }
                    if(first!=null) break;
                }
            }
            if(first==null) throw new IllegalStateException("INSUFFICIENT_DOUBLE_CHEST_SPACE:"+type);
            add(out,origin,"chest",first[0],1,first[1],turns);
            add(out,origin,"chest",second[0],1,second[1],turns);
            used.add(key(first[0],first[1]));used.add(key(second[0],second[1]));
        }
        // Furnish each usable design zone with a small, purpose-appropriate
        // accent AFTER all mandatory items and atomic chest pairs are reserved.
        // Decorative items never consume a required slot or block the main aisle.
        String[] accents = switch(type) {
            case "library" -> new String[]{"lectern", "barrel", "bookshelf"};
            case "storage" -> new String[]{"barrel", "crafting_table", "barrel"};
            case "workshop" -> new String[]{"barrel", "bookshelf", "barrel"};
            case "smithy" -> new String[]{"barrel", "crafting_table", "barrel"};
            case "infirmary" -> new String[]{"barrel", "flower_pot", "barrel"};
            case "town_center" -> new String[]{"bookshelf", "barrel", "crafting_table"};
            default -> new String[]{"barrel", "bookshelf", "flower_pot"};
        };
        int decorated = 0;
        for (int zone = 0; zone < rooms.size(); zone++) {
            java.util.ArrayList<int[]> room = rooms.get(zone);
            // Two accents at most, with enough spare space to remain useful.
            int budget = room.size() >= 10 ? 2 : room.size() >= 5 ? 1 : 0;
            for (int n=0;n<budget;n++) {
                int[] chosen = null;
                for (int[] cell : room) {
                    long k = key(cell[0],cell[1]);
                    if (used.contains(k)) continue;
                    // Do not decorate a narrow approach or isolate the room.
                    int free = 0;
                    for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        long adj=key(cell[0]+d[0],cell[1]+d[1]);
                        if(floor.contains(adj) && !occupied.contains(adj) && !used.contains(adj))
                            free++;
                    }
                    if(free >= 2) { chosen=cell;break; }
                }
                if(chosen==null) break;
                String accent=accents[(zone+n)%accents.length];
                // Floor-level flower pots require a floor below, like other
                // accents; use barrels where the position is too constrained.
                add(out,origin,accent,chosen[0],1,chosen[1],turns);
                used.add(key(chosen[0],chosen[1]));
                decorated++;
            }
        }
        return out;
    }

    private static boolean sameRoom(int x1,int z1,int x2,int z2,ListTag rooms) {
        if(rooms.isEmpty()) return true;
        for(int i=0;i<rooms.size();i++) {
            CompoundTag r=rooms.getCompound(i);
            int x=r.getInt("x"),z=r.getInt("z"),w=r.getInt("width"),d=r.getInt("depth");
            if(x1>=x && x1<x+w && x2>=x && x2<x+w
                    && z1>=z && z1<z+d && z2>=z && z2<z+d) return true;
        }
        return false;
    }

    private static long key(int x,int z) { return (((long)x)<<32) ^ (z & 0xffffffffL); }

    /** The special town center is not considered finished without its portal/statue. */
    public static boolean needsSpecialStructure(String type) {
        return "town_center".equals(type);
    }

    private static void add(ListTag out, BlockPos origin, String block,
                            int x, int y, int z, int turns) {
        int rx = x, rz = z;
        for (int i = 0; i < Math.floorMod(turns, 4); i++) {
            int nx = -rz;
            rz = rx;
            rx = nx;
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("block", "minecraft:" + block);
        entry.putLong("pos", origin.offset(rx, y, rz).asLong());
        entry.putInt("furnishingSchema", 21);
        out.add(entry);
    }

    public static CompoundTag proposal(String type, BlockPos origin, int turns) {
        CompoundTag result = new CompoundTag();
        result.putString("type", type);
        result.putLong("origin", origin.asLong());
        result.putInt("quarterTurns", Math.floorMod(turns, 4));
        result.putBoolean("requiresOwnerApproval", true);
        result.putBoolean("readyToConstruct", false);
        result.put("furnishings", plan(type, origin, turns));
        return result;
    }
}
