package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Read-only, bounded, loaded-chunk-only building key-block discovery. */
public final class WaifuAxoluthuArchitecture {
    private WaifuAxoluthuArchitecture() {}
    public static String scan(WaifuAxoluthuEntity axo) {
        if (!(axo.level() instanceof ServerLevel level)) return "SERVER_ONLY";
        BlockPos center=axo.getHomePosition()==null?axo.blockPosition():axo.getHomePosition();
        int crafting=0, smithing=0, furnaces=0, storage=0, grindstones=0, composters=0, maps=0;
        int visited=0;
        BlockPos nearestCraft=null, nearestSmith=null;
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-12,-4,-12),center.offset(12,4,12))) {
            if(!level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)) continue;
            visited++;
            var block=level.getBlockState(pos).getBlock();
            if(block==Blocks.CRAFTING_TABLE) {crafting++;if(nearestCraft==null)nearestCraft=pos.immutable();}
            else if(block==Blocks.SMITHING_TABLE) {smithing++;if(nearestSmith==null)nearestSmith=pos.immutable();}
            else if(block==Blocks.FURNACE || block==Blocks.BLAST_FURNACE || block==Blocks.SMOKER) furnaces++;
            else if(block==Blocks.CHEST || block==Blocks.TRAPPED_CHEST || block==Blocks.BARREL) storage++;
            else if(block==Blocks.GRINDSTONE) grindstones++;
            else if(block==Blocks.COMPOSTER) composters++;
            else if(block==Blocks.CARTOGRAPHY_TABLE) maps++;
        }
        // Key-block evidence, not definitive enclosed-room detection.
        String type=smithing>0||crafting>0 ? "WORKSHOP_CANDIDATE"
                : storage>0 ? "STORAGE_CANDIDATE" : "UNCLASSIFIED";
        return "type="+type+" sampled="+visited+" crafting="+crafting
                +" smithing="+smithing+" furnaces="+furnaces+" storage="+storage
                +" grindstones="+grindstones+" composters="+composters
                +" cartography="+maps+" firstCraft="+nearestCraft+" firstSmith="+nearestSmith
                +" modifications=NONE";
    }
}
