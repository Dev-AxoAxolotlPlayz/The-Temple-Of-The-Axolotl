package com.devaxo.templeoftheaxolotl.pre07;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
/** Each Sentinel independently pays one reward, including both jockey members. */
public final class TrialTrapDeathEvents {
 private TrialTrapDeathEvents() {}
 public static void onDeath(LivingDeathEvent event) {
  if(!(event.getEntity().level() instanceof ServerLevel level))return;
  var data=event.getEntity().getPersistentData();
  if(!data.getBoolean("tota_sentinel_reward"))return;
  data.putBoolean("tota_sentinel_reward",false);
  int floor=Math.max(1,Math.min(10,data.getInt("tota_sentinel_floor")));
  int tier=Math.min(4,Math.max(1,(floor+2)/3));
  var drop=new ItemEntity(level,event.getEntity().getX(),event.getEntity().getY()+.3,event.getEntity().getZ(),new ItemStack(Pre07Items.STONES.get(tier-1).get()));
  level.addFreshEntity(drop);
 }
}
