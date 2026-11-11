package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.HashMap;
import java.util.Map;

/** Pure deterministic geometry; does not touch the world or player inventory. */
public final class WaifuAxoluthuStage5Blueprint {
    private WaifuAxoluthuStage5Blueprint() {}
    private static boolean inside(int x, int z, int w, int d, String shape) {
        if ("L_WING".equals(shape) && x >= (w * 2) / 3 && z >= (d * 2) / 3) return false;
        if ("OFFSET_WING".equals(shape) && x >= (w * 3) / 4 && z < d / 3) return false;
        return true;
    }
    private static void add(ListTag blocks, Map<String,Integer> counts, BlockPos p, String material) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("pos",p.asLong());
        entry.putString("block",material);
        blocks.add(entry);
        counts.merge(material,1,Integer::sum);
    }
    public static CompoundTag create(CompoundTag project) {
        int w=project.getInt("width"), d=project.getInt("depth");
        if(w<5||d<5||w>31||d>31) throw new IllegalArgumentException("Invalid dimensions");
        String shape=project.getString("footprint");
        BlockPos origin=BlockPos.of(project.getLong("origin"));
        ListTag blocks=new ListTag();
        Map<String,Integer> counts=new HashMap<>();
        for(int x=0;x<w;x++) for(int z=0;z<d;z++) {
            if(!inside(x,z,w,d,shape)) continue;
            boolean north=!inside(x,z-1,w,d,shape), south=!inside(x,z+1,w,d,shape);
            boolean west=!inside(x-1,z,w,d,shape), east=!inside(x+1,z,w,d,shape);
            boolean boundary=north||south||west||east;
            add(blocks,counts,origin.offset(x,0,z),"minecraft:oak_planks");
            if(boundary) for(int y=1;y<=5;y++) {
                boolean doorway=(z==0 && x==w/2 && y<=2);
                boolean window=(y==3 && ((x%4==2 && (north||south)) || (z%4==2 && (west||east))));
                if(doorway) {
                    if(y==1) add(blocks,counts,origin.offset(x,y,z),"minecraft:oak_door");
                } else add(blocks,counts,origin.offset(x,y,z),window?"minecraft:glass":"minecraft:oak_planks");
            }
            // A flat structural ceiling under the future pitched roof, so the air gap remains 4 blocks.
            add(blocks,counts,origin.offset(x,6,z),"minecraft:oak_planks");
            // Stair-step roof profile along X; this is a volume/manifest plan, not oriented stair states.
            int rise=Math.min(3,Math.min(x,w-1-x));
            for(int y=7;y<=7+rise;y++) add(blocks,counts,origin.offset(x,y,z),"minecraft:spruce_stairs");
        }
        CompoundTag out=new CompoundTag();
        out.putInt("version",1);
        out.put("blocks",blocks);
        CompoundTag materials=new CompoundTag();
        for(var e:counts.entrySet()) materials.putInt(e.getKey(),e.getValue());
        out.put("materials",materials);
        out.putInt("placements",blocks.size());
        out.putString("status","BUILDABLE");
        return out;
    }
}
