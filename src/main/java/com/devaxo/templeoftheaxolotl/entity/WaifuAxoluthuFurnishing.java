package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Furnish the starter workshop using existing carried items and the companion's own recorded smithing table.
 * Does not claim ownership of arbitrary nearby player workstations or beds.
 */
public final class WaifuAxoluthuFurnishing {
    private WaifuAxoluthuFurnishing() {}
    private static boolean loaded(ServerLevel level,BlockPos pos) {
        return level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4);
    }
    public static String step(WaifuAxoluthuEntity axo) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        BlockPos origin=axo.getStage4WorkshopOrigin();
        if(origin==null)return "NO_WORKSHOP";
        BlockPos smithTarget=origin.offset(1,1,1);
        if(!loaded(level,smithTarget)) {axo.navigateToSavedLocation(origin,1.3);return "RETURN_TO_WORKSHOP";}
        BlockPos previous=axo.getLastWorkshopPlacement();
        if(!level.getBlockState(smithTarget).is(Blocks.SMITHING_TABLE) && previous!=null
                && !previous.equals(smithTarget) && loaded(level,previous)
                && level.getBlockState(previous).is(Blocks.SMITHING_TABLE)
                && level.getBlockState(smithTarget).isAir() && level.getBlockEntity(previous)==null) {
            if(axo.distanceToSqr(previous.getX()+.5,previous.getY(),previous.getZ()+.5)>9) {
                axo.navigateToSavedLocation(previous,1.05);return "APPROACH_OWN_SMITHING_TABLE";
            }
            if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_WORKSTATION,previous,30,60))return "PACKING_SMITHING_TABLE";
            // Both positions checked before mutation. Restore original if destination placement fails.
            var state=level.getBlockState(previous);
            if(!level.removeBlock(previous,false))return "CANNOT_COLLECT_SMITHING_TABLE";
            if(!level.setBlock(smithTarget,state,3)) {
                level.setBlock(previous,state,3);return "RELOCATION_FAILED_RESTORED";
            }
            axo.finishAction();
            return "MOVED_SMITHING_TABLE_INDOORS";
        }
        // Beds require a genuinely acquired item; never move a player's existing bed.
        BlockPos foot=origin.offset(3,1,1);
        BlockPos head=foot.south();
        if(level.getBlockState(foot).is(Blocks.WHITE_BED) && level.getBlockState(head).is(Blocks.WHITE_BED))
            return "FURNISHED";
        if(!level.getBlockState(foot).isAir() || !level.getBlockState(head).isAir())return "BED_SPACE_OCCUPIED";
        int slot=-1;
        var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.WHITE_BED)) {slot=i;break;}
        if(slot<0)return "WAITING_FOR_OWN_WHITE_BED";
        if(axo.distanceToSqr(foot.getX()+.5,foot.getY(),foot.getZ()+.5)>9) {
            axo.navigateToSavedLocation(foot,1.05);return "APPROACH_BED_POSITION";
        }
        if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED,foot,41,80))return "PLACING_BED";
        var footState=Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH)
                .setValue(BedBlock.PART,BedPart.FOOT);
        var headState=Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH)
                .setValue(BedBlock.PART,BedPart.HEAD);
        if(!level.setBlock(foot,footState,3))return "BED_PLACEMENT_FAILED";
        if(!level.setBlock(head,headState,3)) {
            level.removeBlock(foot,false);return "BED_HEAD_PLACEMENT_FAILED";
        }
        bag.removeItem(slot,1);
        axo.finishAction();
        return "FURNISHED";
    }
}
