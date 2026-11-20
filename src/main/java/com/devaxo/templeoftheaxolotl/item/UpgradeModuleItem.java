package com.devaxo.templeoftheaxolotl.item;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import com.devaxo.templeoftheaxolotl.promise.PromiseUpgrade;
public final class UpgradeModuleItem extends Item {
 public UpgradeModuleItem(Properties properties){super(properties);}
 @Override public Component getName(ItemStack stack){int n=PromiseUpgrade.level(stack);return n==0?super.getName(stack):super.getName(stack).copy().append(" U"+n);}
}
