package com.devaxo.templeoftheaxolotl.block;

import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.registry.ModBlocks;
import com.devaxo.templeoftheaxolotl.repository.CelestialRepository;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=TempleOfTheAxolotl.MODID)
public final class AbyssalChestConversion {
    @SubscribeEvent public static void convert(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(!event.getItemStack().is(ModItems.CELESTIAL_REPOSITORY.get()))return;
        BlockPos pos=event.getPos();var level=player.serverLevel();var state=level.getBlockState(pos);
        if(!state.is(Blocks.CHEST))return;
        if(state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){player.displayClientMessage(Component.literal("Convert a single chest; separate double chests first."),true);return;}
        if(!(level.getBlockEntity(pos) instanceof Container old))return;
        // Preflight: no contents are removed until every stack can fit in the player's repository.
        // Rollback if capacity runs out, avoiding item loss.
        java.util.ArrayList<ItemStack> contents=new java.util.ArrayList<>();
        for(int i=0;i<old.getContainerSize();i++)if(!old.getItem(i).isEmpty())contents.add(old.getItem(i).copy());
        // Repository changes are transactional: snapshot all three persistent pages.
        var data=player.getPersistentData();
        String[] keys={"tota_celestial_repository_v1","tota_celestial_repository_page_two_prototype","tota_celestial_repository_page_three"};
        net.minecraft.nbt.Tag[] snapshots=new net.minecraft.nbt.Tag[keys.length];
        for(int i=0;i<keys.length;i++)snapshots[i]=data.contains(keys[i])?data.get(keys[i]).copy():null;
        boolean fits=true;
        for(ItemStack stack:contents){
            int left=stack.getCount();
            for(int page=0;page<CelestialRepository.pages(player)&&left>0;page++){
                ItemStack part=stack.copyWithCount(left);
                left-=CelestialRepository.insertPage(player,page,part);
            }
            if(left>0){fits=false;break;}
        }
        if(!fits){
            for(int i=0;i<keys.length;i++)if(snapshots[i]==null)data.remove(keys[i]);else data.put(keys[i],snapshots[i]);
            player.displayClientMessage(Component.literal("Not enough Repository space to preserve chest contents."),true);
            event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);return;
        }
        if(!level.setBlock(pos,ModBlocks.ABYSSAL_CHEST.get().defaultBlockState().setValue(AbyssalChestBlock.FACING, state.getValue(ChestBlock.FACING)),3)){
            for(int i=0;i<keys.length;i++)if(snapshots[i]==null)data.remove(keys[i]);else data.put(keys[i],snapshots[i]);
            return;
        }
        if(level.getBlockEntity(pos) instanceof AbyssalChestEntity chest)chest.setOwner(player.getUUID());
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
