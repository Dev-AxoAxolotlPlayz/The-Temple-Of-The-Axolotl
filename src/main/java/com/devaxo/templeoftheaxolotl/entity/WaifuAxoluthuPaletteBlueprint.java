package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Visual-only multi-material architecture proposals.
 * Never edits terrain. State-aware placement is a separate future builder.
 */
public final class WaifuAxoluthuPaletteBlueprint {
    private WaifuAxoluthuPaletteBlueprint() {}
    private static void put(Map<Long, String> blocks, BlockPos p, String id) {
        blocks.put(p.asLong(), "minecraft:" + id);
    }
    public static CompoundTag create(String type, BlockPos origin) {
        if (!"workshop".equals(type) && !"storage".equals(type))
            throw new IllegalArgumentException("Unsupported palette blueprint: " + type);
        Map<Long, String> blocks = new LinkedHashMap<>();
        // Seven-by-seven shell, porch extends two blocks north, roof overhangs one.
        for (int x=0;x<7;x++) for (int z=0;z<7;z++)
            put(blocks,origin.offset(x,0,z),"cobblestone");
        for (int y=1;y<=4;y++) for (int x=0;x<7;x++) for (int z=0;z<7;z++) {
            boolean edge=x==0||x==6||z==0||z==6;
            if (!edge) continue;
            if ((x==0||x==6)&&(z==0||z==6)) {
                put(blocks,origin.offset(x,y,z),"stripped_oak_log");
            } else if (z==0 && x==3 && y<=2) {
                // Entrance remains clear; later state-aware builder installs door.
            } else if (y==2 && ((z==0||z==6)&&(x==2||x==4)
                    || (x==0||x==6)&&(z==2||z==4))) {
                put(blocks,origin.offset(x,y,z),(x+z)%2==0?"glass":"glass");
            } else {
                put(blocks,origin.offset(x,y,z),"oak_planks");
            }
        }
        // Roof: slab perimeter overhang, stair accent along front/rear eaves.
        for (int x=-1;x<=7;x++) for (int z=-1;z<=7;z++) {
            boolean eave=x==-1||x==7||z==-1||z==7;
            put(blocks,origin.offset(x,5,z),eave?"oak_slab":"spruce_planks");
        }
        for (int x=0;x<7;x++) {
            put(blocks,origin.offset(x,5,-1),"spruce_stairs");
            put(blocks,origin.offset(x,5,7),"spruce_stairs");
        }
        // Small porch and fence railings, open at the front center.
        for (int x=1;x<=5;x++) for (int z=-2;z<=-1;z++)
            put(blocks,origin.offset(x,0,z),"cobblestone");
        for (int z=-2;z<=-1;z++) {
            put(blocks,origin.offset(1,1,z),"oak_fence");
            put(blocks,origin.offset(5,1,z),"oak_fence");
        }
        for (int x=1;x<=5;x++) if (x!=3) {
            put(blocks,origin.offset(x,1,-2),"cobblestone_wall");
        }
        // Wall-mounted lighting, in addition to mandatory interior lighting
        // to be resolved by a future orientation-aware furnishing stage.
        put(blocks,origin.offset(2,3,0),"wall_torch");
        put(blocks,origin.offset(4,3,0),"wall_torch");
        // Interior light placeholders are ceiling-hung lanterns.
        put(blocks,origin.offset(2,4,3),"lantern");
        put(blocks,origin.offset(4,4,3),"lantern");

        ListTag entries=new ListTag();
        for (var e:blocks.entrySet()) {
            CompoundTag block=new CompoundTag();
            block.putLong("pos",e.getKey());
            block.putString("block",e.getValue());
            entries.add(block);
        }
        CompoundTag result=new CompoundTag();
        result.putInt("schema",18);
        result.putString("type",type);
        result.putString("status","PREVIEW_ONLY");
        result.putLong("origin",origin.asLong());
        result.putLong("entrance",origin.offset(3,1,-2).asLong());
        result.putString("facing","north");
        result.put("blocks",entries);
        result.putInt("count",entries.size());
        return result;
    }
}
