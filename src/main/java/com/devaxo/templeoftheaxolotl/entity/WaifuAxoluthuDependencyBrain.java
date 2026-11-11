package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** A small dependency executor: discover -> obtain materials -> craft -> place -> resume. */
public final class WaifuAxoluthuDependencyBrain {
    private WaifuAxoluthuDependencyBrain() {}
    // A workstation may recursively require a resource sourced from that same
    // workstation. Keep the guard across RecipeBrain -> DependencyBrain calls.
    private static final ThreadLocal<java.util.Set<Block>> ACTIVE_STATIONS =
            ThreadLocal.withInitial(java.util.HashSet::new);
    private static int count(SimpleContainer bag,Item item) {return WaifuAxoluthuMagicGathering.count(bag,item);}
    private static int planks(SimpleContainer bag) {
        int n=0;for(int i=0;i<bag.getContainerSize();i++)if(bag.getItem(i).is(ItemTags.PLANKS))n+=bag.getItem(i).getCount();return n;
    }
    private static boolean hasRoom(SimpleContainer bag,ItemStack item) {
        int space=0;for(int i=0;i<bag.getContainerSize();i++){
            ItemStack s=bag.getItem(i);
            if(s.isEmpty())space+=item.getMaxStackSize();
            else if(ItemStack.isSameItemSameComponents(s,item))space+=s.getMaxStackSize()-s.getCount();
        }return space>=item.getCount();
    }
    private static void take(SimpleContainer bag,Item item,int n) {
        for(int i=0;i<bag.getContainerSize()&&n>0;i++)if(bag.getItem(i).is(item)){
            int amount=Math.min(n,bag.getItem(i).getCount());bag.removeItem(i,amount);n-=amount;
        }
    }
    private static void takePlanks(SimpleContainer bag,int n) {
        for(int i=0;i<bag.getContainerSize()&&n>0;i++)if(bag.getItem(i).is(ItemTags.PLANKS)){
            int amount=Math.min(n,bag.getItem(i).getCount());bag.removeItem(i,amount);n-=amount;
        }
    }
    private static String wood(WaifuAxoluthuEntity axo) {
        SimpleContainer bag=axo.getAcquiredMaterials();
        if(count(bag,Items.OAK_LOG)>0){
            if(!hasRoom(bag,new ItemStack(Items.OAK_PLANKS,4)))return "INVENTORY_FULL";
            take(bag,Items.OAK_LOG,1);bag.addItem(new ItemStack(Items.OAK_PLANKS,4));return "CRAFTED_PLANKS";
        }
        return "HARVEST_"+WaifuAxoluthuForestry.step(axo);
    }
    private static String place(WaifuAxoluthuEntity axo,Block block) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        BlockPos center=axo.getHomePosition()==null?axo.blockPosition():axo.getHomePosition();
        for(int radius=1;radius<=6;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            BlockPos p=center.offset(dx,0,dz);
            if(!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4))continue;
            if(!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir())continue;
            if(!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))continue;
            if(p.distSqr(axo.blockPosition())>9){
                axo.getNavigation().moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,.85);
                return "APPROACH_BUILD_SITE";
            }
            level.setBlock(p,block.defaultBlockState(),3);
            take(axo.getAcquiredMaterials(),block.asItem(),1);
            axo.recordAcquisition("BUILT "+block.getName().getString()+" "+p.toShortString());
            axo.speak("built_"+block.getName().getString(),"I built a "+block.getName().getString()+"! :D");
            return "BUILT_WORKSTATION";
        }
        return "NO_SAFE_BUILD_SITE";
    }
    public static String ensure(WaifuAxoluthuEntity axo,Block block,long lastGather) {
        // The first grindstone is a player-provided bootstrap workstation.
        // Its stone slab cannot be acquired through Axoluthu's mining route
        // until that very workstation exists; never recursively craft it.
        if(block==Blocks.GRINDSTONE) {
            return WaifuAxoluthuMagicGathering.find(axo,Blocks.GRINDSTONE)!=null
                    ? "READY" : "WAITING_FOR_HOME_GRINDSTONE";
        }
        if(WaifuAxoluthuMagicGathering.find(axo,block)!=null)return "READY";
        java.util.Set<Block> active=ACTIVE_STATIONS.get();
        if(!active.add(block))return "BLOCKED_WORKSTATION_CYCLE_"+block.asItem();
        try {
            SimpleContainer bag=axo.getAcquiredMaterials();
            if(count(bag,block.asItem())>0)return place(axo,block);
            // Do not re-enter the same workstation dependency while recursively
            // resolving a recipe. Return a stable diagnostic instead of crashing.
            return "RECIPE_"+WaifuAxoluthuRecipeBrain.ensure(axo,block.asItem(),1,lastGather);
        } finally {
            active.remove(block);
            if(active.isEmpty())ACTIVE_STATIONS.remove();
        }
    }
}
