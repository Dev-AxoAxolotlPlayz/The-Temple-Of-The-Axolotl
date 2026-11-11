package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Uses actual furnace slots and vanilla ticking; never fabricates ingots. */
public final class WaifuAxoluthuFurnaceActions {
    private WaifuAxoluthuFurnaceActions() {}
    public static String process(WaifuAxoluthuEntity axo) {
        if(!(axo.level() instanceof ServerLevel level))return "SERVER_ONLY";
        BlockPos pos=WaifuAxoluthuMagicGathering.find(axo,Blocks.FURNACE);
        if(pos==null)return "NO_FURNACE";
        if(pos.distSqr(axo.blockPosition())>4) {
            axo.getNavigation().moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,.85);
            return "APPROACH_FURNACE";
        }
        axo.getNavigation().stop();
        axo.getLookControl().setLookAt(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_WORKSTATION,pos,30,60))return "WORKSTATION_ANIMATION";
        if(!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace))return "NO_FURNACE_ENTITY";
        var inv=axo.getAcquiredMaterials();
        ItemStack output=furnace.getItem(2);
        if(!output.isEmpty() && output.is(Items.IRON_INGOT)) {
            ItemStack leftover=inv.addItem(output.copy());
            if(leftover.getCount()==output.getCount())return "INVENTORY_FULL";
            furnace.setItem(2,leftover);
            furnace.setChanged();
            axo.recordAcquisition("FURNACE_OUTPUT "+(output.getCount()-leftover.getCount()));
            return "COLLECTED_INGOTS";
        }
        ItemStack input=furnace.getItem(0);
        if(input.isEmpty()) {
            for(int i=0;i<inv.getContainerSize();i++) {
                ItemStack carried=inv.getItem(i);
                if(!carried.is(Items.RAW_IRON))continue;
                ItemStack one=inv.removeItem(i,1);
                furnace.setItem(0,one);
                furnace.setChanged();
                return "LOADED_RAW_IRON";
            }
        }
        if(furnace.getItem(1).isEmpty() && !furnace.getItem(0).isEmpty()) {
            for(int i=0;i<inv.getContainerSize();i++) {
                ItemStack fuel=inv.getItem(i);
                if(!fuel.is(net.minecraft.tags.ItemTags.PLANKS))continue;
                furnace.setItem(1,inv.removeItem(i,1));
                furnace.setChanged();
                return "FUELED_WITH_PLANK";
            }
            for(int i=0;i<inv.getContainerSize();i++) {
                ItemStack fuel=inv.getItem(i);
                if(!fuel.is(Items.COAL))continue;
                furnace.setItem(1,inv.removeItem(i,1));
                furnace.setChanged();
                return "FUELED_WITH_COAL";
            }
            return "NEED_FUEL";
        }
        return "SMELTING_OR_WAITING";
    }
}
