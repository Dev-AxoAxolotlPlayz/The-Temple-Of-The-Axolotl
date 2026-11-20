package com.devaxo.templeoftheaxolotl.promise;
import com.devaxo.templeoftheaxolotl.item.ModItems;
import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
public final class PromiseUpgrade {
 public static final int[] COST={2500,3125,3907,4883,6104,7630,9537,11921};
 public static int level(ItemStack s){return Math.max(0,Math.min(8,s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("promise_upgrade")));}
 public static int level(ServerPlayer p){return level(TempleHudModules.getSlot(p,PromiseState.SLOT));}
 public static int width(int n){return 3+Math.min(n,3);}
 public static int depth(int n){return 2+Math.max(0,Math.min(n-3,3));}
 public static int height(int n){return 4+Math.max(0,n-6);}
 public static int xp(ServerPlayer p){int l=p.experienceLevel; long base=l<=16?(long)l*l+6L*l:l<=31?(5L*l*l-81L*l+720)/2:(9L*l*l-325L*l+4440)/2;return (int)Math.min(Integer.MAX_VALUE,base+Math.round(p.experienceProgress*p.getXpNeededForNextLevel()));}
 public static void apply(ServerPlayer p){
  ItemStack s=TempleHudModules.getSlot(p,PromiseState.SLOT), limb=p.getMainHandItem();
  if(!s.is(ModItems.AXOLUTHUS_PROMISE.get())||!limb.is(ModItems.KRAKEN_LIMB.get()))return;
  int n=level(s); if(n>=8){p.sendSystemMessage(Component.literal("Promise is fully upgraded."));return;}
  if(!p.isCreative()&&xp(p)<COST[n]){p.sendSystemMessage(Component.literal("Requires "+COST[n]+" XP points."));return;}
  CompoundTag tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("promise_upgrade",n+1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
  TempleHudModules.storeSlot(p,PromiseState.SLOT,s);
  if(!p.isCreative()){limb.shrink(1);p.giveExperiencePoints(-COST[n]);}
  p.sendSystemMessage(Component.literal("Promise upgraded to +"+(n+1)));
 }
}
