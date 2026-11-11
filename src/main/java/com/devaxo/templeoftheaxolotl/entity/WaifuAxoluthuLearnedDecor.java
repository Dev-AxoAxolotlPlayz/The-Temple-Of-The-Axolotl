package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Post-build lesson application. Never changes the 82-step storage-hut blueprint.
 * Only adds to empty spaces; existing/player-modified blocks are never replaced.
 * Each step is persisted in the companion's existing architectural NBT.
 */
public final class WaifuAxoluthuLearnedDecor {
    private WaifuAxoluthuLearnedDecor() {}
    private static boolean knows(WaifuAxoluthuEntity axo,String category) {
        ListTag lessons=axo.getArchitecturalLearning().getList("lessons",10);
        for(int i=0;i<lessons.size();i++)if(lessons.getCompound(i).getInt(category)>0)return true;
        return false;
    }
    private static boolean consume(WaifuAxoluthuEntity axo,Item item) {
        var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(item)) {
            bag.removeItem(i,1);bag.setChanged();return true;
        }
        return false;
    }
    private record Detail(BlockPos pos,BlockState state,Item item,boolean sign) {}
    private static Detail detail(BlockPos origin,int step,boolean roof) {
        // Place a modest gabled stair/slab roof CAP on top of the original roof.
        // Preserve the original roof and the completed construction count.
        if(roof && step<25) {
            int z=step%5;
            int cross=step/5;
            int x=cross;
            int y=4+Math.min(x,4-x);
            if(x==2)return new Detail(origin.offset(x,y,z),Blocks.OAK_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.BOTTOM),Items.OAK_SLAB,false);
            Direction facing=x<2?Direction.WEST:Direction.EAST;
            return new Detail(origin.offset(x,y,z),Blocks.OAK_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING,facing),Items.OAK_STAIRS,false);
        }
        return new Detail(origin.offset(1,2,-1),Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING,Direction.NORTH),Items.OAK_SIGN,true);
    }
    public static String step(WaifuAxoluthuEntity axo,long lastGather) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        BlockPos origin=axo.getSettlementStorageOrigin();
        if(origin==null || !axo.isSettlementStorageComplete())return "NOT_READY";
        CompoundTag memory=axo.getArchitecturalLearning();
        CompoundTag progress=memory.getCompound("storageDecoration");
        if(progress.getBoolean("done"))return "DONE";
        boolean roof=knows(axo,"stairs") && knows(axo,"slabs");
        boolean sign=knows(axo,"signs") || knows(axo,"hanging_signs");
        if(!roof && !sign)return "NO_APPLICABLE_LESSONS";
        int index=progress.getInt("step");
        int max=(roof?25:0)+(sign?1:0);
        if(index>=max) {
            progress.putBoolean("done",true);memory.put("storageDecoration",progress);
            return "DONE";
        }
        Detail target=detail(origin,roof?index:25,roof);
        BlockPos pos=target.pos();
        if(!level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))return "CHUNK_UNLOADED";
        BlockState old=level.getBlockState(pos);
        // Skip rather than overwriting player edits, plants, or block entities.
        if(!old.isAir() || level.getBlockEntity(pos)!=null) {
            progress.putInt("step",index+1);memory.put("storageDecoration",progress);
            return "PRESERVED_EXISTING_BLOCK";
        }
        if(target.sign() && !level.getBlockState(pos.relative(Direction.SOUTH)).isFaceSturdy(level,pos.relative(Direction.SOUTH),Direction.NORTH)) {
            progress.putInt("step",index+1);memory.put("storageDecoration",progress);
            return "NO_SIGN_SUPPORT";
        }
        if(axo.getBoundingBox().intersects(new AABB(pos)))return "WAITING_FOR_SELF_CLEARANCE";
        double dx=pos.getX()+.5-axo.getX(),dz=pos.getZ()+.5-axo.getZ();
        double horizontal=Math.sqrt(dx*dx+dz*dz);
        if(horizontal>3 || Math.abs(pos.getY()+.5-axo.getEyeY())>8) {
            axo.navigateToSavedLocation(origin.offset(2,1,2),1.1);
            return "APPROACH_DECORATION";
        }
        axo.getNavigation().stop();
        axo.getLookControl().setLookAt(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        float yaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90;
        axo.setYRot(yaw);axo.setYBodyRot(yaw);axo.setYHeadRot(yaw);
        if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED,pos,41,80))return "FOCUSED_DECORATION";
        if(!consume(axo,target.item())) {
            axo.finishAction();
            return "MATERIALS_"+WaifuAxoluthuRecipeBrain.ensure(axo,target.item(),1,lastGather);
        }
        if(!level.setBlock(pos,target.state(),3))return "PLACEMENT_FAILED";
        if(target.sign() && level.getBlockEntity(pos) instanceof SignBlockEntity signEntity) {
            signEntity.setText(signEntity.getText(true).setMessage(0,Component.literal("Our Storage Hut")),true);
            signEntity.setChanged();
            level.sendBlockUpdated(pos,target.state(),target.state(),3);
        }
        axo.finishAction();
        progress.putInt("step",index+1);memory.put("storageDecoration",progress);
        return "DECORATED_"+(index+1)+"/"+max;
    }
}
