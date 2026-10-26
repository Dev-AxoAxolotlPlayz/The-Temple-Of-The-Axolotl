package com.devaxo.templeoftheaxolotl.brewing;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.pre07.Pre07Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
public final class CosmicBrewingMenu extends AbstractContainerMenu {
 private final BlockPos pos;private final CosmicBrewingEntity machine;
 private final ContainerData data=new SimpleContainerData(4);
 public CosmicBrewingMenu(int id,Inventory inventory,BlockPos pos){super(CosmicBrewingRegistries.MENU.get(),id);this.pos=pos;
  this.machine=inventory.player.level().getBlockEntity(pos) instanceof CosmicBrewingEntity e?e:null;
  var slots=machine==null?new SimpleContainer(6):machine.items;
  addSlot(new Slot(slots,0,94,41){@Override public boolean mayPlace(ItemStack s){return s.is(ModItems.SHARD_OF_DIVINITY.get());}});
  addSlot(new Slot(slots,1,204,41){@Override public boolean mayPlace(ItemStack s){return s.is(Items.WATER_BUCKET)||s.is(Items.BLUE_ICE);}});
  addSlot(new Slot(slots,2,94,98){@Override public boolean mayPlace(ItemStack s){return CosmicBrewingEntity.recyclable(s);}});
  addSlot(new Slot(slots,3,204,98){@Override public boolean mayPlace(ItemStack s){return false;}});
  addSlot(new Slot(slots,4,204,153){@Override public boolean mayPlace(ItemStack s){return s.is(Items.LAVA_BUCKET)||s.is(Items.MAGMA_BLOCK)||s.is(Items.CAMPFIRE)||s.is(Items.SOUL_CAMPFIRE);}});
  addSlot(new Slot(slots,5,240,153){@Override public boolean mayPlace(ItemStack s){return s.is(Items.BUCKET);}});
  for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inventory,9+r*9+c,77+c*18,211+r*18));
  for(int c=0;c<9;c++)addSlot(new Slot(inventory,c,77+c*18,269));
  for(int i=0;i<4;i++){final int n=i;addDataSlot(new DataSlot(){@Override public int get(){if(machine!=null&&!inventory.player.level().isClientSide)return switch(n){case 0->machine.heat();case 1->machine.catalyst();case 2->machine.water();default->machine.ichor();};return data.get(n);}@Override public void set(int v){data.set(n,v);}});}
 }
 public int heat(){return data.get(0);}public int catalyst(){return data.get(1);}public int water(){return data.get(2);}public int ichor(){return data.get(3);}
 @Override public boolean stillValid(Player p){return p.level().getBlockEntity(pos)==machine && p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
 @Override public boolean clickMenuButton(Player player,int id){if(id!=0||machine==null||player.level().isClientSide)return false;
  if(!machine.withdraw())return false;
  ItemStack filled=new ItemStack(Pre07Fluids.DILUTED_BUCKET.get());
  if(!player.getInventory().add(filled))return false;
  machine.debit();return true;
 }
 @Override public ItemStack quickMoveStack(Player player,int index){Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;ItemStack source=slot.getItem(),copy=source.copy();if(index<6){if(!moveItemStackTo(source,6,42,true))return ItemStack.EMPTY;}else{
  int dest=CosmicBrewingEntity.recyclable(source)?2:source.is(ModItems.SHARD_OF_DIVINITY.get())?0:source.is(Items.WATER_BUCKET)||source.is(Items.BLUE_ICE)?1:source.is(Items.LAVA_BUCKET)||source.is(Items.MAGMA_BLOCK)||source.is(Items.CAMPFIRE)||source.is(Items.SOUL_CAMPFIRE)?4:source.is(Items.BUCKET)?5:-1;
  if(dest<0||!moveItemStackTo(source,dest,dest+1,false))return ItemStack.EMPTY;
 }if(source.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return copy;}
 public void serverTick(){if(machine!=null)machine.process();}
}
