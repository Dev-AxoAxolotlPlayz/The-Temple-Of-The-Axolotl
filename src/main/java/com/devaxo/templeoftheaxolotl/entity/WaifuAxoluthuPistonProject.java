package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** A persistent, demand-driven piston idea. No blocks are placed or removed. */
public final class WaifuAxoluthuPistonProject {
    private WaifuAxoluthuPistonProject() {}
    public static String step(WaifuAxoluthuEntity axo, long lastGather) {
        if (!(axo.level() instanceof ServerLevel level)) return "SERVER_ONLY";
        SimpleContainer bag=axo.getAcquiredMaterials();
        for(int i=0;i<bag.getContainerSize();i++)
            if(bag.getItem(i).is(net.minecraft.tags.ItemTags.SAPLINGS))
                return "REPLANT "+WaifuAxoluthuForestry.step(axo);
        String status=WaifuAxoluthuRecipeBrain.ensure(axo,Items.PISTON,1,lastGather);
        if(status.equals("READY") || status.startsWith("CRAFTED_minecraft:piston"))
            return "CRAFTED_PISTON";
        return status;
    }
    private static boolean hasSpace(SimpleContainer bag,Item item) {
        for(int i=0;i<bag.getContainerSize();i++) {
            ItemStack s=bag.getItem(i);
            if(s.isEmpty() || (s.is(item)&&s.getCount()<s.getMaxStackSize()))return true;
        }
        return false;
    }
}
