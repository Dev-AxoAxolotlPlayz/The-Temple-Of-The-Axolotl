package com.devaxo.templeoftheaxolotl.entity;

import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import com.devaxo.templeoftheaxolotl.block.AbyssalChestEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Temporary equipment for approved exact-site construction only. */
public final class WaifuAxoluthuConstructionSite {
    private WaifuAxoluthuConstructionSite() {}

    public static String prepare(WaifuAxoluthuEntity axo, ServerLevel level, CompoundTag request) {
        ListTag planned = request.getCompound("blueprintPreview").getList("blocks",10);
        if (planned.isEmpty()) return "NO_BLUEPRINT";
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        int y=BlockPos.of(request.getLong("target")).getY();
        for(int i=0;i<planned.size();i++) {
            BlockPos b=BlockPos.of(planned.getCompound(i).getLong("pos"));
            minX=Math.min(minX,b.getX());maxX=Math.max(maxX,b.getX());maxZ=Math.max(maxZ,b.getZ());
        }
        // Place the kit behind the rear edge of the entire approved blueprint.
        // Try alternative strips when the nearest strip is obstructed.
        if (!request.contains("trial3SiteTable")) {
            boolean found=false;
            for(int offset=3;offset<=10 && !found;offset++) {
                BlockPos table=new BlockPos(minX,y,maxZ+offset);
                BlockPos chest=table.east();
                BlockPos grind=table.east(2);
                if (safe(level,table)&&safe(level,chest)&&safe(level,grind)) {
                    request.putLong("trial3SiteTable",table.asLong());
                    request.putLong("trial3SiteChest",chest.asLong());
                    request.putLong("trial3SiteGrindstone",grind.asLong());
                    found=true;
                }
            }
            if (!found) return "NO_CLEAR_EQUIPMENT_STRIP";
        }
        BlockPos[] places={BlockPos.of(request.getLong("trial3SiteChest")),
                BlockPos.of(request.getLong("trial3SiteTable")),
                BlockPos.of(request.getLong("trial3SiteGrindstone"))};
        Block[] blocks={ModBlocks.ABYSSAL_CHEST.get(),Blocks.CRAFTING_TABLE,Blocks.GRINDSTONE};
        String[] flags={"trial3PlacedChest","trial3PlacedTable","trial3PlacedGrindstone"};
        for(int i=0;i<3;i++) {
            if (request.getBoolean(flags[i])) {
                if (!level.getChunkSource().hasChunk(places[i].getX() >> 4, places[i].getZ() >> 4) || !level.getBlockState(places[i]).is(blocks[i]))
                    return "EQUIPMENT_MISSING_OR_CHANGED_"+i;
                continue;
            }
            if (!safe(level,places[i])) return "SITE_POSITION_OCCUPIED_"+i;
            if (!level.setBlock(places[i],blocks[i].defaultBlockState(),3)) return "PLACEMENT_FAILED_"+i;
            request.putBoolean(flags[i],true);
            if (i==0 && level.getBlockEntity(places[i]) instanceof AbyssalChestEntity chest)
                chest.setOwner(axo.getBondedPlayer());
            return "PLACING_EQUIPMENT_"+i;
        }
        request.putBoolean("trial3SiteReady",true);
        return "READY";
    }

    private static boolean safe(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP);
    }

    public static String cleanup(ServerLevel level, CompoundTag request) {
        String[] flags={"trial3PlacedGrindstone","trial3PlacedTable","trial3PlacedChest"};
        String[] positions={"trial3SiteGrindstone","trial3SiteTable","trial3SiteChest"};
        Block[] blocks={Blocks.GRINDSTONE,Blocks.CRAFTING_TABLE,ModBlocks.ABYSSAL_CHEST.get()};
        for(int i=0;i<3;i++) {
            if(!request.getBoolean(flags[i])) continue;
            BlockPos pos=BlockPos.of(request.getLong(positions[i]));
            if(!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4))return "WAITING_FOR_CHUNK";
            // If another block replaced the temporary one, do not touch it.
            if(level.getBlockState(pos).is(blocks[i])) {
                if(i==2 && level.getBlockEntity(pos) instanceof AbyssalChestEntity chest
                        && (!request.hasUUID("owner") || !request.getUUID("owner").equals(chest.owner())))
                    return "CHEST_OWNERSHIP_CHANGED";
                if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),3))return "REMOVE_FAILED_"+i;
            }
            request.putBoolean(flags[i],false);
            return "REMOVING_EQUIPMENT_"+i;
        }
        request.putBoolean("trial3SiteReady",false);
        return "DONE";
    }
}
