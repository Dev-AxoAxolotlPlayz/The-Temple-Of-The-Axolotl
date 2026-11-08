package com.devaxo.templeoftheaxolotl.kiln;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
public class AnnealingKilnMenu extends AbstractContainerMenu {
 private final Container contents;private final AnnealingKilnEntity kiln;private final BlockPos pos;
 private final ContainerData data;
 public AnnealingKilnMenu(int id,Inventory player,BlockPos pos){this(id,player,new SimpleContainer(AnnealingKilnEntity.SLOTS),pos);}
 public AnnealingKilnMenu(int id,Inventory player,Container contents,BlockPos pos){
  super(KilnRegistries.MENU.get(),id);this.contents=contents;this.pos=pos;
  kiln=player.player.level().getBlockEntity(pos) instanceof AnnealingKilnEntity be?be:null;
  data=new ContainerData(){private final int[] client=new int[4];public int get(int i){if(kiln!=null&&!player.player.level().isClientSide)return switch(i){case 0->kiln.cooling()?1:0;case 1->kiln.progress();case 2->kiln.coolant();default->kiln.count();};return client[i];}public void set(int i,int v){client[i]=v;}public int getCount(){return 4;}};
  addDataSlots(data);contents.startOpen(player.player);
  for(int i=0;i<9;i++){final int index=i;addSlot(new Slot(contents,i,18+(i%3)*18,19+(i/3)*18){@Override public boolean mayPlace(ItemStack stack){return !isCooling()&&stack.is(ModItems.MOLTEN_GLASS_IN_A_MOLD.get());}@Override public boolean mayPickup(Player p){return !isCooling();}@Override public int getMaxStackSize(){return 1;}});}
  addSlot(new Slot(contents,9,89,37){@Override public boolean mayPlace(ItemStack s){return s.is(Items.BLUE_ICE);}});
  addSlot(new Slot(contents,10,144,37){@Override public boolean mayPlace(ItemStack s){return false;}});
  for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(player,x+y*9+9,8+x*18,103+y*18));
  for(int x=0;x<9;x++)addSlot(new Slot(player,x,8+x*18,161));
 }
 public boolean isCooling(){return data.get(0)!=0;}
 public int progress(){return data.get(1);}public int coolant(){return data.get(2);}public int count(){return data.get(3);}
 @Override public boolean clickMenuButton(Player p,int id){return id==0&&stillValid(p)&&(p.level().isClientSide||kiln!=null&&kiln.start());}
 @Override public boolean stillValid(Player p){return kiln!=null && AbstractContainerMenu.stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(p.level(),pos),p,com.devaxo.templeoftheaxolotl.registry.ModBlocks.ANNEALING_KILN.get());}
 @Override public ItemStack quickMoveStack(Player p,int slot){
  if(slot<0||slot>=slots.size())return ItemStack.EMPTY;
  Slot from=slots.get(slot);if(!from.hasItem())return ItemStack.EMPTY;
  ItemStack original=from.getItem(),copy=original.copy();
  if(slot<11){if(slot<9&&isCooling())return ItemStack.EMPTY;if(!moveItemStackTo(original,11,47,true))return ItemStack.EMPTY;}
  else if(original.is(Items.BLUE_ICE)){if(!moveItemStackTo(original,9,10,false))return ItemStack.EMPTY;}
  else if(original.is(ModItems.MOLTEN_GLASS_IN_A_MOLD.get())&&!isCooling()){
   if(!moveItemStackTo(original,0,9,false))return ItemStack.EMPTY;
  }else return ItemStack.EMPTY;
  if(original.isEmpty())from.set(ItemStack.EMPTY);else from.setChanged();return copy;
 }
 @Override public void removed(Player p){super.removed(p);contents.stopOpen(p);}
}
