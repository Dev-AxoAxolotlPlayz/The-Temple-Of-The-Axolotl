package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;

/** Stage 4 prototype: a single 5x5 oak-plank workshop, built one block per AI decision.
 * No world-edit shortcuts, no free items, no chunk loading, no replacement of existing blocks.
 * Conservative site selection: natural-looking grass/dirt support and completely empty build volume.
 * NOTE: grass/dirt alone cannot prove terrain was not placed by a player.
 */
public final class WaifuAxoluthuConstruction {
    private WaifuAxoluthuConstruction() {}
    private static final int TOTAL=58; // floor 25, corner pillars 8, roof 25

    private static BlockPos at(BlockPos o,int index) {
        if(index<25) return o.offset(index%5,0,index/5);
        if(index<33) {
            int corner=(index-25)/2;
            int x=(corner&1)==0?0:4;
            int z=(corner&2)==0?0:4;
            return o.offset(x,1+(index-25)%2,z);
        }
        int roof=index-33;
        return o.offset(roof%5,3,roof/5);
    }
    private static boolean ground(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.DIRT);
    }
    private static boolean siteSafe(ServerLevel level,BlockPos origin) {
        for(int x=-2;x<=6;x++)for(int z=-2;z<=6;z++) {
            BlockPos base=origin.offset(x,-1,z);
            if(!level.getChunkSource().hasChunk(base.getX()>>4,base.getZ()>>4))return false;
            if(x>=0&&x<5&&z>=0&&z<5 && !ground(level.getBlockState(base)))return false;
            for(int y=0;y<=4;y++) {
                BlockPos p=origin.offset(x,y,z);
                if(!level.getBlockState(p).isAir())return false;
                if(level.getBlockEntity(p)!=null)return false;
            }
        }
        // Avoid selecting a site occupied by a player, or right beside the companion.
        return level.getEntitiesOfClass(Player.class,
                new AABB(origin.getX()-2, origin.getY(), origin.getZ()-2,
                        origin.getX()+7, origin.getY()+5, origin.getZ()+7)).isEmpty();
    }
    private static BlockPos chooseSite(WaifuAxoluthuEntity axo,ServerLevel level) {
        BlockPos home=axo.getHomePosition()==null?axo.blockPosition():axo.getHomePosition();
        // Never treat coordinates from a different dimension as the same home.
        if(axo.hasManuallyAssignedHome() &&
                !axo.getHomeDimension().equals(level.dimension().location().toString()))return null;
        for(int radius=9;radius<=20;radius+=2)for(int side=0;side<4;side++) {
            for(int n=-radius;n<=radius;n+=3) {
                int dx=side==0?radius:side==1?-radius:n;
                int dz=side==2?radius:side==3?-radius:n;
                int x=home.getX()+dx,z=home.getZ()+dz;
                BlockPos test=new BlockPos(x,home.getY()+1,z);
                // Search nearby vertical levels, but never build underground or on water.
                for(int dy=-3;dy<=3;dy++) {
                    BlockPos p=test.offset(0,dy,0);
                    if(siteSafe(level,p))return p;
                }
            }
        }
        return null;
    }
    private static int count(WaifuAxoluthuEntity axo) {
        int total=0;
        var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(Items.OAK_PLANKS))
            total+=bag.getItem(i).getCount();
        return total;
    }
    private static boolean consume(WaifuAxoluthuEntity axo) {
        var bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++) {
            ItemStack s=bag.getItem(i);
            if(!s.is(Items.OAK_PLANKS)||s.isEmpty())continue;
            bag.removeItem(i,1);
            bag.setChanged();
            return true;
        }
        return false;
    }
    /** Roof blocks are three blocks above the floor. Stand INSIDE the workshop,
     * on its completed floor, rather than pathfinding to the roof block itself.
     * Re-evaluate candidates as the roof closes; never stand inside a solid block.
     */
    private static BlockPos roofStandingSpot(ServerLevel level, BlockPos origin,
                                              BlockPos target, WaifuAxoluthuEntity axo) {
        BlockPos best=null;
        double score=Double.MAX_VALUE;
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
            int x=target.getX()+dx,z=target.getZ()+dz;
            if(x<origin.getX()||x>=origin.getX()+5||z<origin.getZ()||z>=origin.getZ()+5)continue;
            BlockPos feet=new BlockPos(x,origin.getY()+1,z);
            if(!level.getBlockState(feet.below()).is(Blocks.OAK_PLANKS))continue;
            if(!level.getBlockState(feet).isAir()||!level.getBlockState(feet.above()).isAir())continue;
            if(!level.noCollision(axo,new AABB(x+.2,feet.getY(),z+.2,x+.8,feet.getY()+1.8,z+.8)))continue;
            double horizontal=(x+.5-target.getX()-.5)*(x+.5-target.getX()-.5)
                    +(z+.5-target.getZ()-.5)*(z+.5-target.getZ()-.5);
            if(horizontal>6.25)continue;
            double distance=axo.distanceToSqr(x+.5,feet.getY(),z+.5);
            double candidate=distance+horizontal*.35;
            if(candidate<score){score=candidate;best=feet;}
        }
        return best;
    }
    public static String step(WaifuAxoluthuEntity axo,long lastGather) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        if(axo.isStage4WorkshopComplete())return "COMPLETE";
        BlockPos origin=axo.getStage4WorkshopOrigin();
        if(origin==null) {
            origin=chooseSite(axo,level);
            if(origin==null)return "WAITING_FOR_SAFE_EMPTY_SITE";
            axo.setStage4WorkshopOrigin(origin);
            return "SITE_FOUND "+origin.toShortString();
        }
        int index=axo.getStage4WorkshopStep();
        if(index>=TOTAL) { axo.finishStage4Workshop(); return "COMPLETE"; }
        BlockPos target=at(origin,index);
        if(!level.getChunkSource().hasChunk(target.getX()>>4,target.getZ()>>4)) {
            axo.navigateToSavedLocation(origin,1.3);
            WaifuAxoluthuForestry.resetNavigationMemory(axo);
            return "RETURNING_TO_UNLOADED_WORKSHOP";
        }
        BlockState existing=level.getBlockState(target);
        // On reload, an already placed correct block can advance without consuming twice.
        if(existing.is(Blocks.OAK_PLANKS)) {
            axo.advanceStage4Workshop();
            return "RESUMED_EXISTING_BLOCK "+(index+1)+"/"+TOTAL;
        }
        if(!existing.isAir() && !existing.is(com.devaxo.templeoftheaxolotl.registry.ModBlocks.CELESTIAL_FOOTSTEP.get()) || level.getBlockEntity(target)!=null)return "BLOCKED_PROTECTED_OR_OCCUPIED "+target.toShortString();
        if(index<25 && !ground(level.getBlockState(target.below())))return "BLOCKED_SUPPORT_CHANGED";
        if(count(axo)==0) {
            String craft=WaifuAxoluthuRecipeBrain.ensure(axo,Items.OAK_PLANKS,4,lastGather);
            return "MATERIALS_"+craft;
        }
        // Reach check first; only pathfind when genuinely out of range.
        String approach=WaifuAxoluthuBuildNavigation.approach(axo,level,target,1.05);
        if(approach!=null) { axo.finishAction(); return approach; }
        axo.getNavigation().stop();
        axo.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5);
        double lookDx=target.getX()+.5-axo.getX(), lookDz=target.getZ()+.5-axo.getZ();
        float lookYaw=(float)(Math.atan2(lookDz,lookDx)*180.0/Math.PI)-90F;
        axo.setYRot(lookYaw);axo.setYBodyRot(lookYaw);axo.setYHeadRot(lookYaw);
        if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_FOCUSED,target,41,80))return "FOCUSED_PLACING";
        if(axo.getBoundingBox().intersects(new AABB(target))) {
            axo.finishAction();return WaifuAxoluthuBuildNavigation.clearSelf(axo,level,target);
        }
        if(!consume(axo))return "WAITING_FOR_PLANKS";
        if(!level.setBlock(target,Blocks.OAK_PLANKS.defaultBlockState(),3)) {
            // Restore material if the world rejected the placement.
            axo.getAcquiredMaterials().addItem(new ItemStack(Items.OAK_PLANKS));
            return "PLACEMENT_FAILED";
        }
        axo.finishAction();
        axo.advanceStage4Workshop();
        WaifuAxoluthuBuildNavigation.completed(axo);
        axo.recordAcquisition("STAGE4_PLACE oak_planks at="+target.toShortString()+" step="+(index+1)+"/"+TOTAL);
        if(index+1==TOTAL) { axo.finishStage4Workshop();return "COMPLETE"; }
        return "BUILDING "+(index+1)+"/"+TOTAL;
    }
}
