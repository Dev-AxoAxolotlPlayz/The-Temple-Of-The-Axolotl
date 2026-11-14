package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Persistent second building: a small dedicated storage hut with a paired double chest.
 * One action per AI decision. All placements consume real items. Never clears existing structures.
 */
public final class WaifuAxoluthuSettlement {
    private WaifuAxoluthuSettlement() {}
    private static final int WIDTH=5;
    private static final int TOTAL=25+30+25+2+1; // floor, walls, roof, double chest, door LAST
    private record Piece(BlockPos pos,BlockState state,Item material) {}
    private static Piece piece(BlockPos o,int index) {
        if(index<25)return new Piece(o.offset(index%5,0,index/5),Blocks.OAK_PLANKS.defaultBlockState(),Items.OAK_PLANKS);
        index-=25;
        for(int y=1;y<=2;y++)for(int z=0;z<5;z++)for(int x=0;x<5;x++) {
            if(x!=0&&x!=4&&z!=0&&z!=4)continue;
            if(x==2&&z==0)continue; // accessible entry
            if(index--==0)return new Piece(o.offset(x,y,z),Blocks.OAK_PLANKS.defaultBlockState(),Items.OAK_PLANKS);
        }
        if(index<25)return new Piece(o.offset(index%5,3,index/5),Blocks.OAK_PLANKS.defaultBlockState(),Items.OAK_PLANKS);
        index-=25;
        if(index==2)return new Piece(o.offset(2,1,0),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH).setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER),Items.OAK_DOOR);
        BlockState chest=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH)
                .setValue(ChestBlock.TYPE,index==0?ChestType.LEFT:ChestType.RIGHT);
        return new Piece(o.offset(1+index,1,3),chest,Items.CHEST);
    }
    private static boolean loaded(ServerLevel level,BlockPos p) {
        return level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4);
    }
    private static boolean site(ServerLevel level,BlockPos o) {
        for(int x=-1;x<=5;x++)for(int z=-1;z<=5;z++)for(int y=0;y<=4;y++) {
            BlockPos p=o.offset(x,y,z);
            if(!loaded(level,p))return false;
            if(x>=0&&x<5&&z>=0&&z<5) {
                if(y==0 && !level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return false;
                if(!level.getBlockState(p).isAir() || level.getBlockEntity(p)!=null)return false;
            }
        }
        return true;
    }
    private static BlockPos choose(WaifuAxoluthuEntity axo,ServerLevel level) {
        BlockPos workshop=axo.getStage4WorkshopOrigin();
        if(workshop==null)return null;
        for(int r=10;r<=25;r+=3)for(int side=0;side<4;side++) {
            int dx=side==0?r:side==1?-r:0;
            int dz=side==2?r:side==3?-r:0;
            for(int dy=-3;dy<=3;dy++) {
                BlockPos candidate=workshop.offset(dx,dy,dz);
                if(site(level,candidate))return candidate;
            }
        }
        return null;
    }
    private static int count(WaifuAxoluthuEntity axo,Item item) {
        int n=0;
        var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(item))n+=bag.getItem(i).getCount();
        return n;
    }
    private static boolean consume(WaifuAxoluthuEntity axo,Item item) {
        var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(item)) {
            bag.removeItem(i,1);bag.setChanged();return true;
        }
        return false;
    }
    // Functional placements use the hut's central X/Z as their dedicated
    // standing destination, never a generic spot outside the building.
    private static String reachInteriorCenter(WaifuAxoluthuEntity axo,BlockPos origin) {
        BlockPos outside=origin.offset(2,1,-1);
        BlockPos center=origin.offset(2,1,2);
        double cx=center.getX()+0.5, cz=center.getZ()+0.5;
        double dx=axo.getX()-cx, dz=axo.getZ()-cz;
        boolean atCenter=dx*dx+dz*dz<=0.75*0.75
                && Math.abs(axo.getY()-center.getY())<1.25;
        if(atCenter) {
            axo.getNavigation().stop();
            return null;
        }
        // Approach the entrance from outside before routing to the center.
        // Once near the entrance, route through it to the center.
        boolean nearEntry=axo.getZ()>=origin.getZ()-1.25
                && axo.getZ()<=origin.getZ()+1.75
                && Math.abs(axo.getX()-(origin.getX()+2.5))<=1.6;
        boolean withinHut=axo.getX()>origin.getX()+0.5
                && axo.getX()<origin.getX()+4.5
                && axo.getZ()>origin.getZ()+0.5
                && axo.getZ()<origin.getZ()+4.5;
        BlockPos waypoint=(nearEntry || withinHut)?center:outside;
        axo.finishAction();
        if(axo.getNavigation().isDone())
            axo.getNavigation().moveTo(waypoint.getX()+0.5,waypoint.getY(),waypoint.getZ()+0.5,1.15);
        return "WALK_TO_STORAGE_CENTER target="+waypoint.toShortString();
    }
    private static boolean clearSight(ServerLevel level,WaifuAxoluthuEntity axo,BlockPos target) {
        Vec3 eye=axo.getEyePosition();
        Vec3 destination=Vec3.atCenterOf(target);
        return level.clip(new ClipContext(eye,destination,ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,axo)).getType()==HitResult.Type.MISS;
    }
    public static String step(WaifuAxoluthuEntity axo,long lastGather) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        if(axo.isSettlementStorageComplete())return "COMPLETE";
        BlockPos origin=axo.getSettlementStorageOrigin();
        if(origin==null) {
            origin=choose(axo,level);
            if(origin==null)return "WAITING_FOR_SAFE_STORAGE_SITE";
            axo.setSettlementStorageOrigin(origin);
            return "STORAGE_SITE_FOUND "+origin.toShortString();
        }
        String permit = WaifuAxoluthuBuildingPermit.await(axo, level, "storage", origin);
        if (permit != null) return permit;
        int index=axo.getSettlementStorageStep();
        if(index>=TOTAL) {axo.finishSettlementStorage(); WaifuAxoluthuBuildingPermit.complete(axo, "storage"); WaifuAxoluthuVillageRegistry.registerCompleted(axo, "storage", axo.getSettlementStorageOrigin(), 5, 4, 5);return "COMPLETE";}
        Piece p=piece(origin,index);
        if(!loaded(level,p.pos())) {
            axo.navigateToSavedLocation(origin,1.3);
            return "RETURNING_TO_STORAGE_SITE";
        }
        BlockState old=level.getBlockState(p.pos());
        if(old.equals(p.state()) || (p.material()==Items.CHEST && old.is(Blocks.CHEST))
                || (p.material()==Items.OAK_DOOR && old.is(Blocks.OAK_DOOR))) {
            if(p.material()==Items.CHEST)axo.authorizeContainer(p.pos(),axo.getBondedPlayer(),true);
            axo.advanceSettlementStorageStep();
            return "RESUMED "+(index+1)+"/"+TOTAL;
        }
        if(!old.isAir() && !old.is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.CELESTIAL_FOOTSTEP.get()) || level.getBlockEntity(p.pos())!=null)return "PROTECTED_BLOCK "+p.pos().toShortString();
        if(count(axo,p.material())<1)return "MATERIALS_"+WaifuAxoluthuRecipeBrain.ensure(axo,p.material(),1,lastGather);
        // A door occupies the entryway, not the interior. Placing it from the
        // interior center is unnecessary and can repeatedly restart the action
        // when line-of-sight or navigation checks disagree. Use a reachable
        // exterior/adjacent stand position for doors instead.
        boolean door=p.material()==Items.OAK_DOOR;
        boolean functional=p.material()==Items.CHEST;
        if(functional) {
            String entering=reachInteriorCenter(axo,origin);
            if(entering!=null)return entering;
            if(!clearSight(level,axo,p.pos())) {
                axo.finishAction();
                BlockPos interior=origin.offset(2,1,2);
                if(axo.getNavigation().isDone())axo.getNavigation().moveTo(interior.getX()+.5,interior.getY(),interior.getZ()+.5,1.15);
                return "INTERIOR_LINE_OF_SIGHT_BLOCKED target="+p.pos().toShortString();
            }
        }
        if(!functional) {
            String approach=WaifuAxoluthuBuildNavigation.approach(axo,level,p.pos(),1.15);
            if(approach!=null) { axo.finishAction(); return approach; }
        }
        // Doors require a clear upper half. A roof or player block here must
        // not be overwritten, and a blocked door must not animate forever.
        if(door) {
            BlockPos upper=p.pos().above();
            if(!loaded(level,upper))return "WAITING_FOR_DOOR_UPPER_CHUNK";
            BlockState upperState=level.getBlockState(upper);
            if(!upperState.isAir() && !upperState.is(Blocks.OAK_DOOR)
                    && !upperState.is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.CELESTIAL_FOOTSTEP.get()))
                return "PROTECTED_DOOR_UPPER "+upper.toShortString();
        }
        axo.getNavigation().stop();
        axo.getLookControl().setLookAt(p.pos().getX()+.5,p.pos().getY()+.5,p.pos().getZ()+.5);
        double lookDx=p.pos().getX()+.5-axo.getX(), lookDz=p.pos().getZ()+.5-axo.getZ();
        float lookYaw=(float)(Math.atan2(lookDz,lookDx)*180.0/Math.PI)-90F;
        axo.setYRot(lookYaw);axo.setYBodyRot(lookYaw);axo.setYHeadRot(lookYaw);
        if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED,p.pos(),41,80))return "FOCUSED_BUILD";
        if(axo.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(p.pos()))) {
            axo.finishAction();return WaifuAxoluthuBuildNavigation.clearSelf(axo,level,p.pos());
        }
        if(!consume(axo,p.material()))return "MATERIALS_MISSING";
        if(!level.setBlock(p.pos(),p.state(),3)) {
            axo.getAcquiredMaterials().addItem(new ItemStack(p.material()));
            return "PLACE_FAILED";
        }
        if(door) {
            BlockPos top=p.pos().above();
            BlockState topState=level.getBlockState(top);
            if(topState.isAir() || topState.is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.CELESTIAL_FOOTSTEP.get()))
                level.setBlock(top,p.state().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
        }
        axo.finishAction();
        if(p.material()==Items.CHEST)axo.authorizeContainer(p.pos(),axo.getBondedPlayer(),true);
        axo.advanceSettlementStorageStep();
        WaifuAxoluthuBuildNavigation.completed(axo);
        axo.recordAcquisition("STORAGE_HUT_PLACE "+p.material()+" at "+p.pos().toShortString()+" "+(index+1)+"/"+TOTAL);
        if(index+1>=TOTAL) {axo.finishSettlementStorage(); WaifuAxoluthuBuildingPermit.complete(axo, "storage"); WaifuAxoluthuVillageRegistry.registerCompleted(axo, "storage", axo.getSettlementStorageOrigin(), 5, 4, 5);return "COMPLETE";}
        return "BUILDING_STORAGE "+(index+1)+"/"+TOTAL;
    }
}
