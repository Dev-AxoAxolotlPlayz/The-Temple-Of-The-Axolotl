package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;

/** One composter cycle = one unenchanted player-kill loot roll, not one item. */
public final class WaifuAxoluthuComposterHunt {
    private WaifuAxoluthuComposterHunt() {}
    public static String hunt(WaifuAxoluthuEntity axo, EntityType<?> target, long lastCycle, int cooldown) {
        if(!(axo.level() instanceof ServerLevel level)) return "SERVER_ONLY";
        BlockPos station=WaifuAxoluthuMagicGathering.find(axo,Blocks.COMPOSTER);
        if(station==null)return "NEED_COMPOSTER";
        if(station.distSqr(axo.blockPosition())>9){
            axo.getNavigation().moveTo(station.getX()+.5,station.getY(),station.getZ()+.5,.85);
            return "APPROACH_COMPOSTER";
        }
        if(level.getGameTime()-lastCycle<cooldown)return "CHANNELING";
        ServerPlayer owner=level.getServer().getPlayerList().getPlayer(axo.getBondedPlayer());
        if(owner==null || owner.level()!=level)return "OWNER_NOT_PRESENT";
        Entity mob=target.create(level);
        if(mob==null)return "INVALID_TARGET";
        // Entity is a loot context only; never added to the world.
        mob.moveTo(station.getX()+.5,station.getY()+1,station.getZ()+.5);
        var params=new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY,mob)
            .withParameter(LootContextParams.ORIGIN,mob.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(owner))
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY,owner)
            .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER,owner)
            .create(LootContextParamSets.ENTITY);
        List<ItemStack> loot=level.getServer().reloadableRegistries()
            .getLootTable(target.getDefaultLootTable()).getRandomItems(params);
        // Atomic capacity check: never generate partial loot or silently discard drops.
        var simulation=new net.minecraft.world.SimpleContainer(axo.getAcquiredMaterials().getContainerSize());
        for(int i=0;i<simulation.getContainerSize();i++)simulation.setItem(i,axo.getAcquiredMaterials().getItem(i).copy());
        for(ItemStack drop:loot)if(!simulation.addItem(drop.copy()).isEmpty())return "INVENTORY_FULL";
        for(int i=0;i<simulation.getContainerSize();i++)axo.getAcquiredMaterials().setItem(i,simulation.getItem(i).copy());
        WaifuAxoluthuMagicGathering.poof(level,station);
        axo.recordAcquisition("SIMULATED_KILL "+target+" stacks="+loot.size()+" looting=0");
        return "HUNTED";
    }
}
