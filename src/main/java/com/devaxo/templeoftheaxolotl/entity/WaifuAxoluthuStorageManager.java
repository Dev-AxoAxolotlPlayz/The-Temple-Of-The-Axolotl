package com.devaxo.templeoftheaxolotl.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import java.util.ArrayList;
import java.util.Collections;

/** Stage 3: conservative surplus deposits, authorized block inventories only.
 * Runs on the server thread. Never drops, deletes, or overwrites items.
 */
public final class WaifuAxoluthuStorageManager {
    private WaifuAxoluthuStorageManager() {}
    private record OpenChest(BlockPos first,BlockPos second,long expires) {}
    private static final Map<UUID,OpenChest> OPEN=new HashMap<>();
    private static BlockPos paired(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE))return null;
        ChestType type=state.getValue(ChestBlock.TYPE);
        if(type==ChestType.SINGLE)return null;
        Direction facing=state.getValue(ChestBlock.FACING);
        Direction connection=type==ChestType.LEFT?facing.getClockWise():facing.getCounterClockWise();
        BlockPos other=pos.relative(connection);
        if(!level.getChunkSource().hasChunk(other.getX()>>4,other.getZ()>>4))return null;
        var adjacent=level.getBlockState(other);
        if(!adjacent.is(state.getBlock()) || !adjacent.hasProperty(ChestBlock.TYPE)
                || adjacent.getValue(ChestBlock.TYPE)==type
                || adjacent.getValue(ChestBlock.FACING)!=facing)return null;
        return other;
    }
    private static Container combined(ServerLevel level,BlockPos pos) {
        if(!(level.getBlockEntity(pos) instanceof Container first))return null;
        BlockPos second=paired(level,pos);
        if(second!=null && level.getBlockEntity(second) instanceof Container other)
            return new CompoundContainer(first,other);
        return first;
    }
    private static void lid(ServerLevel level,BlockPos pos,boolean open) {
        if(pos!=null && level.getBlockState(pos).is(Blocks.CHEST))
            level.blockEvent(pos,level.getBlockState(pos).getBlock(),1,open?1:0);
    }
    public static void tickChestAnimation(WaifuAxoluthuEntity axo) {
        if(!(axo.level() instanceof ServerLevel level))return;
        OpenChest session=OPEN.get(axo.getUUID());
        if(session!=null && (level.getGameTime()>=session.expires()
                || axo.distanceToSqr(session.first().getX()+.5,session.first().getY(),session.first().getZ()+.5)>25)) {
            lid(level,session.first(),false);lid(level,session.second(),false);
            OPEN.remove(axo.getUUID());
        }
    }
    private static void openFor(WaifuAxoluthuEntity axo,ServerLevel level,BlockPos pos) {
        OpenChest old=OPEN.get(axo.getUUID());
        BlockPos second=paired(level,pos);
        if(old!=null && !old.first().equals(pos)) {
            lid(level,old.first(),false);lid(level,old.second(),false);
        }
        if(old==null || !old.first().equals(pos)) {
            lid(level,pos,true);lid(level,second,true);
        }
        OPEN.put(axo.getUUID(),new OpenChest(pos,second,level.getGameTime()+55));
    }
    public record Result(boolean completed, boolean moved, String detail) {}
    public static boolean inventoryCrowded(WaifuAxoluthuEntity axo) {
        int used=0;
        var inv=axo.getAcquiredMaterials();
        for(int i=0;i<inv.getContainerSize();i++) if(!inv.getItem(i).isEmpty()) used++;
        return used>=8;
    }
    private static int reserved(WaifuAxoluthuEntity axo, ItemStack stack) {
        if(WaifuAxoluthuWorkshopActions.findSmithingTable(axo)!=null) return 0;
        if(stack.is(Items.IRON_INGOT)) return 2;
        if(stack.is(ItemTags.PLANKS)) return 8;
        return 0;
    }
    /** Deposit only surplus saplings, preserving all construction ingredients.
     * Returns an incomplete result when travel/animation is pending; no items are
     * destroyed or dropped if storage is unavailable or full.
     */
    public static Result depositSaplings(WaifuAxoluthuEntity axo) {
        if (!(axo.level() instanceof ServerLevel level)) return new Result(false,false,"SERVER_ONLY");
        var bag=axo.getAcquiredMaterials();
        boolean any=false;
        for(int i=0;i<bag.getContainerSize();i++)
            if(bag.getItem(i).is(ItemTags.SAPLINGS)) {any=true;break;}
        if(!any)return new Result(true,false,"NO_SURPLUS_SAPLINGS");
        var positions=new ArrayList<>(axo.getAuthorizedContainers());
        Collections.sort(positions);
        for(long key:positions) {
            BlockPos pos=BlockPos.of(key);
            if(!level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4))continue;
            Container chest=combined(level,pos);
            if(chest==null)continue;
            if(pos.distSqr(axo.blockPosition())>9) {
                boolean moving=axo.getNavigation().moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,1.0);
                return new Result(false,false,moving?"APPROACH_SAPLING_STORAGE":"SAPLING_STORAGE_PATH_FAILED");
            }
            axo.getNavigation().stop();
            axo.getLookControl().setLookAt(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_WORKSTATION,pos,30,60))
                return new Result(false,false,"OPENING_SAPLING_STORAGE");
            openFor(axo,level,pos);
            int moved=0;
            for(int i=0;i<bag.getContainerSize();i++) {
                ItemStack stack=bag.getItem(i);
                if(!stack.is(ItemTags.SAPLINGS))continue;
                for(int j=0;j<chest.getContainerSize() && !stack.isEmpty();j++) {
                    if(!chest.canPlaceItem(j,stack))continue;
                    ItemStack existing=chest.getItem(j);
                    if(!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,stack))continue;
                    int cap=existing.isEmpty()?Math.min(stack.getMaxStackSize(),chest.getMaxStackSize()):
                            Math.min(existing.getMaxStackSize(),chest.getMaxStackSize())-existing.getCount();
                    int count=Math.min(stack.getCount(),Math.max(0,cap));
                    if(count==0)continue;
                    if(existing.isEmpty())chest.setItem(j,stack.copyWithCount(count));
                    else {existing.grow(count);chest.setChanged();}
                    stack.shrink(count);moved+=count;
                }
                if(stack.isEmpty())bag.setItem(i,ItemStack.EMPTY);
            }
            if(moved>0) {
                chest.setChanged();bag.setChanged();axo.finishAction();
                axo.recordAcquisition("DEPOSITED_SURPLUS_SAPLINGS count="+moved+" chest="+pos.toShortString());
                return new Result(true,true,"SAPLINGS_DEPOSITED "+moved);
            }
        }
        return new Result(false,false,"NO_SAPLING_STORAGE_SPACE");
    }
    public static Result deposit(WaifuAxoluthuEntity axo) { return deposit(axo,false); }
    public static Result deposit(WaifuAxoluthuEntity axo, boolean depositEverything) {
        if(axo.level().isClientSide()) return new Result(false,false,"SERVER_ONLY");
        var positions=new ArrayList<>(axo.getAuthorizedContainers());
        Collections.sort(positions);
        var carried=axo.getAcquiredMaterials();
        int totalMoved=0;
        boolean foundAuthorized=false;
        for(long key:positions) {
            BlockPos pos=BlockPos.of(key);
            if(!axo.level().getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)) continue;
            if(!(axo.level() instanceof ServerLevel server))continue;
            Container chest=combined(server,pos);
            if(chest==null)continue;
            foundAuthorized=true;
            if(pos.distSqr(axo.blockPosition())>9) {
                boolean path=axo.getNavigation().moveTo(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,1.0);
                return new Result(false,totalMoved>0,path?"APPROACH_STORAGE "+pos.toShortString():
                        "STORAGE_PATH_FAILED "+pos.toShortString());
            }
            axo.getNavigation().stop();
            axo.getLookControl().setLookAt(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            if(!axo.actionReady(WaifuAxoluthuEntity.ACTION_WORKSTATION,pos,30,60))
                return new Result(false,totalMoved>0,"OPENING_STORAGE "+pos.toShortString());
            openFor(axo,server,pos);
            int moved=0;
            for(int i=0;i<carried.getContainerSize();i++) {
                ItemStack stack=carried.getItem(i);
                if(stack.isEmpty()) continue;
                int available=Math.max(0,stack.getCount()-(depositEverything?0:reserved(axo,stack)));
                if(available==0) continue;
                for(int j=0;j<chest.getContainerSize() && available>0;j++) {
                    if(!chest.canPlaceItem(j,stack)) continue;
                    ItemStack old=chest.getItem(j);
                    if(!old.isEmpty() && !ItemStack.isSameItemSameComponents(old,stack)) continue;
                    int capacity=old.isEmpty()?Math.min(stack.getMaxStackSize(),chest.getMaxStackSize()):
                        Math.min(old.getMaxStackSize(),chest.getMaxStackSize())-old.getCount();
                    int amount=Math.min(available,Math.max(0,capacity));
                    if(amount<=0) continue;
                    if(old.isEmpty()) chest.setItem(j,stack.copyWithCount(amount));
                    else { old.grow(amount);chest.setChanged(); }
                    stack.shrink(amount);
                    carried.setChanged();
                    available-=amount;
                    moved+=amount;
                }
                if(stack.isEmpty()) carried.setItem(i,ItemStack.EMPTY);
            }
            if(moved>0) {
                chest.setChanged();
                axo.finishAction();
                axo.recordAcquisition("DEPOSIT authorized="+pos.toShortString()+" items="+moved);
                axo.speak("storage_deposit","I've put "+moved+" spare items away in our storage! :D");
                totalMoved+=moved;
                if(!depositEverything) return new Result(true,true,"DEPOSITED "+moved+" at "+pos.toShortString());
            }
        }
        if(depositEverything) {
            boolean empty=true;
            for(int i=0;i<carried.getContainerSize();i++) if(!carried.getItem(i).isEmpty()) {empty=false;break;}
            if(empty) return new Result(true,totalMoved>0,"ALL_ITEMS_STORED "+totalMoved);
            return new Result(false,totalMoved>0,foundAuthorized?"AUTHORIZED_STORAGE_FULL":"NO_AUTHORIZED_STORAGE");
        }
        return new Result(false,false,"NO_AUTHORIZED_STORAGE_WITH_SPACE");
    }
}
