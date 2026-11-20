package com.devaxo.templeoftheaxolotl.item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
public final class AxolitePlating {
 public static int tier(ItemStack s){return s.is(ModItems.CELESTIAL_REPOSITORY.get())?1:s.is(ModItems.IRON_REPOSITORY.get())?2:s.is(ModItems.GOLD_REPOSITORY.get())?3:s.is(ModItems.DIAMOND_REPOSITORY.get())?4:s.is(ModItems.NETHERITE_REPOSITORY.get())?5:0;}
 public static boolean armor(ItemStack s){return s.is(ModItems.AXOLOTL_ABSOLUTE.get())||s.is(ModItems.AXOLOTL_CHESTPLATE.get())||s.is(ModItems.AXOLOTL_LEGGINGS.get())||s.is(ModItems.AXOLOTL_BOOTS.get());}
 public static boolean plated(ItemStack s){return !s.isEmpty() && (tier(s)>0||armor(s)) && s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBoolean("axolite_plated");}
 public static int capacity(ItemStack s){int t=tier(s);return t==0?0:600<<(t-1);}
 public static int oxygen(ItemStack s){return plated(s)?Math.max(0,Math.min(capacity(s),s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("axolite_oxygen"))):0;}
 public static void oxygen(ItemStack s,int value){var tag=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putInt("axolite_oxygen",Math.max(0,Math.min(capacity(s),value)));s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
 public static ItemStack apply(ItemStack s){var out=s.copyWithCount(1);var tag=out.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putBoolean("axolite_plated",true);out.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return out;}
 public static int pieces(Player p){int n=0;for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){var s=p.getItemBySlot(slot);if(armor(s)&&plated(s))n++;}return n;}
 public static int percent(ItemStack s){return capacity(s)==0?0:Math.round(100f*oxygen(s)/capacity(s));}
}
