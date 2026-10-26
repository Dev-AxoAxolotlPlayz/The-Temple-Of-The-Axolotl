package com.devaxo.templeoftheaxolotl.brewing;

import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.pre07.Pre07Fluids;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
public final class CosmicBrewingEntity extends BlockEntity implements software.bernie.geckolib.animatable.GeoBlockEntity {
 private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache animationCache = software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
 @Override public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
  controllers.add(new software.bernie.geckolib.animation.AnimationController<>(this, "tesseract", 0, state -> state.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenLoop("animation.cosmic_brewing_stand.tesseract"))));
 }
 @Override public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {return animationCache;}
 public static final int CATALYST=0,WATER=1,INPUT=2,OUTPUT=3,HEAT=4,BUCKET=5;
 public static final int BANK_MAX=16000,HEAT_MAX=100;
 private int heatCharges,catalystCharges,waterCharges,ichor;
 public final SimpleContainer items=new SimpleContainer(6){@Override public void setChanged(){super.setChanged();CosmicBrewingEntity.this.setChanged();}};
 public CosmicBrewingEntity(BlockPos pos,BlockState state){super(CosmicBrewingRegistries.ENTITY.get(),pos,state);}
 public int heat(){return heatCharges;} public int catalyst(){return catalystCharges;} public int water(){return waterCharges;} public int ichor(){return ichor;}
 private void take(int slot,int amount){items.getItem(slot).shrink(amount);items.setChanged();}
 private void emptyBucket(){ItemStack s=items.getItem(BUCKET);if(s.isEmpty())items.setItem(BUCKET,new ItemStack(Items.BUCKET));else if(s.is(Items.BUCKET)&&s.getCount()<s.getMaxStackSize())s.grow(1);else return;items.setChanged();}
 private boolean canReturnBucket(){ItemStack s=items.getItem(BUCKET);return s.isEmpty()||(s.is(Items.BUCKET)&&s.getCount()<s.getMaxStackSize());}
 public void process(){
  if(level==null||level.isClientSide)return;
  ItemStack h=items.getItem(HEAT);
  if(heatCharges<=HEAT_MAX-10 && h.is(Items.LAVA_BUCKET) && canReturnBucket()){take(HEAT,1);emptyBucket();heatCharges+=10;}
  else if(heatCharges<HEAT_MAX && h.is(Items.MAGMA_BLOCK)&&h.getCount()>=2){take(HEAT,2);heatCharges++;}
  else if(heatCharges<=HEAT_MAX-2 && h.is(Items.SOUL_CAMPFIRE)){take(HEAT,1);heatCharges+=2;}
  else if(heatCharges<HEAT_MAX && h.is(Items.CAMPFIRE)){take(HEAT,1);heatCharges++;}
  ItemStack c=items.getItem(CATALYST);
  if(catalystCharges<=990 && c.is(ModItems.SHARD_OF_DIVINITY.get())){take(CATALYST,1);catalystCharges+=10;}
  ItemStack w=items.getItem(WATER);
  if(waterCharges<1000 && w.is(Items.WATER_BUCKET)&&canReturnBucket()){take(WATER,1);emptyBucket();waterCharges++;}
  else if(waterCharges<1000 && w.is(Items.BLUE_ICE)&&w.getCount()>=2){take(WATER,2);waterCharges++;}
  while(ichor<=BANK_MAX-1000 && heatCharges>=2 && catalystCharges>=1 && waterCharges>=1){heatCharges-=2;catalystCharges--;waterCharges--;ichor+=1000;}
  // Recycle one eligible celestial duplicate using one bank unit; 10% Shard chance.
  ItemStack in=items.getItem(INPUT),out=items.getItem(OUTPUT);
  if(ichor>=1000 && recyclable(in) && out.isEmpty()){
   take(INPUT,1);ichor-=1000;
   if(level.random.nextFloat()<0.1f)items.setItem(OUTPUT,new ItemStack(ModItems.SHARD_OF_DIVINITY.get()));
  }
  setChanged();
 }
 public static boolean recyclable(ItemStack s){return s.is(ModItems.CELESTIAL_MAGNET.get())||s.is(ModItems.CELESTIAL_REPOSITORY.get())||s.is(ModItems.CELESTIAL_JUKEBOX.get())||s.is(ModItems.LOGISTICS_MAESTRO.get())||s.is(ModItems.DISC_COSMIC_SEAS.get())||s.is(ModItems.DISC_BENEATH_SURFACE.get())||s.is(ModItems.DISC_OUT_OF_REACH.get())||s.is(Items.ENCHANTED_BOOK);}
 public boolean withdraw(){if(ichor<1000)return false;ItemStack s=items.getItem(BUCKET);if(!s.is(Items.BUCKET))return false;
  // Requires a free inventory slot for the resulting filled bucket; handled by menu.
  return true;
 }
 public void debit(){ichor-=1000;items.getItem(BUCKET).shrink(1);items.setChanged();setChanged();}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider regs){super.saveAdditional(tag,regs);tag.putInt("Heat",heatCharges);tag.putInt("Catalyst",catalystCharges);tag.putInt("Water",waterCharges);tag.putInt("Ichor",ichor);NonNullList<ItemStack> stacks=NonNullList.withSize(6,ItemStack.EMPTY);for(int i=0;i<6;i++)stacks.set(i,items.getItem(i));ContainerHelper.saveAllItems(tag,stacks,regs);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider regs){super.loadAdditional(tag,regs);heatCharges=tag.getInt("Heat");catalystCharges=tag.getInt("Catalyst");waterCharges=tag.getInt("Water");ichor=tag.getInt("Ichor");NonNullList<ItemStack> stacks=NonNullList.withSize(6,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,stacks,regs);for(int i=0;i<6;i++)items.setItem(i,stacks.get(i));}
}
