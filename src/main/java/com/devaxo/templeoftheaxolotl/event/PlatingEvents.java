package com.devaxo.templeoftheaxolotl.event;
import com.devaxo.templeoftheaxolotl.TempleOfTheAxolotl;
import com.devaxo.templeoftheaxolotl.item.*;
import com.devaxo.templeoftheaxolotl.compass.TempleHudModules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.*;
@EventBusSubscriber(modid=TempleOfTheAxolotl.MODID) public final class PlatingEvents {
 @SubscribeEvent(priority=EventPriority.HIGH) public static void armor(LivingDamageEvent.Pre e){if(e.getEntity() instanceof Player p){int n=AxolitePlating.pieces(p);if(n>0)e.setNewDamage(e.getNewDamage()*(1f-.1f*n));}}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void heart(LivingDropsEvent e){if(e.getEntity() instanceof com.devaxo.templeoftheaxolotl.entity.AxoleviathanEntity mob && !mob.level().isClientSide()){e.getDrops().removeIf(d->d.getItem().is(ModItems.HEART_OF_THE_ABYSS.get()));e.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),new ItemStack(ModItems.HEART_OF_THE_ABYSS.get())));}}
}
