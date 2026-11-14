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
    private static void addState(ListTag blocks, Map<String,Integer> counts, BlockPos pos,
                                 String material, int phase, String facing, String half) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("pos", pos.asLong());
        entry.putString("block", material);
        entry.putInt("phase", phase);
        if (!facing.isEmpty()) entry.putString("facing", facing);
        if (!half.isEmpty()) entry.putString("half", half);
        blocks.add(entry);
        counts.merge(material, 1, Integer::sum);
    }

    /** Starter cottage: left living room, right-front kitchen, right-back bedroom,
     * small raised storage loft and a true two-sided gable roof. New proposals only;
     * existing approved manifests are never silently regenerated. */
    public static CompoundTag create(CompoundTag project) {
        int w=project.getInt("width"), d=project.getInt("depth");
        if(w<7||d<7||w>31||d>31) throw new IllegalArgumentException("Invalid dimensions");
        BlockPos origin=BlockPos.of(project.getLong("origin"));
        ListTag blocks=new ListTag();
        Map<String,Integer> counts=new HashMap<>();
        int partition=w/2, split=d/2;
        // Main floor and external shell: interior remains empty.
        for(int x=0;x<w;x++) for(int z=0;z<d;z++) {
            addState(blocks,counts,origin.offset(x,0,z),"minecraft:oak_planks",0,"","");
            boolean edge=x==0||x==w-1||z==0||z==d-1;
            if(!edge)continue;
            for(int y=1;y<=5;y++) {
                boolean entrance=z==0&&x==partition&&y<=2;
                boolean window=y==3&&((z==0||z==d-1)&&(x==2||x==w-3)
                        ||(x==0||x==w-1)&&(z==2||z==d-3));
                if(entrance) {
                    if(y==1)addState(blocks,counts,origin.offset(x,y,z),"minecraft:oak_door",3,"north","");
                } else addState(blocks,counts,origin.offset(x,y,z),window?"minecraft:glass":"minecraft:oak_planks",window?3:1,"","");
            }
        }
        // Internal partitions and functional doors: three distinct accessible rooms.
        for(int z=1;z<d-1;z++) for(int y=1;y<=4;y++) {
            if((z==Math.max(2,split-1)||z==Math.min(d-3,split+1))&&y<=2) {
                if(y==1)addState(blocks,counts,origin.offset(partition,y,z),"minecraft:oak_door",3,"east","");
            } else addState(blocks,counts,origin.offset(partition,y,z),"minecraft:oak_planks",1,"","");
        }
        for(int x=partition+1;x<w-1;x++) for(int y=1;y<=4;y++) {
            if(x==Math.min(w-2,partition+2)&&y<=2) {
                if(y==1)addState(blocks,counts,origin.offset(x,y,split),"minecraft:oak_door",3,"south","");
            } else addState(blocks,counts,origin.offset(x,y,split),"minecraft:oak_planks",1,"","");
        }
        // A small loft deck, not a second full story. Reserve the ladder opening.
        int loftStart=Math.max(1,partition-1),loftEnd=Math.min(w-2,partition+2);
        int ladderX=partition+1, ladderZ=d-2;
        for(int x=loftStart;x<=loftEnd;x++) for(int z=split+1;z<d-1;z++) {
            if(x==ladderX&&z==ladderZ)continue;
            addState(blocks,counts,origin.offset(x,6,z),"minecraft:oak_planks",2,"","");
        }
        // Ladder on the east face of the center partition (support on its west).
        for(int y=1;y<=6;y++)addState(blocks,counts,origin.offset(ladderX,y,ladderZ),"minecraft:ladder",4,"east","");
        // One surface per roof column, not a solid vertical stack of stairs.
        // Stair facing points DOWN the slope, outward toward the eaves.
        for(int x=0;x<w;x++) {
            int rise=Math.min(x,w-1-x);
            int roofY=7+rise;
            String facing=x<partition?"west":x>partition?"east":"west";
            for(int z=0;z<d;z++)
                addState(blocks,counts,origin.offset(x,roofY,z),"minecraft:spruce_stairs",2,facing,"bottom");
            // Solid triangular gable ends only, never fill the attic volume.
            for(int y=6;y<roofY;y++) for(int z:new int[]{0,d-1})
                addState(blocks,counts,origin.offset(x,y,z),"minecraft:oak_planks",2,"","");
        }
        CompoundTag out=new CompoundTag();
        out.putInt("version",2);
        out.put("blocks",blocks);
        CompoundTag materials=new CompoundTag();
        for(var e:counts.entrySet())materials.putInt(e.getKey(),e.getValue());
        out.put("materials",materials);
        out.putInt("placements",blocks.size());
        out.putString("status","BUILDABLE");
        out.putString("layout","THREE_ROOM_STARTER_WITH_LOFT");
        return out;
    }
}
