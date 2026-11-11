package com.devaxo.templeoftheaxolotl.kiln;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import net.minecraft.core.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
public class AnnealingKilnEntity extends BlockEntity implements MenuProvider {
 public static final int MOLD_COUNT=9,FUEL=9,OUTPUT=10,SLOTS=11,COOL_TICKS=200;
 private final SimpleContainer inventory=new SimpleContainer(SLOTS){@Override public void setChanged(){super.setChanged();AnnealingKilnEntity.this.setChanged();}};
 private int coolantUnits,progress,activeCount;
 private boolean cooling;
 public AnnealingKilnEntity(BlockPos pos,BlockState state){super(KilnRegistries.KILN.get(),pos,state);}
 public Container items(){return inventory;}
 public boolean cooling(){return cooling;}
 public int progress(){return progress;}
 public int coolant(){return coolantUnits;}
 public int count(){return activeCount;}
 public boolean start(){
  if(level==null||level.isClientSide||cooling)return false;
  int n=0;for(int i=0;i<MOLD_COUNT;i++)if(!inventory.getItem(i).isEmpty()){
   if(!inventory.getItem(i).is(ModItems.MOLTEN_GLASS_IN_A_MOLD.get()))return false;n++;}
  if(n==0)return false;
  ItemStack out=inventory.getItem(OUTPUT);
  if(!out.isEmpty()&&(!ItemStack.isSameItemSameComponents(out,new ItemStack(ModItems.PRESSURE_TREATED_ACRYLIC_PANEL.get()))||out.getCount()+n>out.getMaxStackSize()))return false;
  int cost=n*(n+7),available=coolantUnits+inventory.getItem(FUEL).getCount()*24;
  if(available<cost||(!inventory.getItem(FUEL).isEmpty()&&!inventory.getItem(FUEL).is(Items.BLUE_ICE)))return false;
  // Commit the exact batch before the first cooling tick; subsequent slot mutation is forbidden.
  int missing=Math.max(0,cost-coolantUnits),blocks=(missing+23)/24;
  if(blocks>0)inventory.removeItem(FUEL,blocks);
  coolantUnits+=blocks*24-cost;
  for(int i=0;i<MOLD_COUNT;i++)if(!inventory.getItem(i).isEmpty())inventory.removeItem(i,1);
  activeCount=n;progress=0;cooling=true;setChanged();return true;
 }
 public static void tick(Level level,BlockPos pos,BlockState state,AnnealingKilnEntity be){
  if(!(level instanceof ServerLevel)||!be.cooling)return;
  if(++be.progress>=COOL_TICKS){
   ItemStack out=be.inventory.getItem(OUTPUT);
   if(out.isEmpty())be.inventory.setItem(OUTPUT,new ItemStack(ModItems.PRESSURE_TREATED_ACRYLIC_PANEL.get(),be.activeCount));
   else if(out.is(ModItems.PRESSURE_TREATED_ACRYLIC_PANEL.get())&&out.getCount()+be.activeCount<=out.getMaxStackSize())out.grow(be.activeCount);
   else {be.progress=COOL_TICKS-1;return;}
   be.activeCount=0;be.cooling=false;be.progress=0;
  }
  be.setChanged();
 }
 public void cancelAndRefund(){
  if(!cooling)return;
  // Committed molds are returned on destruction, so interrupted batches never silently vanish.
  if(level!=null)Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,new ItemStack(ModItems.MOLTEN_GLASS_IN_A_MOLD.get(),activeCount));
  cooling=false;activeCount=0;progress=0;
 }
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){super.saveAdditional(tag,provider);tag.putInt("CoolantUnits",coolantUnits);tag.putInt("CoolingProgress",progress);tag.putInt("ActiveCount",activeCount);tag.putBoolean("Cooling",cooling);net.minecraft.world.ContainerHelper.saveAllItems(tag,asList(),provider);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){super.loadAdditional(tag,provider);coolantUnits=tag.getInt("CoolantUnits");progress=tag.getInt("CoolingProgress");activeCount=tag.getInt("ActiveCount");cooling=tag.getBoolean("Cooling");net.minecraft.core.NonNullList<ItemStack> saved=net.minecraft.core.NonNullList.withSize(SLOTS,ItemStack.EMPTY);net.minecraft.world.ContainerHelper.loadAllItems(tag,saved,provider);for(int i=0;i<SLOTS;i++)inventory.setItem(i,saved.get(i));}
 private net.minecraft.core.NonNullList<ItemStack> asList(){net.minecraft.core.NonNullList<ItemStack> list=net.minecraft.core.NonNullList.withSize(SLOTS,ItemStack.EMPTY);for(int i=0;i<SLOTS;i++)list.set(i,inventory.getItem(i));return list;}
 @Override public Component getDisplayName(){return Component.literal("Annealing Kiln");}
 @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new AnnealingKilnMenu(id,inv,inventory,worldPosition);}
}
