package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/** Bounded, time-gated gathering; no terrain changes or unloaded chunk access. */
public final class WaifuAxoluthuMagicGathering {
    private WaifuAxoluthuMagicGathering() {}
    public record Result(String status, boolean completed) {}
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(.26f,.96f,.94f),1.0f);
    public static void poof(ServerLevel level,BlockPos pos) {
        level.sendParticles(CYAN,pos.getX()+.5,pos.getY()+1.08,pos.getZ()+.5,13,.23,.15,.23,.04);
    }
    public static BlockPos find(WaifuAxoluthuEntity axo,Block block) {
        if(!(axo.level() instanceof ServerLevel level)) return null;
        BlockPos center=axo.getHomePosition()==null?axo.blockPosition():axo.getHomePosition();
        BlockPos nearest=null;
        double best=Double.MAX_VALUE;
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-12,-3,-12),center.offset(12,3,12))) {
            if(!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4))continue;
            if(level.getBlockState(p).getBlock()!=block)continue;
            double dist=p.distSqr(axo.blockPosition());
            if(dist<best){best=dist;nearest=p.immutable();}
        }
        return nearest;
    }
    public static int count(SimpleContainer inv,Item item) {
        int n=0;
        for(int i=0;i<inv.getContainerSize();i++)if(inv.getItem(i).is(item))n+=inv.getItem(i).getCount();
        return n;
    }
    public static Result gather(WaifuAxoluthuEntity axo,Item item,long lastTick,int duration) {
        if(!(axo.level() instanceof ServerLevel level))return new Result("SERVER_ONLY",false);
        Block station;
        if(item==Items.RAW_IRON || item==Items.RAW_GOLD || item==Items.RAW_COPPER ||
           item==Items.COAL || item==Items.COBBLESTONE || item==Items.COBBLED_DEEPSLATE ||
           item==Items.STONE || item==Items.DIAMOND || item==Items.REDSTONE ||
           item==Items.LAPIS_LAZULI || item==Items.EMERALD || item==Items.FLINT)
            station=Blocks.GRINDSTONE;
        else if(item==Items.SUGAR_CANE || item==Items.WHEAT_SEEDS || item==Items.WHEAT ||
                item==Items.POPPY || item==Items.DANDELION || item==Items.CORNFLOWER)
            station=Blocks.CARTOGRAPHY_TABLE;
        else if(item==Items.SAND || item==Items.RED_SAND || item==Items.TERRACOTTA ||
                item==Items.CLAY_BALL || item==Items.NETHERRACK || item==Items.BLACKSTONE ||
                item==Items.BASALT || item==Items.END_STONE || item==Items.SOUL_SAND ||
                item==Items.NETHER_BRICKS || item==Items.CRIMSON_STEM || item==Items.WARPED_STEM)
            station=Blocks.STONECUTTER;
        else return new Result("NO_ROUTE",false);
        BlockPos pos=find(axo,station);
        if(pos==null)return new Result("NO_WORKSTATION",false);
        if(pos.distSqr(axo.blockPosition())>9) {
            axo.getNavigation().moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,.85);
            return new Result("APPROACH",false);
        }
        if(level.getGameTime()-lastTick<duration)return new Result("CHANNELING",false);
        SimpleContainer inv=axo.getAcquiredMaterials();
        boolean space=false;
        for(int i=0;i<inv.getContainerSize();i++) {
            ItemStack old=inv.getItem(i);
            if(old.isEmpty() || (old.is(item)&&old.getCount()<old.getMaxStackSize())) {space=true;break;}
        }
        if(!space)return new Result("INVENTORY_FULL",false);
        ItemStack remaining=inv.addItem(new ItemStack(item));
        if(!remaining.isEmpty())return new Result("INVENTORY_FULL",false);
        poof(level,pos);
        axo.recordAcquisition("MAGIC "+item+" at "+pos.toShortString());
        return new Result("GATHERED",true);
    }
}
